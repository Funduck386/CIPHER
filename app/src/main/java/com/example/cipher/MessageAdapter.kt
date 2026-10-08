package com.example.cipher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<EncryptedMessage>,
    private val onClick: (EncryptedMessage) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSenderKey: TextView = view.findViewById(R.id.tvSenderKey)
        val tvReceivedAt: TextView = view.findViewById(R.id.tvReceivedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvSenderKey.text = message.decryptedPreview
        holder.tvReceivedAt.text = message.receivedAt
        holder.itemView.setOnClickListener { onClick(message) }
    }

    override fun getItemCount(): Int = messages.size
}