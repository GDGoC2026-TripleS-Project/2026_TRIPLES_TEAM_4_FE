//깃허브에 폴더 구조를 올리기 위해 임시로 만들어둔 파일입니다.
//개발 과정에 따라 파일을 삭제하거나 파일명을 변경해도 됩니다.
// base 폴더의 경우 필요 시 BaseActivity도 필요할 수 있음

// BaseFragment.kt: 프래그먼트들의 기본틀이 될 코드를 작성하는 파일

package com.project.unimate.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding


abstract class BaseFragment<VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup() // 화면 초기 설정 위치
    }

    abstract fun setup() // 상속받는 프래그먼트에서 내용을 채울 함수

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}