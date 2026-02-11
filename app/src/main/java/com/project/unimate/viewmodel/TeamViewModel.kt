package com.project.unimate.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// : ViewModel() 이 부분이 가장 중요해요! (상속)
class TeamViewModel : ViewModel() {

    // 1. 팀 이름 (필수)
    val teamName = MutableLiveData<String>()

    // 2. 팀 소개 (선택)
    val teamDescription = MutableLiveData<String>()

    // 3. 선택된 팀 컬러 (기존 디자인의 노란색을 기본값으로 설정)
    val selectedColor = MutableLiveData<String>("#D8F28B")

    // 4. 생성된 초대 코드
    val inviteCode = MutableLiveData<String>()
}