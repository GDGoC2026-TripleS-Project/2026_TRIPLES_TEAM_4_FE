//깃허브에 폴더 구조를 올리기 위해 임시로 만들어둔 파일입니다.
//개발 과정에 따라 파일을 삭제하거나 파일명을 변경해도 됩니다.
// 파일명 수정 시 연결된 xml 파일명도 수정 필요

package com.project.unimate.ui.cock

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.R
import com.project.unimate.notification.NotificationApi
import com.project.unimate.notification.NotificationItem
import com.project.unimate.notification.NotificationStore

class CockFragment : Fragment() {
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cock, container, false)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.notification_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = NotificationAdapter(
            onCompleteClicked = { item, onResult ->
                val api = NotificationApi()
                api.completeNotification(requireContext(), item.notificationId) { success ->
                    if (success) {
                        val updated = item.copy(isCompleted = true)
                        if (isAdded) {
                            requireActivity().runOnUiThread {
                                NotificationStore.upsert(requireContext(), updated)
                                onResult(updated)
                            }
                        }
                    }
                }
            }
        )
        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val items = NotificationStore.loadAll(requireContext())
        adapter.submit(items)
        syncFromServerIfAvailable()
    }

    private fun syncFromServerIfAvailable() {
        // TODO: 서버 알림 목록 API 연동 시 사용
        // 1) fetchServerList()
        // 2) val merged = NotificationStore.mergeWithServer(local, server)
        // 3) NotificationStore.saveAll(context, merged)
        // 4) adapter.submit(merged)
    }
}
