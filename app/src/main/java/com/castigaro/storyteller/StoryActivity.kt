package com.castigaro.storyteller

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.castigaro.storyteller.api.StoryApi
import com.castigaro.storyteller.data.AppSettings
import com.castigaro.storyteller.data.Message
import com.castigaro.storyteller.data.Story
import com.castigaro.storyteller.data.StoryStore
import com.castigaro.storyteller.databinding.ActivityStoryBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

class StoryActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityStoryBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var story: Story

    private var generating = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val speechInput = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) {
                val existing = binding.inputReply.text?.toString().orEmpty()
                val combined = if (existing.isBlank()) spoken else "$existing $spoken"
                binding.inputReply.setText(combined)
                binding.inputReply.setSelection(combined.length)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val loaded = StoryStore.get(this, intent.getStringExtra(EXTRA_STORY_ID).orEmpty())
        if (loaded == null) {
            finish()
            return
        }
        story = loaded
        updateTitle()

        adapter = MessageAdapter()
        binding.messageList.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messageList.adapter = adapter
        renderMessages()

        binding.buttonSend.setOnClickListener { sendReply() }
        binding.buttonMic.setOnClickListener { startSpeechInput() }

        tts = TextToSpeech(this, this)

        if (story.finished) {
            setFinishedUi()
        } else if (story.messages.lastOrNull()?.role == Message.ROLE_PLAYER) {
            // Neue Geschichte — oder die letzte Erzähler-Antwort fehlt noch
            // (z. B. App zwischendurch beendet): weitererzählen lassen.
            generateNextBeat()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    // ---- Erzähl-Schleife ----

    private fun sendReply() {
        if (generating || story.finished) return
        val text = binding.inputReply.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.inputReply.setText("")
        story.messages.add(Message(Message.ROLE_PLAYER, text))
        StoryStore.save(this)
        renderMessages()
        generateNextBeat()
    }

    private fun generateNextBeat() {
        generating = true
        setBusy(true)
        lifecycleScope.launch {
            try {
                var text = StoryApi.nextBeat(this@StoryActivity, story)

                // Titelzeile der allerersten Antwort übernehmen.
                if (story.title.isBlank() && text.startsWith(StoryApi.TITLE_PREFIX)) {
                    val newline = text.indexOf('\n')
                    if (newline > 0) {
                        story.title = text.substring(StoryApi.TITLE_PREFIX.length, newline).trim()
                        text = text.substring(newline + 1).trim()
                    }
                }
                if (story.title.isBlank()) {
                    story.title = story.messages.first().text.take(40)
                }

                // Ende-Markierung erkennen und entfernen.
                if (text.contains(StoryApi.END_MARKER)) {
                    text = text.replace(StoryApi.END_MARKER, "").trim()
                    story.finished = true
                }

                story.messages.add(Message(Message.ROLE_NARRATOR, text))
                StoryStore.save(this@StoryActivity)
                updateTitle()
                renderMessages()
                speakIfEnabled(text)
                if (story.finished) setFinishedUi()
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    getString(R.string.story_error, e.message ?: "?"),
                    Snackbar.LENGTH_INDEFINITE,
                )
                    .setAction(R.string.retry) { generateNextBeat() }
                    .show()
            } finally {
                generating = false
                setBusy(false)
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.progress.visibility = if (busy) View.VISIBLE else View.GONE
        binding.statusText.visibility = binding.progress.visibility
        binding.inputReply.isEnabled = !busy && !story.finished
        binding.buttonSend.isEnabled = !busy && !story.finished
        binding.buttonMic.isEnabled = !busy && !story.finished
    }

    private fun setFinishedUi() {
        binding.inputRow.visibility = View.GONE
        binding.finishedText.visibility = View.VISIBLE
    }

    private fun renderMessages() {
        adapter.submit(story.messages.toList())
        binding.messageList.scrollToPosition(adapter.itemCount - 1)
    }

    private fun updateTitle() {
        supportActionBar?.title =
            story.title.ifBlank { getString(R.string.untitled_story) }
    }

    // ---- Spracheingabe (System-Dialog, keine eigene Berechtigung nötig) ----

    private fun startSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
        }
        runCatching { speechInput.launch(intent) }
            .onFailure {
                Toast.makeText(this, R.string.speech_unavailable, Toast.LENGTH_LONG).show()
            }
    }

    // ---- Vorlesen (Android-TTS) ----

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun speakIfEnabled(text: String) {
        if (!AppSettings.isTtsEnabled(this) || !ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "beat")
    }

    // ---- Menü ----

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_story, menu)
        menu.findItem(R.id.action_tts).isChecked = AppSettings.isTtsEnabled(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_tts -> {
            val enabled = !item.isChecked
            item.isChecked = enabled
            AppSettings.setTtsEnabled(this, enabled)
            if (enabled) {
                // Zum Ausprobieren gleich den letzten Erzähl-Abschnitt vorlesen.
                story.messages.lastOrNull { it.role == Message.ROLE_NARRATOR }
                    ?.let { speakIfEnabled(it.text) }
            } else {
                tts?.stop()
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_STORY_ID = "storyId"
    }
}
