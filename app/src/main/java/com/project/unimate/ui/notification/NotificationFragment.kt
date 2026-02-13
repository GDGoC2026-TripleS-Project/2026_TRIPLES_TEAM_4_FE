package com.project.unimate.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.R
import com.project.unimate.notification.NotificationApi
import com.project.unimate.notification.NotificationStore
import com.project.unimate.ui.cock.NotificationAdapter

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
                api.markActionDone(requireContext(), item.notificationId) { success ->
                    if (success && isAdded) {
                        val updated = item.copy(actionDone = true)
                        requireActivity().runOnUiThread {
                            NotificationStore.upsert(requireContext(), updated)
                            onResult(updated)
                        }
                    }
                }
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
}
