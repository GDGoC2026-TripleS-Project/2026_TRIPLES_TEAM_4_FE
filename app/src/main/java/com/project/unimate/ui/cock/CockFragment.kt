//깃허브에 폴더 구조를 올리기 위해 임시로 만들어둔 파일입니다.
//개발 과정에 따라 파일을 삭제하거나 파일명을 변경해도 됩니다.
// 파일명 수정 시 연결된 xml 파일명도 수정 필요

package com.project.unimate.ui.cock

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.project.unimate.R

class CockFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cock, container, false)
    }

}