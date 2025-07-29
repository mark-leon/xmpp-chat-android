
package com.example.whatsappclone

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsappclone.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvSender)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
//        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvSender.text = message.sender
        holder.tvMessage.text = message.message

        // Format timestamp
//        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
//        holder.tvTime.text = timeFormat.format(Date(message.timestamp))

        // Style outgoing messages differently
        if (message.isOutgoing) {
            holder.tvSender.setTextColor(Color.BLUE)
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue background
        } else {
            holder.tvSender.setTextColor(Color.BLACK)
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF")) // White background
        }
    }

    override fun getItemCount() = messages.size
}