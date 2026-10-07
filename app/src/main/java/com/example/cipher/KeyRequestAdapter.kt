package com.example.cipher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KeyRequestAdapter(
    private val requests: List<KeyRequest>,
    private val onSendKey: (KeyRequest) -> Unit
) : RecyclerView.Adapter<KeyRequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRequesterName: TextView = view.findViewById(R.id.tvRequesterName)
        val tvRequestedAt: TextView = view.findViewById(R.id.tvRequestedAt)
        val btnSendKey: TextView = view.findViewById(R.id.btnSendKey)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_key_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]
        holder.tvRequesterName.text = request.fromDisplayName
        holder.tvRequestedAt.text = "requested your current key • ${request.requestedAt}"
        holder.btnSendKey.setOnClickListener { onSendKey(request) }
    }

    override fun getItemCount(): Int = requests.size
}