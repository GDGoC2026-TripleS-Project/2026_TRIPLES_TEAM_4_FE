package com.project.unimate.data.repository

import android.content.Context
import android.util.Log

/**
 * 전역 서버 동기화 유틸.
 * 앱 시작·홈 진입·생성/수정 후 호출. 내부적으로 ServerSync를 사용하여
 * 유저정보·팀·팀일정·개인일정을 서버 데이터로 완전 교체.
 * Mutex(ServerSync 내부)가 동시 실행을 직렬화하므로 중복 호출 안전.
 */
object SyncManager {

    private const val TAG = "SyncManager"

    /**
     * 서버에서 팀·일정 전체를 로드하여 DummyRepository를 완전 교체.
     * @return 동기화 성공 여부
     */
    suspend fun syncAllDataFromServer(context: Context): Boolean {
        return try {
            Log.d(TAG, "서버 전체 동기화 시작")
            ServerSync.syncFromServer(context.applicationContext)
            Log.d(TAG, "서버 전체 동기화 완료: 팀 ${DummyRepository.allTeams.size}개, 일정 ${DummyRepository.allTaskItems.size}개")
            true
        } catch (e: Exception) {
            Log.e(TAG, "서버 동기화 실패: ${e.message}", e)
            false
        }
    }
}
