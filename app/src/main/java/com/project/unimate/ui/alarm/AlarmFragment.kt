// 역할: 알림(찌르기) 탭. 찌르기에서는 알림 액션·읽음 처리 미제공

package com.project.unimate.ui.alarm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.R

class AlarmFragment : Fragment() {
    private lateinit var adapter: NotificationAdapter
    private lateinit var emptyView: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.notification_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        emptyView = view.findViewById(R.id.empty_state)
        emptyView.text = getString(R.string.no_schedule)

        adapter = NotificationAdapter(
            onCompleteClicked = { _, _ ->
                // 정책: 찌르기 탭에서는 알림 액션 미제공
            },
            onCardClicked = { _, _ ->
                // 정책: 찌르기 탭에서는 읽음 처리 미제공
            }
        )
        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.submit(emptyList())
        emptyView.visibility = View.VISIBLE
    }
}
