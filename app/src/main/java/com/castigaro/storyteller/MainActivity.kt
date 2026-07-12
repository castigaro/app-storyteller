package com.castigaro.storyteller

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.castigaro.common.update.UpdateChecker
import com.castigaro.common.update.UpdateUi
import com.castigaro.storyteller.data.AppSettings
import com.castigaro.storyteller.data.Message
import com.castigaro.storyteller.data.Story
import com.castigaro.storyteller.data.StoryStore
import com.castigaro.storyteller.databinding.ActivityMainBinding
import com.castigaro.storyteller.databinding.DialogNewStoryBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StoryAdapter
    private val updateChecker = UpdateChecker(VERSION_URL, BuildConfig.VERSION_CODE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = StoryAdapter(
            onClick = { story -> openStory(story) },
            onLongClick = { story -> confirmDelete(story) },
        )
        binding.storyList.layoutManager = LinearLayoutManager(this)
        binding.storyList.adapter = adapter

        binding.fabNewStory.setOnClickListener { startNewStory() }

        // Stille Prüfung beim Start — zeigt nur etwas, wenn eine neue Version da ist.
        UpdateUi.checkSilently(this, binding.root, updateChecker)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val stories = StoryStore.getAll(this)
        adapter.submit(stories)
        binding.emptyState.visibility =
            if (stories.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun startNewStory() {
        if (!AppSettings.isConfigured(this)) {
            Snackbar.make(binding.root, R.string.needs_key, Snackbar.LENGTH_LONG)
                .setAction(R.string.action_settings) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                .show()
            return
        }

        val dialogBinding = DialogNewStoryBinding.inflate(layoutInflater)
        AlertDialog.Builder(this)
            .setTitle(R.string.new_story_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.new_story_start) { _, _ ->
                val world = dialogBinding.inputWorld.text.toString().trim()
                if (world.isNotEmpty()) {
                    val story = Story()
                    story.messages.add(Message(Message.ROLE_PLAYER, world))
                    StoryStore.add(this, story)
                    openStory(story)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openStory(story: Story) {
        startActivity(
            Intent(this, StoryActivity::class.java)
                .putExtra(StoryActivity.EXTRA_STORY_ID, story.id)
        )
    }

    private fun confirmDelete(story: Story) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_story)
            .setMessage(
                getString(
                    R.string.delete_story_message,
                    story.title.ifBlank { getString(R.string.untitled_story) },
                )
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                StoryStore.delete(this, story)
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_help -> {
            startActivity(Intent(this, HelpActivity::class.java))
            true
        }
        R.id.action_check_update -> {
            UpdateUi.checkManually(this, binding.root, updateChecker)
            true
        }
        R.id.action_settings -> {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val VERSION_URL =
            "https://appsonar.de/downloads/storyteller-version.json"
    }
}
