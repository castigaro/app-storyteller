package com.castigaro.storyteller.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class StoryStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun reset() {
        StoryStore.resetForTest()
        File(context.filesDir, "stories.json").delete()
    }

    @Test
    fun `Geschichten ueberleben einen Neustart`() {
        val story = Story(title = "Roundtrip", createdAt = 1L)
        story.messages.add(Message(Message.ROLE_PLAYER, "Welt"))
        StoryStore.add(context, story)

        StoryStore.resetForTest() // simuliert App-Neustart

        val loaded = StoryStore.get(context, story.id)!!
        assertEquals("Roundtrip", loaded.title)
        assertEquals("Welt", loaded.messages.single().text)
    }

    @Test
    fun `getAll sortiert neueste zuerst`() {
        StoryStore.add(context, Story(title = "alt", createdAt = 1L))
        StoryStore.add(context, Story(title = "neu", createdAt = 2L))
        assertEquals(listOf("neu", "alt"), StoryStore.getAll(context).map { it.title })
    }

    @Test
    fun `delete entfernt die Geschichte dauerhaft`() {
        val story = Story(title = "weg damit", createdAt = 1L)
        StoryStore.add(context, story)
        StoryStore.delete(context, story)

        StoryStore.resetForTest()
        assertNull(StoryStore.get(context, story.id))
        assertTrue(StoryStore.getAll(context).isEmpty())
    }
}
