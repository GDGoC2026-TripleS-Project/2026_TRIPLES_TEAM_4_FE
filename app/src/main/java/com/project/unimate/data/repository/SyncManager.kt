package com.project.unimate.data.repository

import android.content.Context
import android.util.Log

/**
 * 전역 서버 동기화. 앱 시작·홈 진입·생성/수정 후 호출.
 * ServerSync로 유저·팀·팀일정·개인일정 서버 데이터 완전 교체. Mutex로 중복 호출 방지.
 */
object SyncManager {

    private const val TAG = "SyncManager"

    /** 서버에서 팀·일정 전체 로드 후 DummyRepository 완전 교체. 반환: 성공 여부 */
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
