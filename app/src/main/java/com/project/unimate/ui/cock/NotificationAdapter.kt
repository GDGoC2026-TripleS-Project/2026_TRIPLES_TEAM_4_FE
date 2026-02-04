package com.project.unimate.ui.cock

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R
import com.project.unimate.notification.NotificationItem

class NotificationAdapter(
    private val onCompleteClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ListItem>()

    fun submit(notifications: List<NotificationItem>) {
        val sorted = notifications.sortedByDescending { it.createdAtMillis() }
        val grouped = sorted.groupBy { it.alarmType }

        items.clear()
        for ((section, list) in grouped) {
            items.add(ListItem.Section(section))
            for (n in list) {
                items.add(ListItem.Card(n))
            }
        }
        notifyDataSetChanged()
    }

    fun updateItem(updated: NotificationItem) {
        val idx = items.indexOfFirst {
            it is ListItem.Card && it.notification.notificationId == updated.notificationId
        }
        if (idx >= 0) {
            items[idx] = ListItem.Card(updated)
            notifyItemChanged(idx)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Section -> VIEW_SECTION
            is ListItem.Card -> VIEW_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SECTION) {
            val v = inflater.inflate(R.layout.item_notification_section, parent, false)
            SectionHolder(v)
        } else {
            val v = inflater.inflate(R.layout.item_notification_card, parent, false)
            CardHolder(v, onCompleteClicked)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Section -> (holder as SectionHolder).bind(item)
            is ListItem.Card -> (holder as CardHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class SectionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.section_title)
        fun bind(item: ListItem.Section) {
            val raw = item.title.trim()
            title.text = if (raw.startsWith("찌르기 ")) raw else "찌르기 $raw"
        }
    }

    class CardHolder(
        itemView: View,
        private val onCompleteClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val dot: View = itemView.findViewById(R.id.team_dot)
        private val teamName: TextView = itemView.findViewById(R.id.team_name)
        private val title: TextView = itemView.findViewById(R.id.message_title)
        private val body: TextView = itemView.findViewById(R.id.message_body)
        private val button: Button = itemView.findViewById(R.id.complete_button)

        fun bind(item: ListItem.Card) {
            val n = item.notification
            itemView.setBackgroundResource(
                if (n.isCompleted) R.drawable.bg_notification_card_done
                else R.drawable.bg_notification_card_unread
            )
            teamName.text = n.teamName.ifBlank { "Unknown" }
            title.text = n.messageTitle
            body.text = n.messageBody

            val bg = dot.background
            if (bg is GradientDrawable) {
                try {
                    val color = n.teamColorHex.ifBlank { "#CCCCCC" }
                    bg.setColor(Color.parseColor(color))
                } catch (e: Exception) {
                    bg.setColor(Color.parseColor("#CCCCCC"))
                }
            }

            if (n.isCompleted) {
                button.isEnabled = false
                button.text = "콕누르기 완료"
                button.setBackgroundResource(R.drawable.bg_notification_button_disabled)
                button.setOnClickListener(null)
            } else {
                button.isEnabled = true
                button.text = "확인 콕누르기"
                button.setBackgroundResource(R.drawable.bg_notification_button_enabled)
                button.setOnClickListener {
                    onCompleteClicked(n) { updated ->
                        bind(ListItem.Card(updated))
                    }
                }
            }
        }
    }

    sealed class ListItem {
        data class Section(val title: String) : ListItem()
        data class Card(val notification: NotificationItem) : ListItem()
    }

    companion object {
        private const val VIEW_SECTION = 1
        private const val VIEW_CARD = 2
    }
}
