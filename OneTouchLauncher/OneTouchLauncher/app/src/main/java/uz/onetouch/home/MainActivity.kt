package uz.onetouch.home

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.onetouch.home.data.SaverStyle
import uz.onetouch.home.ui.screens.HomePager
import uz.onetouch.home.ui.screens.PlayerScreen
import uz.onetouch.home.ui.screens.RoomScreen
import uz.onetouch.home.ui.screens.SaverScreen
import uz.onetouch.home.ui.screens.ScenesScreen
import uz.onetouch.home.ui.screens.SettingsScreen
import uz.onetouch.home.ui.screens.VoiceScreen
import uz.onetouch.home.ui.screens.saverBackground
import uz.onetouch.home.ui.theme.AppFonts
import uz.onetouch.home.ui.theme.DarkPalette
import uz.onetouch.home.ui.theme.DesignFrame
import uz.onetouch.home.ui.theme.LightPalette
import uz.onetouch.home.ui.theme.LocalFont
import uz.onetouch.home.ui.theme.LocalPalette

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val font = AppFonts.load(this)

        setContent {
            val settings by vm.settings.state.collectAsStateWithLifecycle()
            val now by vm.now.collectAsStateWithLifecycle()
            val dark = vm.isDark(settings.theme, now)
            val screen = vm.screen
            val saverStyle = if (screen == Screen.SAVER) vm.saverStyle() else null

            // Палитра: заставки «Свет», «Музыка», «Ночь» и плеер всегда тёмные.
            val palette = when {
                screen == Screen.PLAYER -> DarkPalette
                saverStyle != null && saverStyle != SaverStyle.ORBITS -> DarkPalette
                dark -> DarkPalette
                else -> LightPalette
            }
            val bg = if (saverStyle != null) saverBackground(saverStyle, palette.isDark) else palette.bg

            // Разрешения при первом запуске: календарь и микрофон.
            val firstRun = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                vm.refreshAgenda()
            }
            LaunchedEffect(Unit) {
                val sp = getSharedPreferences("launcher", MODE_PRIVATE)
                if (!sp.getBoolean("askedPerms", false)) {
                    sp.edit().putBoolean("askedPerms", true).apply()
                    firstRun.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.RECORD_AUDIO))
                }
            }
            val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                if (ok) vm.startVoice()
            }
            LaunchedEffect(vm.needMic) {
                if (vm.needMic) {
                    vm.needMic = false
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            // Ночью на заставке «Ночь» приглушаем подсветку.
            LaunchedEffect(saverStyle, settings.dimAtNight) {
                val lp = window.attributes
                lp.screenBrightness = if (saverStyle == SaverStyle.NIGHT && settings.dimAtNight) 0.01f
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }

            BackHandler(enabled = true) {
                if (vm.screen != Screen.HOME) {
                    vm.goHome()
                } else if (vm.page != 0) {
                    vm.page = 0
                }
            }

            CompositionLocalProvider(LocalPalette provides palette, LocalFont provides remember { font }) {
                DesignFrame(bg) {
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(200)) },
                        label = "screen"
                    ) { s ->
                        when (s) {
                            Screen.HOME -> HomePager(vm)
                            Screen.ROOM -> RoomScreen(vm)
                            Screen.SCENES -> ScenesScreen(vm)
                            Screen.PLAYER -> PlayerScreen(vm)
                            Screen.SETTINGS -> SettingsScreen(vm)
                            Screen.VOICE -> VoiceScreen(vm)
                            Screen.SAVER -> SaverScreen(vm, saverStyle ?: SaverStyle.ORBITS)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        vm.onResume()
    }

    /** Нажата кнопка «Домой», когда лаунчер уже открыт. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome(resetPage = vm.screen == Screen.HOME)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        vm.touched()
        return super.dispatchTouchEvent(ev)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
