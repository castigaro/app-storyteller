package com.castigaro.storyteller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.castigaro.storyteller.data.Message
import com.castigaro.storyteller.databinding.ItemMsgNarratorBinding
import com.castigaro.storyteller.databinding.ItemMsgPlayerBinding

/** Chat-Verlauf der Geschichte: Erzähler links, Spieler rechts. */
class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<Message>()

    fun submit(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].role == Message.ROLE_NARRATOR) TYPE_NARRATOR else TYPE_PLAYER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_NARRATOR) {
            NarratorHolder(ItemMsgNarratorBinding.inflate(inflater, parent, false))
        } else {
            PlayerHolder(ItemMsgPlayerBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is NarratorHolder -> holder.binding.messageText.text = message.text
            is PlayerHolder -> holder.binding.messageText.text = message.text
        }
    }

    class NarratorHolder(val binding: ItemMsgNarratorBinding) :
        RecyclerView.ViewHolder(binding.root)

    class PlayerHolder(val binding: ItemMsgPlayerBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_NARRATOR = 0
        private const val TYPE_PLAYER = 1
    }
}
