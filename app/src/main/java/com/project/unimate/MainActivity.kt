package com.project.unimate

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.launch
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.project.unimate.auth.FcmRegistrar
import com.project.unimate.auth.JwtStore
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.data.repository.PendingCompletionPopupStore
import com.project.unimate.data.repository.NicknameStore
import com.project.unimate.data.repository.ProfileImageStore
import com.project.unimate.data.repository.ServerSync
import com.project.unimate.data.repository.SyncManager
import com.project.unimate.databinding.ActivityMainBinding
import com.project.unimate.network.Env
import com.project.unimate.ui.timepick.TimepickStateHolder

// 네비게이션바 로직을 위해 AppCompatActivity로 상속 변경
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "UnimateFCM"
    private val BASE_URL = Env.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전체 화면 설정 (Edge-to-Edge)
        enableEdgeToEdge()

        // 1. 레이아웃 설정 (뷰 바인딩 방식으로 수정)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        Log.d(TAG, "JWT exists? ${!jwt.isNullOrBlank()} len=${jwt?.length ?: 0}")
        FcmRegistrar.registerIfPossible(this, BASE_URL)

        // 저장된 유저 프로필 이미지 경로 복원 (서버 sync에서 덮어쓸 수 있음)
        ProfileImageStore.get(this).takeIf { it.isNotBlank() }?.let {
            DummyRepository.setCurrentUserProfileImageResName(it)
        }

        if (!jwt.isNullOrBlank()) {
            // 이미 로그인된 상태: 닉네임 + 로컬 캐시 복원 후 서버 동기화
            NicknameStore.get(this).takeIf { it.isNotBlank() }?.let {
                DummyRepository.setCurrentUserName(it)
            }
            DummyRepository.loadSchedulesFrom(this)
            // 앱 시작(cold start) 시 서버에서 최신 팀·일정 동기화. SplashFragment 동기화와
            // 직렬화(syncMutex)되므로 중복 실행 안전.
            lifecycleScope.launch {
                val ok = SyncManager.syncAllDataFromServer(applicationContext)
                Log.d(TAG, "앱 시작 동기화 결과: $ok")
            }
        } else {
            DummyRepository.loadSchedulesFrom(this)
            Log.d(TAG, "토큰 없음 → 동기화 스킵")
        }
    }

    /** 스플래시를 한 번이라도 떠났으면 true (onResume에서 서버 sync 할지 판단용) */
    private var hasLeftSplashScreen = false

    override fun onResume() {
        super.onResume()
        checkAndShowTeamEndPopups()
        // 앱 백그라운드 복귀 시에만 서버 동기화. cold start 시에는 Splash에서만 sync 하므로 여기서는 스킵
        if (JwtStore.load(this).isNullOrBlank()) return
        if (!hasLeftSplashScreen) return
        lifecycleScope.launch { ServerSync.syncFromServer(this@MainActivity) }
    }

    private val teamEndPopupPrefs: SharedPreferences
        get() = getSharedPreferences("team_end_popup", MODE_PRIVATE)

    private fun hasShownTeamEndPopup(teamId: String): Boolean =
        teamEndPopupPrefs.getBoolean("shown_$teamId", false)

    private fun markTeamEndPopupShown(teamId: String) {
        teamEndPopupPrefs.edit().putBoolean("shown_$teamId", true).apply()
    }

    private fun checkAndShowTeamEndPopups() {
        val pendingIds = PendingCompletionPopupStore.getPendingIds(this)
        if (pendingIds.isEmpty()) return
        val endedTeams = pendingIds.mapNotNull { id -> DummyRepository.getTeamById(id) }.toMutableList()
        if (endedTeams.isEmpty()) return
        showNextTeamEndPopup(endedTeams)
    }

    private fun showNextTeamEndPopup(queue: MutableList<Team>) {
        if (queue.isEmpty()) return
        val team = queue.removeAt(0)
        val view = layoutInflater.inflate(R.layout.dialog_team_space_ended, null)
        val dialog = AlertDialog.Builder(this, R.style.TeamEndDialogTheme)
            .setView(view)
            .setCancelable(false)
            .create()
        view.findViewById<View>(R.id.dialogTeamEndConfirm).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            PendingCompletionPopupStore.remove(this, team.id)
            markTeamEndPopupShown(team.id)
            if (queue.isNotEmpty()) showNextTeamEndPopup(queue)
        }
        val dm = resources.displayMetrics
        val wPx = (dm.widthPixels * 0.9f).toInt()
        val hPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260f, dm).toInt()
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes?.let { params ->
                params.width = wPx
                params.height = hPx
                window.attributes = params
            }
            window.setDimAmount(0.6f)
        }
        dialog.show()
        dialog.window?.let { window ->
            window.setLayout(wPx, hPx)
            view.post { window.setLayout(wPx, hPx) }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navView: BottomNavigationView = binding.bottomNavigation

        // 디자인 디테일 적용
        navView.itemIconTintList = null // 아이콘 원래 색상 유지
        navView.itemActiveIndicatorColor = ColorStateList.valueOf(Color.TRANSPARENT)

        // 네비게이션바 초기화 로직 (홈/캘린더/찌르기/마이페이지만 이동, 팀스페이스 등 서브 화면은 pop)
        navView.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)
                .setLaunchSingleTop(true)
                .build()
            navController.navigate(item.itemId, null, navOptions)
            true
        })

        // 네비게이션바 내부 간격 조정 호출
        applyBottomNavGap(navView, gapDp = 6)

        // 목적지 변경 리스너 (스플래시 이탈 여부 기록 → onResume에서 sync 1회만 하기 위함)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.splashFragment) hasLeftSplashScreen = true
            when (destination.id) {
                // 숨김 목록
                R.id.splashFragment, R.id.loginFragment, R.id.profileCreateFragment,
                R.id.teamAddFragment, R.id.teamCreateFragment, R.id.teamJoinFragment,
                R.id.teamCompleteFragment, R.id.teamJoinedSuccessFragment,
                R.id.editTeamSpaceFragment, R.id.joinTeamSpaceFragment,
                R.id.createTimepickFragment, R.id.selectTimeFragment, R.id.timepickStatusFragment,
                R.id.timepickResultFragment, R.id.editTimepickFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }

            when (destination.id) {
                R.id.homeFragment,
                R.id.calendarFragment,
                R.id.pokeFragment,
                R.id.myPageFragment -> {
                    navView.menu.findItem(destination.id)?.isChecked = true
                }
                R.id.notificationFragment -> {
                    // 홈 헤더 종 버튼에서 들어온 알림 화면은 하단탭을 홈 상태로 유지
                    navView.menu.findItem(R.id.homeFragment)?.isChecked = true
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
        if (intent == null) return

        val screen = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_SCREEN)
        val alarmId = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_ALARM_ID)
        val alarmType = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_ALARM_TYPE)
        val teamId = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_TEAM_ID)
        val messageTitle = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_MESSAGE_TITLE)
        val messageBody = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_MESSAGE_BODY)
        val hasPushPayload = !screen.isNullOrBlank() || !alarmId.isNullOrBlank() || !alarmType.isNullOrBlank()
        if (!hasPushPayload) return

        val msg = "PushClick: screen=$screen alarmId=$alarmId alarmType=$alarmType teamId=$teamId title=$messageTitle"
        Log.d(TAG, msg)
        tv?.text = msg

        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
            ?.navController ?: return

        val destinationId = when {
            isTimepickNotification(screen, alarmType, messageTitle, messageBody) -> {
                if (!teamId.isNullOrBlank()) {
                    TimepickStateHolder.teamId = teamId
                }
                R.id.editTimepickFragment
            }
            screen.orEmpty().lowercase().contains("calendar") -> R.id.calendarFragment
            screen.orEmpty().lowercase().contains("poke") -> R.id.pokeFragment
            screen.orEmpty().lowercase().contains("mypage") ||
                screen.orEmpty().lowercase().contains("my_page") -> R.id.myPageFragment
            screen.orEmpty().lowercase().contains("notification") ||
                screen.orEmpty().lowercase().contains("alarm") -> R.id.notificationFragment
            else -> R.id.homeFragment
        }

        try {
            val args = if (destinationId == R.id.editTimepickFragment) {
                Bundle().apply { putString("taskId", "") }
            } else {
                null
            }
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .build()
            navController.navigate(destinationId, args, navOptions)
        } catch (e: Exception) {
            Log.w(TAG, "Push navigation failed: ${e.message}")
        }

        intent.removeExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_SCREEN)
        intent.removeExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_ALARM_ID)
        intent.removeExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_ALARM_TYPE)
        intent.removeExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_TEAM_ID)
        intent.removeExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_MESSAGE_TITLE)
        intent.removeExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_MESSAGE_BODY)
    }

    private fun isTimepickNotification(
        screen: String?,
        alarmType: String?,
        messageTitle: String?,
        messageBody: String?
    ): Boolean {
        val s = screen.orEmpty().lowercase()
        val a = alarmType.orEmpty().lowercase()
        val t = messageTitle.orEmpty().lowercase()
        val b = messageBody.orEmpty().lowercase()
        return s.contains("timepick") ||
            s.contains("meeting") ||
            s.contains("edit") ||
            a.contains("meeting_request") ||
            a.contains("meeting") ||
            a.contains("timepick") ||
            a.contains("모임") ||
            a.contains("체크요청") ||
            t.contains("모임") ||
            t.contains("체크요청") ||
            b.contains("모임") ||
            b.contains("체크요청") ||
            b.contains("시간 입력")
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

    // UI 간격 조정 로직
    private fun applyBottomNavGap(navView: BottomNavigationView, gapDp: Int) {
        navView.post {
            val gapPx = (gapDp * resources.displayMetrics.density)
            val menuView = navView.getChildAt(0) as? ViewGroup ?: return@post
            for (i in 0 until menuView.childCount) {
                val item = menuView.getChildAt(i) as? ViewGroup ?: continue
                val icons = ArrayList<ImageView>()
                val labels = ArrayList<TextView>()
                collectNavChildren(item, icons, labels)
                labels.forEach { it.translationY = gapPx }
                icons.forEach { it.translationY = 0f }
            }
        }
    }

    private fun collectNavChildren(root: View, icons: MutableList<ImageView>, labels: MutableList<TextView>) {
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
