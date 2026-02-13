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
        // 간단한 텍스트 뷰 레이아웃 (android.R.layout.simple_list_item_1 등 사용 가능)
        // 여기서는 직접 코드로 텍스트뷰 생성해서 반환 (xml 안 만들어도 됨)
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