package com.appsonar.storyteller.api

import com.appsonar.storyteller.data.Message
import com.appsonar.storyteller.data.Story
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoryApiTest {

    private fun storyWith(count: Int): Story {
        val story = Story(title = "Test")
        story.messages.add(Message(Message.ROLE_PLAYER, "welt"))
        for (i in 2..count) {
            story.messages.add(Message(Message.ROLE_NARRATOR, "m$i"))
        }
        return story
    }

    @Test
    fun `bis 40 Beitraege bleibt der Verlauf unveraendert`() {
        val story = storyWith(40)
        assertEquals(story.messages, StoryApi.trimmedHistory(story))
    }

    @Test
    fun `ueber 40 Beitraegen bleibt die Welt-Beschreibung immer erhalten`() {
        val story = storyWith(41)
        val trimmed = StoryApi.trimmedHistory(story)

        assertEquals(40, trimmed.size)
        assertEquals("welt", trimmed.first().text)
        // Der zweite Beitrag fällt weg, danach geht es lückenlos weiter.
        assertEquals("m3", trimmed[1].text)
        assertEquals("m41", trimmed.last().text)
    }
}
