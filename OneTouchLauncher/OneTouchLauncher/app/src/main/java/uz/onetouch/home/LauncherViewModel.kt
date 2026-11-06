package uz.onetouch.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import uz.onetouch.home.data.Agenda
import uz.onetouch.home.data.AgendaRepo
import uz.onetouch.home.data.AppsRepo
import uz.onetouch.home.data.MediaRepo
import uz.onetouch.home.data.SaverStyle
import uz.onetouch.home.data.SettingsStore
import uz.onetouch.home.data.SmartHome
import uz.onetouch.home.data.ThemeMode
import uz.onetouch.home.data.VoiceController
import uz.onetouch.home.data.VoiceState
import uz.onetouch.home.data.Weather
import uz.onetouch.home.data.WeatherRepo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class Screen { HOME, ROOM, SCENES, PLAYER, SETTINGS, VOICE, SAVER }

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    val settings = SettingsStore(ctx)
    val home = SmartHome(ctx)
    val media = MediaRepo(ctx)
    val apps = AppsRepo(ctx, viewModelScope)
    val voice = VoiceController(ctx, ::handleCommand)

    private val _now = MutableStateFlow(LocalDateTime.now())
    val now: StateFlow<LocalDateTime> = _now.asStateFlow()

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    private val _agenda = MutableStateFlow<Agenda?>(null)
    val agenda: StateFlow<Agenda?> = _agenda.asStateFlow()

    var screen by mutableStateOf(Screen.HOME)
        private set
    var page by mutableIntStateOf(0)

    /** Растёт при каждом возврате на лаунчер — чтобы перепроверить разрешения. */
    var resumeCount by mutableIntStateOf(0)
        private set

    /** Запрос «открыть заставку» после голосовой команды или сценария «Спокойной ночи». */
    var forcedSaver by mutableStateOf<SaverStyle?>(null)
        private set

    private var lastTouch = System.currentTimeMillis()
    private var launchJob: Job? = null

    init {
        // Часы: тик ровно на смене минуты.
        viewModelScope.launch {
            while (true) {
                _now.value = LocalDateTime.now()
                val ms = System.currentTimeMillis()
                delay(60_000 - ms % 60_000 + 50)
            }
        }
        // Погода: при смене города и каждые 30 минут.
        viewModelScope.launch {
            settings.state.distinctUntilChangedBy { it.cityIndex }.collect { refreshWeather() }
        }
        viewModelScope.launch {
            while (true) {
                delay(30 * 60_000L)
                refreshWeather()
            }
        }
        // Календарь: каждые 5 минут.
        viewModelScope.launch {
            while (true) {
                refreshAgenda()
                delay(5 * 60_000L)
            }
        }
        // Заставка по бездействию.
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                val minutes = settings.state.value.saverMinutes
                val idle = System.currentTimeMillis() - lastTouch
                if (minutes > 0 && idle > minutes * 60_000L && screen != Screen.SAVER && screen != Screen.VOICE) {
                    open(Screen.SAVER)
                }
            }
        }
    }

    fun onResume() {
        resumeCount++
        media.start()
        refreshAgenda()
        if (_weather.value == null || System.currentTimeMillis() - (_weather.value?.updatedAt ?: 0) > 30 * 60_000L) {
            refreshWeather()
        }
        _now.value = LocalDateTime.now()
    }

    fun refreshWeather() {
        viewModelScope.launch { WeatherRepo.load(settings.state.value.city)?.let { _weather.value = it } }
    }

    fun refreshAgenda() {
        viewModelScope.launch { _agenda.value = AgendaRepo.load(ctx) }
    }

    fun touched() { lastTouch = System.currentTimeMillis() }

    fun open(s: Screen) {
        if (s != Screen.SAVER) forcedSaver = null
        if (s != Screen.VOICE) voice.cancel()
        screen = s
    }

    fun goHome(resetPage: Boolean = false) {
        open(Screen.HOME)
        if (resetPage) page = 0
    }

    fun openSaver(style: SaverStyle? = null) {
        forcedSaver = style
        screen = Screen.SAVER
    }

    /** true — нужно запросить разрешение на микрофон (обрабатывает MainActivity). */
    var needMic by mutableStateOf(false)

    fun startVoice() {
        val granted = ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) { needMic = true; return }
        screen = Screen.VOICE
        voice.start()
    }

    /** Тёмная тема: явно выбрана или «Авто» и сейчас вечер/ночь (20:00–07:00). */
    fun isDark(mode: ThemeMode, t: LocalDateTime): Boolean = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> t.hour >= 20 || t.hour < 7
    }

    fun saverStyle(): SaverStyle {
        val forced = forcedSaver
        if (forced != null) return forced
        val s = settings.state.value.saver
        if (s != SaverStyle.AUTO) return s
        val h = LocalDateTime.now().hour
        return when {
            h >= 23 || h < 6 -> SaverStyle.NIGHT
            media.state.value?.playing == true -> SaverStyle.MUSIC
            else -> SaverStyle.ORBITS
        }
    }

    fun runScenario(id: String) {
        home.runScenario(id)
        if (id == "night") openSaver(SaverStyle.NIGHT)
        if (id == "leave") media.pause()
    }

    // ───────────────────────── Голосовые команды ─────────────────────────

    private val openVerbs = listOf("открой", "запусти", "включи приложение", "покажи приложение")

    private fun handleCommand(raw: String): VoiceState {
        val t = raw.lowercase().replace("ё", "е").trim()
        fun said(title: String, sub: String) = VoiceState.Result(raw, title, sub)

        // 1. «Открой YouTube»
        openVerbs.firstOrNull { t.startsWith(it) }?.let { verb ->
            val name = t.removePrefix(verb).trim()
            val app = apps.find(name)
            return if (app != null) {
                launchJob?.cancel()
                launchJob = viewModelScope.launch {
                    delay(700)
                    apps.launch(app)
                    goHome()
                }
                said("Открываю ${app.label}", "Приложение")
            } else said("Не нашёл «$name»", "Проверьте название приложения на странице «Приложения»")
        }

        // 2. Сценарии
        val scenario = when {
            "спокойной ночи" in t || "отбой" in t -> "night"
            "ухожу" in t || "я ушел" in t -> "leave"
            "доброе утро" in t -> "morning"
            "кино" in t || "фильм" in t -> "cinema"
            "уборк" in t -> "cleaning"
            "гост" in t -> "guests"
            else -> null
        }
        if (scenario != null) {
            val sc = home.scenarios.first { it.id == scenario }
            return VoiceState.Result(raw, sc.title, sc.description, confirm = { runScenario(scenario) })
        }

        // 3. Свет
        Regex("""свет\D*(\d{1,3})""").find(t)?.let {
            val v = it.groupValues[1].toInt().coerceIn(0, 100)
            home.setBrightness(v)
            return said("Свет — $v%", home.state.value.roomName)
        }
        if (("выключи" in t || "погаси" in t) && "свет" in t) {
            home.setBrightness(0); return said("Свет выключен", home.state.value.roomName)
        }
        if ("включи" in t && "свет" in t) {
            home.setBrightness(70); return said("Свет включён", "${home.state.value.roomName} · 70%")
        }
        if ("штор" in t) {
            val close = "закр" in t
            home.update { it.copy(curtainsClosed = close) }
            return said(if (close) "Шторы закрыты" else "Шторы открыты", home.state.value.roomName)
        }
        if ("охран" in t) {
            val on = !("выключ" in t || "сними" in t)
            home.update { it.copy(securityOn = on) }
            return said(if (on) "Охрана включена" else "Охрана выключена", "Все двери и окна")
        }

        // 4. Музыка и громкость
        when {
            "пауза" in t || "стоп" in t || "останови" in t -> { media.pause(); return said("Пауза", media.state.value?.title ?: "Музыка") }
            "следующ" in t || "дальше" in t || "переключи" in t -> { media.next(); return said("Следующий трек", "") }
            "предыдущ" in t -> { media.prev(); return said("Предыдущий трек", "") }
            "громче" in t -> { media.setVolume(media.volume() + 0.15f); return said("Громче", "") }
            "тише" in t -> { media.setVolume(media.volume() - 0.15f); return said("Тише", "") }
            "музык" in t || "играй" in t || "продолжи" in t -> { media.play(); return said("Включаю музыку", media.state.value?.appLabel ?: "") }
        }

        // 5. Справка
        if ("погод" in t || "градус" in t) {
            val w = _weather.value
            return if (w != null) said("${settings.state.value.city.name}: ${w.temp}°", w.condition)
            else said("Погода пока не загрузилась", "Проверьте подключение к интернету")
        }
        if ("который час" in t || "сколько времени" in t || t == "время") {
            return said(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), "")
        }
        if ("заставк" in t || "часы" in t) {
            viewModelScope.launch { delay(900); openSaver() }
            return said("Включаю заставку", "")
        }

        return said(
            "Не понял команду",
            "Например: «включи свет на 50», «открой YouTube», «спокойной ночи», «погода»"
        )
    }

    override fun onCleared() {
        voice.cancel()
        super.onCleared()
    }
}
