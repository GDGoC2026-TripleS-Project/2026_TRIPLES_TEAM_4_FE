package com.project.unimate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.project.unimate.auth.FcmRegistrar
import com.project.unimate.auth.JwtStore

class MainActivity : AppCompatActivity() {

    private val TAG = "UnimateFCM"
    private val BASE_URL = "https://seok-hwan1.duckdns.org"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전체 화면 설정 (Edge-to-Edge)
        enableEdgeToEdge()

        // 1. 레이아웃 설정
        setContentView(R.layout.activity_main)

        // 2. 내비게이션 및 하단바 가시성 설정
        try {
            setupNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation Setup Error: ${e.message}")
        }

        // 3. 권한 및 푸시 알림 설정
        requestNotificationPermissionIfNeeded()
        handlePushIntent(intent)

        // ✅ 여기 " " 안에 Swagger에서 받은 JWT를 그대로 붙여넣기 (Bearer 붙이지 말 것)
        val TEST_JWT = ""

        if (TEST_JWT.isNotBlank()) {
            val token = TEST_JWT.trim().removePrefix("Bearer ").trim()
            JwtStore.save(this, token)
            val after = JwtStore.load(this)
            Log.d(TAG, "✅ TEST_JWT injected. afterLoad len=${after?.length ?: 0}, head=${after?.take(12)}")
        }

        val jwt = JwtStore.load(this)
        Log.d(TAG, "JWT exists? ${!jwt.isNullOrBlank()}")
        FcmRegistrar.registerIfPossible(this, BASE_URL)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 바텀 네비게이션을 감싸고 있는 CardView (그림자 및 라운드 처리를 위해 사용 중인 뷰)
        val navCardView = findViewById<MaterialCardView>(R.id.nav_card_view)

        if (navController != null && bottomNav != null) {
            // 하단바와 내비게이션 연결
            NavigationUI.setupWithNavController(bottomNav, navController)

            // [추가] 목적지 변경 리스너: 특정 화면에서 하단바 숨기기
            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    // 1. 초기 진입 및 인증 화면
                    R.id.splashFragment,
                    R.id.loginFragment,
                    R.id.profileCreateFragment,

                        // 2. 팀 관련 화면 (team_nav 내부 프래그먼트들)
                    R.id.teamAddFragment,
                    R.id.teamCreateFragment,
                    R.id.teamJoinFragment,
                    R.id.teamCompleteFragment,
                    R.id.teamJoinedSuccessFragment -> {
                        // 하단바 숨김 (CardView가 있다면 CardView를 숨기는 게 더 깔끔합니다)
                        navCardView?.visibility = View.GONE
                    }
                    else -> {
                        // 그 외 화면(Home, Calendar 등)에서는 표시
                        navCardView?.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val tv = findViewById<TextView>(R.id.tvLog)
        if (intent == null || tv == null) return

        val screen = intent.getStringExtra("EXTRA_PUSH_SCREEN")
        val alarmId = intent.getStringExtra("EXTRA_PUSH_ALARM_ID")

        if (!screen.isNullOrBlank() || !alarmId.isNullOrBlank()) {
            val msg = "PushClick: screen=$screen alarmId=$alarmId"
            Log.d(TAG, msg)
            tv.text = msg
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}