package com.project.unimate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.project.unimate.auth.FcmRegistrar
import com.project.unimate.auth.JwtStore
import com.project.unimate.databinding.ActivityMainBinding
import com.project.unimate.network.Env

// 네비게이션바 로직을 위해 AppCompatActivity로 상속 변경
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "UnimateFCM"
    private val BASE_URL = Env.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 뷰 바인딩 초기화 및 레이아웃 설정
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // 네비게이션바 초기화 로직
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navView: BottomNavigationView = binding.bottomNavigation
        navView.itemIconTintList = null // 아이콘 원래 색상 유지
        navView.itemActiveIndicatorColor = ColorStateList.valueOf(Color.TRANSPARENT)
        navView.setupWithNavController(navController)
        applyBottomNavGap(navView, gapDp = 6)



        requestNotificationPermissionIfNeeded()
        handlePushIntent(intent)

        // ✅ 여기 " " 안에 Swagger에서 받은 JWT를 그대로 붙여넣기 (Bearer 붙이지 말 것)
        val TEST_JWT = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJzaDA4ODE0QG5hdmVyLmNvbSIsImlhdCI6MTc3MDY0OTkwOCwiZXhwIjoxNzcwNjUzNTA4fQ.BO_sDpWsG3DFF0Ra2fA6Q6fj1qgujvVbm11nW_7sLH9KCGlOwLsMk-vVFs6JrxxdGyYFnnatgPnXyMbdYugXLQ"

        if (TEST_JWT.isNotBlank()) {
            val token = TEST_JWT.trim().removePrefix("Bearer ").trim()
            JwtStore.save(this, token)
            val after = JwtStore.load(this)
            Log.d(TAG, "✅ TEST_JWT injected. afterLoad len=${after?.length ?: 0}, head=${after?.take(12)}")
        }

        val jwt = JwtStore.load(this)
        Log.d(TAG, "JWT exists? ${!jwt.isNullOrBlank()} len=${jwt?.length ?: 0}")

        FcmRegistrar.registerIfPossible(this, BASE_URL)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val tv = findViewById<TextView>(R.id.tvLog)
        if (intent == null) return

        val screen = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_SCREEN)
        val alarmId = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_ALARM_ID)

        if (!screen.isNullOrBlank() || !alarmId.isNullOrBlank()) {
            val msg = "PushClick: screen=$screen alarmId=$alarmId"
            Log.d(TAG, msg)
            tv.text = msg
        } else {
            tv.text = "Hello World!"
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




    // 네비게이션바 내부 간격 조정
    private fun applyBottomNavGap(navView: BottomNavigationView, gapDp: Int) {
        navView.post {
            val gapPx = (gapDp * resources.displayMetrics.density)

            val menuView = navView.getChildAt(0) as? ViewGroup ?: return@post
            for (i in 0 until menuView.childCount) {
                val item = menuView.getChildAt(i) as? ViewGroup ?: continue

                val icons = ArrayList<ImageView>()
                val labels = ArrayList<TextView>()
                collectNavChildren(item, icons, labels)

                // 아이콘&글자 사이 간격 증가
                labels.forEach { it.translationY = gapPx}
                icons.forEach { it.translationY = 0f }
                }
        }
    }

    private fun collectNavChildren(
        root: View,
        icons: MutableList<ImageView>,
        labels: MutableList<TextView>
    ) {
        when (root) {
            is ImageView -> icons.add(root)
            is TextView -> labels.add(root)
            is ViewGroup -> {
                for (i in 0 until root.childCount) {
                    collectNavChildren(root.getChildAt(i), icons, labels)
                }
            }
        }
    }

}

