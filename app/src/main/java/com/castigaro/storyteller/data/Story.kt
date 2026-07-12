package com.castigaro.storyteller.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Ein einzelner Beitrag im Geschichtsverlauf. */
class Message(
    val role: String, // ROLE_PLAYER oder ROLE_NARRATOR
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("role", role)
        put("text", text)
        put("createdAt", createdAt)
    }

    companion object {
        const val ROLE_PLAYER = "player"
        const val ROLE_NARRATOR = "narrator"

        fun fromJson(json: JSONObject) = Message(
            role = json.optString("role", ROLE_PLAYER),
            text = json.optString("text"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
        )
    }
}

/**
 * Eine Geschichte: die Welt-Beschreibung des Spielers, der bisherige
 * Erzählverlauf und der Status. Der erste Eintrag in [messages] ist immer
 * die Welt-Beschreibung des Spielers.
 */
class Story(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var finished: Boolean = false,
    val messages: MutableList<Message> = mutableListOf(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt)
        put("finished", finished)
        put("messages", JSONArray().also { arr -> messages.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): Story {
            val story = Story(
                id = json.optString("id", UUID.randomUUID().toString()),
                title = json.optString("title"),
                createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                finished = json.optBoolean("finished", false),
            )
            val arr = json.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until arr.length()) {
                story.messages.add(Message.fromJson(arr.getJSONObject(i)))
            }
            return story
        }
    }
}
