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
import com.project.unimate.notification.NotificationUiMapper

class NotificationAdapter(
    private val onCompleteClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val sourceItems = mutableListOf<NotificationItem>()
    private val items = mutableListOf<ListItem>()

    fun submit(notifications: List<NotificationItem>) {
        sourceItems.clear()
        sourceItems.addAll(notifications)
        rebuildUiItems()
    }

    fun updateItem(updated: NotificationItem) {
        val idx = sourceItems.indexOfFirst { it.notificationId == updated.notificationId }
        if (idx >= 0) {
            sourceItems[idx] = updated
            rebuildUiItems()
        }
    }

    private fun rebuildUiItems() {
        val sections = NotificationUiMapper.toSections(sourceItems)
        items.clear()
        for (section in sections) {
            items.add(ListItem.Section(section.title))
            for (n in section.items) {
                items.add(ListItem.Card(n))
            }
        }
        notifyDataSetChanged()
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
            title.text = item.title
        }
    }

    inner class CardHolder(
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
            val done = if (n.action) n.actionDone else n.isRead
            itemView.setBackgroundResource(
                if (done) R.drawable.bg_notification_card_done
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

            if (!n.action) {
                button.visibility = View.GONE
                button.setOnClickListener(null)
            } else if (n.actionDone) {
                button.visibility = View.VISIBLE
                button.isEnabled = false
                button.text = "콕누르기 완료"
                button.setBackgroundResource(R.drawable.bg_notification_button_disabled)
                button.setOnClickListener(null)
            } else {
                button.visibility = View.VISIBLE
                button.isEnabled = true
                button.text = "확인 콕누르기"
                button.setBackgroundResource(R.drawable.bg_notification_button_enabled)
                button.setOnClickListener {
                    onCompleteClicked(n) { updated ->
                        updateItem(updated)
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
