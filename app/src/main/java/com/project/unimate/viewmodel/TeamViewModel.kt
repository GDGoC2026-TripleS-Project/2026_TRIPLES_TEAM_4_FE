package com.project.unimate.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// 역할: 팀 생성 화면 입력 상태. ViewModel 상속으로 화면 재생성 시 값 유지
class TeamViewModel : ViewModel() {

    val teamName = MutableLiveData<String>()

    val teamDescription = MutableLiveData<String>()

    val selectedColor = MutableLiveData<String>("#D8F28B")

    val inviteCode = MutableLiveData<String>()
}