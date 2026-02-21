package com.project.unimate.ui.alarm

// 역할: 알림 목록 RecyclerView 어댑터. 섹션(지금/N일 전)·카드 바인딩, 완료/클릭 콜백

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R
import com.project.unimate.notification.NotificationItem
import com.project.unimate.notification.NotificationUiMapper

class NotificationAdapter(
    private val onCompleteClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit,
    private val onCardClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit,
    private val onMeetingNavigated: (NotificationItem) -> Unit = {}
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
            CardHolder(v, onCompleteClicked, onCardClicked)
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
        private val onCompleteClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit,
        private val onCardClicked: (NotificationItem, (NotificationItem) -> Unit) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val dot: View = itemView.findViewById(R.id.team_dot)
        private val teamName: TextView = itemView.findViewById(R.id.team_name)
        private val title: TextView = itemView.findViewById(R.id.message_title)
        private val body: TextView = itemView.findViewById(R.id.message_body)
        private val button: Button = itemView.findViewById(R.id.complete_button)

        fun bind(item: ListItem.Card) {
            val n = item.notification
            val isMeetingNotification = isMeetingNotification(n)
            // 모임 알림은 항상 흰 카드 유지 (actionDone/isRead 에 따른 회색 카드 처리 제외)
            val useReadTone = n.isRead && !n.actionDone && !isMeetingNotification
            itemView.setBackgroundResource(
                if (useReadTone) R.drawable.bg_notification_card_done
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

            if (isMeetingNotification) {
                button.visibility = View.VISIBLE
                when {
                    !n.action && n.actionDone -> {
                        // 체크 일정 확인 완료: 회색이지만 활성(재진입 가능) "체크 완료"
                        button.isEnabled = true
                        button.text = "체크 완료"
                        button.setBackgroundResource(R.drawable.bg_notification_button_disabled)
                        button.backgroundTintList = null
                        button.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray10))
                        button.setOnClickListener { onMeetingNavigated(n) }
                    }
                    n.action && n.actionDone -> {
                        // 시간 입력 완료: 회색이지만 활성(재진입 가능) "시간 수정하기"
                        button.isEnabled = true
                        button.text = "시간 수정하기"
                        button.setBackgroundResource(R.drawable.bg_notification_button_disabled)
                        button.backgroundTintList = null
                        button.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray10))
                        button.setOnClickListener { onMeetingNavigated(n) }
                    }
                    else -> {
                        // 미완료: 연두색 활성 버튼 (체크 일정 확인하기 / 시간 입력하기)
                        button.isEnabled = true
                        button.text = meetingButtonText(n)
                        button.setBackgroundResource(R.drawable.bg_notification_button_enabled)
                        button.backgroundTintList = null
                        button.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray10))
                        button.setOnClickListener {
                            if (!n.action) {
                                // 체크 일정 확인하기(action=false): 클릭 즉시 완료 처리 후 이동
                                onCompleteClicked(n) { updated ->
                                    updateItem(updated)
                                    onMeetingNavigated(updated)
                                }
                            } else {
                                // 시간 입력하기(action=true): 이동만 하고 완료는 저장 시점에 처리
                                onMeetingNavigated(n)
                            }
                        }
                    }
                }
            } else if (!n.action) {
                button.visibility = View.GONE
                button.setOnClickListener(null)
            } else if (n.actionDone) {
                button.visibility = View.VISIBLE
                button.isEnabled = false
                button.text = "콕누르기 완료"
                button.setBackgroundResource(R.drawable.bg_notification_button_disabled)
                button.backgroundTintList = null
                button.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray10))
                button.setOnClickListener(null)
            } else {
                button.visibility = View.VISIBLE
                button.isEnabled = true
                button.text = "확인 콕누르기"
                button.setBackgroundResource(R.drawable.bg_notification_button_enabled)
                button.backgroundTintList = null
                button.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray10))
                button.setOnClickListener {
                    onCompleteClicked(n) { updated ->
                        updateItem(updated)
                    }
                }
            }

            itemView.setOnClickListener {
                if (isMeetingNotification) {
                    // 모임 알림은 하단 CTA 버튼으로만 이동
                    return@setOnClickListener
                }
                if (n.action) {
                    if (!n.actionDone || n.isRead) return@setOnClickListener
                } else {
                    if (n.isRead) return@setOnClickListener
                }
                onCardClicked(n) { updated ->
                    updateItem(updated)
                }
            }
        }

        private fun isMeetingNotification(n: NotificationItem): Boolean {
            val alarmType = n.alarmType.lowercase()
            val title = n.messageTitle.lowercase()
            val body = n.messageBody.lowercase()
            return alarmType.contains("meeting") ||
                alarmType.contains("timepick") ||
                alarmType.contains("모임") ||
                title.contains("모임") ||
                title.contains("체크요청") ||
                body.contains("모임") ||
                body.contains("시간 입력") ||
                body.contains("체크")
        }

        private fun meetingButtonText(n: NotificationItem): String {
            val alarmType = n.alarmType.lowercase()
            val text = "${n.messageTitle} ${n.messageBody}".lowercase()
            val isRequest = n.action ||
                alarmType.contains("request") ||
                alarmType.contains("meeting_request") ||
                text.contains("체크요청") ||
                text.contains("시간 체크")

            return if (isRequest) "시간 입력하기" else "체크 일정 확인하기"
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
