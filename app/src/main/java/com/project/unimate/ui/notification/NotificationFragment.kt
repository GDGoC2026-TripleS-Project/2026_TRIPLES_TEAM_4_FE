package com.project.unimate.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.R
import com.project.unimate.notification.NotificationApi
import com.project.unimate.notification.NotificationItem
import com.project.unimate.notification.NotificationStore
import com.project.unimate.ui.alarm.NotificationAdapter
import com.project.unimate.ui.timepick.TimepickPollSync
import com.project.unimate.ui.timepick.TimepickStateHolder
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private lateinit var adapter: NotificationAdapter
    private lateinit var emptyView: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notification, container, false)
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.notification_list)
        emptyView = view.findViewById(R.id.empty_state)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(
            onCompleteClicked = { item, onResult ->
                val api = NotificationApi()
                api.markActionDone(requireContext(), item.notificationId) { actionDoneOk ->
                    if (!actionDoneOk || !isAdded) return@markActionDone
                    api.markRead(requireContext(), item.notificationId) { readOk ->
                        if (!isAdded) return@markRead
                        val updated = item.copy(actionDone = true, isRead = readOk || item.isRead)
                        requireActivity().runOnUiThread {
                            NotificationStore.upsert(requireContext(), updated)
                            onResult(updated)
                        }
                    }
                }
            },
            onCardClicked = { item, onResult ->
                val api = NotificationApi()
                api.markRead(requireContext(), item.notificationId) { success ->
                    if (!isAdded) return@markRead
                    requireActivity().runOnUiThread {
                        if (success) {
                            val updated = item.copy(isRead = true)
                            NotificationStore.upsert(requireContext(), updated)
                            onResult(updated)
                        }
                    }
                }
            },
            onMeetingNavigated = { item ->
                navigateByNotification(item)
            }
        )
        recyclerView.adapter = adapter

        return view
    }

    //뒤로가기 버튼
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.notificationBack).setOnClickListener {
            findNavController().popBackStack()
        }

        val items = NotificationStore.loadAll(requireContext())
        render(items)
        syncFromServerIfAvailable()
    }

    override fun onResume() {
        super.onResume()
        syncFromServerIfAvailable()
    }

    private fun syncFromServerIfAvailable() {
        val api = NotificationApi()
        api.getNotifications(requireContext()) { server ->
            if (!isAdded) return@getNotifications
            val local = NotificationStore.loadAll(requireContext())
            val merged = NotificationStore.mergeWithServer(local, server)
            requireActivity().runOnUiThread {
                for (item in merged) {
                    NotificationStore.upsert(requireContext(), item)
                }
                render(merged)
            }
        }
    }

    private fun render(items: List<com.project.unimate.notification.NotificationItem>) {
        adapter.submit(items)
        emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun navigateByNotification(item: NotificationItem) {
        if (!isTimepickNotification(item)) return

        val teamId = item.teamId.takeIf { it > 0 }?.toString().orEmpty()
        if (teamId.isNotBlank()) {
            TimepickStateHolder.teamId = teamId
        }
        val pollId = item.meetingPollId
        val target = item.meetingNavigationTarget

        if (pollId != null && pollId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                if (!isAdded) return@launch
                TimepickPollSync.refreshFromServer(requireContext(), pollId)
                if (!isAdded) return@launch
                navigateToMeetingScreen(target, item)
            }
            return
        }
        navigateToMeetingScreen(target, item)
    }

    private fun navigateToMeetingScreen(target: String?, item: NotificationItem) {
        val normalized = target?.trim()?.uppercase()
        val destination = when (normalized) {
            "TIMEPICK_STATUS" -> R.id.timepickStatusFragment
            "TIMEPICK_RESULT" -> R.id.timepickResultFragment
            "EDIT_TIMEPICK" -> R.id.editTimepickFragment
            else -> if (item.action && !item.actionDone) R.id.timepickStatusFragment else R.id.editTimepickFragment
        }
        runCatching {
            if (destination == R.id.editTimepickFragment) {
                findNavController().navigate(
                    destination,
                    Bundle().apply { putString("taskId", "") }
                )
            } else {
                findNavController().navigate(destination)
            }
        }
    }

    private fun isTimepickNotification(item: NotificationItem): Boolean {
        val alarmType = item.alarmType.lowercase()
        val title = item.messageTitle.lowercase()
        val body = item.messageBody.lowercase()
        return alarmType.contains("meeting_request") ||
            alarmType.contains("meeting") ||
            alarmType.contains("timepick") ||
            alarmType.contains("모임") ||
            alarmType.contains("체크요청") ||
            title.contains("타임픽") ||
            title.contains("모이기") ||
            title.contains("모임") ||
            title.contains("체크요청") ||
            body.contains("타임픽") ||
            body.contains("모이기") ||
            body.contains("모임") ||
            body.contains("체크요청") ||
            body.contains("시간 입력")
    }
}
