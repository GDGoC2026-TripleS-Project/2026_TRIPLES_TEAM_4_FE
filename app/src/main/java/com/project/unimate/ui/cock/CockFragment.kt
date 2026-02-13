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

class CockFragment : Fragment() {
    private lateinit var adapter: NotificationAdapter
    private lateinit var emptyView: android.widget.TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cock, container, false)

        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.notification_list)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        emptyView = view.findViewById(R.id.empty_state)
        emptyView.text = getString(R.string.no_schedule)

        adapter = NotificationAdapter(
            onCompleteClicked = { _, _ ->
                // 찌르기 탭에서는 알림 액션을 제공하지 않음
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
