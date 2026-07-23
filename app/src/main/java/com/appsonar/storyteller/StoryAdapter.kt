package com.appsonar.storyteller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.appsonar.storyteller.data.Story
import com.appsonar.storyteller.databinding.RowStoryBinding
import java.text.DateFormat
import java.util.Date

class StoryAdapter(
    private val onClick: (Story) -> Unit,
    private val onLongClick: (Story) -> Unit,
) : RecyclerView.Adapter<StoryAdapter.Holder>() {

    private val stories = mutableListOf<Story>()

    fun submit(newStories: List<Story>) {
        stories.clear()
        stories.addAll(newStories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = RowStoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return Holder(binding)
    }

    override fun getItemCount(): Int = stories.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(stories[position])
    }

    inner class Holder(private val binding: RowStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(story: Story) {
            val context = binding.root.context
            binding.storyTitle.text =
                story.title.ifBlank { context.getString(R.string.untitled_story) }

            val date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(Date(story.createdAt))
            val status = context.getString(
                if (story.finished) R.string.story_status_finished
                else R.string.story_status_running
            )
            binding.storySubtitle.text =
                context.getString(R.string.story_subtitle, date, status)

            binding.root.setOnClickListener { onClick(story) }
            binding.root.setOnLongClickListener { onLongClick(story); true }
        }
    }
}
