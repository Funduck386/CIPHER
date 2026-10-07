package com.example.cipher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2
    }

    class BubbleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvBubbleText)
        val tvTime: TextView = view.findViewById(R.id.tvBubbleTime)
    }

    override fun getItemViewType(position: Int): Int =
        if (messages[position].isSentByMe) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutRes = if (viewType == TYPE_SENT) R.layout.item_bubble_sent else R.layout.item_bubble_received
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return BubbleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        holder as BubbleViewHolder
        holder.tvText.text = message.text
        holder.tvTime.text = message.timestamp
    }

    override fun getItemCount(): Int = messages.size
}