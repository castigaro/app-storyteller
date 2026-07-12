package com.castigaro.storyteller.data

import android.content.Context
import org.json.JSONArray
import java.io.File

/**
 * Einfacher JSON-Datei-Speicher für die Geschichten — gleiche Bauart wie
 * bei den anderen AppSonar-Apps: für überschaubare Datenmengen robuster
 * und schlanker als eine Datenbank.
 */
object StoryStore {

    private var stories: MutableList<Story>? = null

    private fun file(context: Context): File = File(context.filesDir, "stories.json")

    private fun load(context: Context): MutableList<Story> {
        stories?.let { return it }
        val loaded = mutableListOf<Story>()
        val f = file(context)
        if (f.exists()) {
            runCatching {
                val arr = JSONArray(f.readText())
                for (i in 0 until arr.length()) {
                    loaded.add(Story.fromJson(arr.getJSONObject(i)))
                }
            }
        }
        stories = loaded
        return loaded
    }

    fun save(context: Context) {
        val arr = JSONArray()
        load(context).forEach { arr.put(it.toJson()) }
        file(context).writeText(arr.toString())
    }

    fun getAll(context: Context): List<Story> =
        load(context).sortedByDescending { it.createdAt }

    fun get(context: Context, id: String): Story? =
        load(context).firstOrNull { it.id == id }

    fun add(context: Context, story: Story) {
        load(context).add(story)
        save(context)
    }

    fun delete(context: Context, story: Story) {
        load(context).removeAll { it.id == story.id }
        save(context)
    }
}
