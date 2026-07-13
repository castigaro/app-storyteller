package com.castigaro.storyteller.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoryTest {

    @Test
    fun `Message-Roundtrip erhaelt alle Felder`() {
        val message = Message(role = Message.ROLE_NARRATOR, text = "Es war einmal …", createdAt = 42L)
        val loaded = Message.fromJson(message.toJson())
        assertEquals(Message.ROLE_NARRATOR, loaded.role)
        assertEquals("Es war einmal …", loaded.text)
        assertEquals(42L, loaded.createdAt)
    }

    @Test
    fun `Story-Roundtrip erhaelt Verlauf und Status`() {
        val story = Story(title = "Die Reise", createdAt = 7L, finished = true)
        story.messages.add(Message(Message.ROLE_PLAYER, "Eine Wüstenwelt"))
        story.messages.add(Message(Message.ROLE_NARRATOR, "Der Sand glühte …"))

        val loaded = Story.fromJson(story.toJson())
        assertEquals(story.id, loaded.id)
        assertEquals("Die Reise", loaded.title)
        assertTrue(loaded.finished)
        assertEquals(2, loaded.messages.size)
        assertEquals(Message.ROLE_PLAYER, loaded.messages[0].role)
        assertEquals("Der Sand glühte …", loaded.messages[1].text)
    }

    @Test
    fun `spaerliche JSON-Daten bekommen Defaults`() {
        val loaded = Story.fromJson(JSONObject("""{"title":"Nur Titel"}"""))
        assertEquals("Nur Titel", loaded.title)
        assertFalse(loaded.finished)
        assertTrue(loaded.messages.isEmpty())
        assertTrue("Id muss generiert werden", loaded.id.isNotBlank())
    }
}
