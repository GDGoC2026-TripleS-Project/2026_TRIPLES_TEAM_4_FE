package com.project.unimate.ui.poke

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MsgViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        val textView = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(40, 30, 40, 30) // 패딩 설정
            textSize = 15f
            setTextColor(android.graphics.Color.BLACK)
        }
        return MsgViewHolder(textView)
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(msg: String) {
            (itemView as TextView).text = msg
            itemView.setOnClickListener { onItemClick(msg) }
        }
    }
}