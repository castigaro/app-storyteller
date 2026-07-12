package com.castigaro.storyteller.api

import android.content.Context
import com.castigaro.common.llm.ModelPricing
import com.castigaro.common.llm.ProviderSettings
import com.castigaro.storyteller.data.Message
import com.castigaro.storyteller.data.Story
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Anbindung an die Text-Modelle von Anthropic (Messages API) und OpenAI
 * (Chat Completions API) für das Erzählen der Geschichte. Welcher Anbieter
 * genutzt wird, entscheidet die Einstellung des Nutzers.
 */
object StoryApi {

    /** Markierung, mit der das Modell das Ende der Geschichte signalisiert. */
    const val END_MARKER = "[ENDE]"

    /** Präfix der Titelzeile in der allerersten Erzähler-Antwort. */
    const val TITLE_PREFIX = "TITEL:"

    // Bei langen Geschichten nur die jüngsten Beiträge mitschicken — der
    // erste Beitrag (die Welt-Beschreibung) bleibt aber immer erhalten.
    private const val MAX_HISTORY = 40

    private val JSON = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    class ApiException(message: String) : IOException(message)

    private val SYSTEM_PROMPT = """
        Du bist ein interaktiver Geschichtenerzähler. Der allererste Beitrag
        des Spielers beschreibt die Welt bzw. die Prämisse der Geschichte.
        Halte dich an diese Regeln:
        - Erzähle lebendig und bildhaft in der Sprache des Spielers
          (im Zweifel Deutsch), etwa 100 bis 180 Wörter pro Abschnitt.
        - Baue die Geschichte in drei Akten auf: Einführung, Verwicklung mit
          Höhepunkt, Auflösung — mit ungefähr vier bis sechs Abschnitten
          pro Akt.
        - Beende jeden Abschnitt mit einer offenen Frage oder einer
          Entscheidung an den Spieler.
        - Fakten, Figuren und Ideen, die der Spieler nennt, webst du in die
          weitere Handlung ein und bleibst über die ganze Geschichte
          konsistent dazu.
        - Die Geschichte ist für die ganze Familie geeignet: keine drastische
          Gewalt, keine Inhalte nur für Erwachsene. Unpassende Vorschläge
          des Spielers lenkst du charmant in eine harmlose Richtung um.
        - Deine allererste Antwort beginnt mit einer Titelzeile im Format
          "$TITLE_PREFIX <kurzer Titel der Geschichte>", danach folgt eine
          Leerzeile und dann der erste Abschnitt.
        - Wenn die Geschichte nach dem dritten Akt zu einem stimmigen Ende
          gekommen ist, schließe sie ab und setze ganz ans Ende exakt die
          Markierung $END_MARKER.
    """.trimIndent()

    /** Erzeugt den nächsten Erzähl-Abschnitt für die Geschichte. */
    suspend fun nextBeat(context: Context, story: Story): String =
        withContext(Dispatchers.IO) {
            val config = ProviderSettings.activeConfig(context)
                ?: throw ApiException("Kein aktiver API-Key — in den Einstellungen hinterlegen oder aktivieren.")
            val history = trimmedHistory(story)

            if (config.provider == ProviderSettings.PROVIDER_OPENAI) {
                requestOpenAi(context, config.apiKey, config.model, history)
            } else {
                requestAnthropic(context, config.apiKey, config.model, history)
            }
        }

    /** Bucht die geschätzten Kosten eines Aufrufs auf den Zähler des Anbieters. */
    private fun trackCost(
        context: Context,
        provider: String,
        model: String,
        inputTokens: Long,
        outputTokens: Long,
    ) {
        val micros = ModelPricing.estimateCostMicros(provider, model, inputTokens, outputTokens)
        ProviderSettings.addCostMicros(context, provider, micros)
    }

    private fun trimmedHistory(story: Story): List<Message> {
        val all = story.messages
        if (all.size <= MAX_HISTORY) return all
        return listOf(all.first()) + all.takeLast(MAX_HISTORY - 1)
    }

    private fun apiRole(message: Message): String =
        if (message.role == Message.ROLE_NARRATOR) "assistant" else "user"

    private fun requestAnthropic(
        context: Context,
        apiKey: String,
        model: String,
        history: List<Message>,
    ): String {
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 1024)
            put("system", SYSTEM_PROMPT)
            put("messages", JSONArray().also { arr ->
                history.forEach { msg ->
                    arr.put(JSONObject().apply {
                        put("role", apiRole(msg))
                        put("content", msg.text)
                    })
                }
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw ApiException(apiErrorMessage(responseText, response.code))
            }
            val json = JSONObject(responseText)
            json.optJSONObject("usage")?.let { usage ->
                trackCost(
                    context, ProviderSettings.PROVIDER_ANTHROPIC, model,
                    inputTokens = usage.optLong("input_tokens", 0L),
                    outputTokens = usage.optLong("output_tokens", 0L),
                )
            }
            if (json.optString("stop_reason") == "refusal") {
                throw ApiException("Die Anfrage wurde vom Modell abgelehnt.")
            }
            val content = json.getJSONArray("content")
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.getString("type") == "text") {
                    return block.getString("text").trim()
                }
            }
            throw ApiException("Leere Antwort vom Modell.")
        }
    }

    private fun requestOpenAi(
        context: Context,
        apiKey: String,
        model: String,
        history: List<Message>,
    ): String {
        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", 1024)
            put("messages", JSONArray().also { arr ->
                arr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                history.forEach { msg ->
                    arr.put(JSONObject().apply {
                        put("role", apiRole(msg))
                        put("content", msg.text)
                    })
                }
            })
        }

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw ApiException(apiErrorMessage(responseText, response.code))
            }
            val json = JSONObject(responseText)
            json.optJSONObject("usage")?.let { usage ->
                trackCost(
                    context, ProviderSettings.PROVIDER_OPENAI, model,
                    inputTokens = usage.optLong("prompt_tokens", 0L),
                    outputTokens = usage.optLong("completion_tokens", 0L),
                )
            }
            return json
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
        }
    }

    private fun apiErrorMessage(responseText: String, code: Int): String {
        val detail = runCatching {
            JSONObject(responseText).getJSONObject("error").getString("message")
        }.getOrNull()
        return when {
            detail != null -> "API-Fehler ($code): $detail"
            code == 401 -> "API-Key ungültig (401)."
            code == 429 -> "Rate-Limit erreicht (429) — bitte kurz warten."
            else -> "API-Fehler ($code)."
        }
    }
}
