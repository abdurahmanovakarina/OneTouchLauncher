package uz.onetouch.home.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import uz.onetouch.home.LauncherViewModel
import uz.onetouch.home.Screen
import uz.onetouch.home.data.AgendaRepo
import uz.onetouch.home.data.AppEntry
import uz.onetouch.home.data.MediaInfo
import uz.onetouch.home.ui.components.Avatars
import uz.onetouch.home.ui.components.Caps
import uz.onetouch.home.ui.components.DeviceTile
import uz.onetouch.home.ui.components.DoorIllustration
import uz.onetouch.home.ui.components.Header
import uz.onetouch.home.ui.components.Ic
import uz.onetouch.home.ui.components.LineIcon
import uz.onetouch.home.ui.components.Orbits
import uz.onetouch.home.ui.components.OutlinePill
import uz.onetouch.home.ui.components.RoundIconButton
import uz.onetouch.home.ui.components.TransportControls
import uz.onetouch.home.ui.components.VoicePill
import uz.onetouch.home.ui.components.at
import uz.onetouch.home.ui.components.tap
import uz.onetouch.home.ui.theme.LocalFont
import uz.onetouch.home.ui.theme.LocalPalette
import uz.onetouch.home.ui.theme.T
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU = Locale.forLanguageTag("ru")
fun timeText(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("HH:mm"))
fun dateText(t: LocalDateTime): String =
    t.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", RU)).replaceFirstChar { it.titlecase(RU) }

/** Прогресс трека, обновляется раз в секунду, пока играет. */
@Composable
fun rememberProgress(m: MediaInfo?): Float {
    val v by produceState(0f, m) {
        while (true) {
            value = if (m != null && m.durationMs > 0) m.position().toFloat() / m.durationMs else 0f
            if (m?.playing != true) break
            delay(1000)
        }
    }
    return v.coerceIn(0f, 1f)
}

/** Три страницы главного экрана: обзор, минимал, приложения. */
@Composable
fun HomePager(vm: LauncherViewModel) {
    val pager = rememberPagerState(initialPage = vm.page, pageCount = { 3 })
    LaunchedEffect(vm.page) { if (pager.currentPage != vm.page) pager.animateScrollToPage(vm.page) }
    LaunchedEffect(pager.currentPage) { vm.page = pager.currentPage }
    val settings by vm.settings.state.collectAsStateWithLifecycle()
    val p = LocalPalette.current

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { i ->
            Box(Modifier.fillMaxSize()) {
                when (i) {
                    0 -> OverviewPage(vm)
                    1 -> MinimalPage(vm)
                    else -> AppsPage(vm)
                }
            }
        }
        Header(
            onLogoLongPress = { vm.open(Screen.SETTINGS) },
            page = pager.currentPage,
            pages = 3,
            onPage = { vm.page = it },
        ) {
            if (pager.currentPage == 0) {
                Avatars(settings.residents)
                Box(Modifier.width(22.dp))
                VoicePill { vm.startVoice() }
            } else {
                Row(
                    Modifier.tap { vm.startVoice() }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LineIcon(Ic.mic, p.muted, 14.dp)
                    T("Скажите «OneTouch»", 14.sp, color = p.muted, weight = FontWeight.Light)
                }
            }
        }
    }
}

// ───────────────────────────── Страница 1 · Обзор ─────────────────────────────

@Composable
private fun OverviewPage(vm: LauncherViewModel) {
    val p = LocalPalette.current
    val now by vm.now.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    val media by vm.media.state.collectAsStateWithLifecycle()
    val home by vm.home.state.collectAsStateWithLifecycle()
    val settings by vm.settings.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        // Часы, дата, погода
        Column(Modifier.at(58, 124).width(520.dp)) {
            T(
                timeText(now), 152.sp, Modifier.tap { vm.openSaver() },
                weight = FontWeight.ExtraLight, tracking = -0.045f, lineHeight = 0.9f
            )
            Row(Modifier.padding(start = 6.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                T(dateText(now), 19.sp, color = p.ink2, weight = FontWeight.Light)
                val w = weather
                T(
                    if (w != null) "${settings.city.name} · ${w.temp}° · ${w.condition}" else settings.city.name,
                    19.sp, color = p.muted, weight = FontWeight.Light, maxLines = 1
                )
            }
            WeatherStrip(vm, Modifier.padding(start = 6.dp, top = 30.dp))
            agenda?.next?.let { ev ->
                OutlinePill(
                    AgendaRepo.chipText(ev),
                    Modifier.padding(start = 6.dp, top = 26.dp),
                    height = 52.dp, fontSize = 16f,
                ) { vm.refreshAgenda() }
            }
        }

        // Плитки
        Row(Modifier.at(64, 620), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DeviceTile(Ic.bulb, home.roomName, if (home.lightOn) "Свет ${home.brightness}%" else "Свет выключен", home.lightOn,
                onLongClick = { vm.open(Screen.ROOM) }) { vm.open(Screen.ROOM) }
            DeviceTile(Ic.thermo, "Климат", "${home.climateTemp}° · ${home.humidity}%", false) { vm.open(Screen.ROOM) }
            DeviceTile(Ic.curtains, "Шторы", if (home.curtainsClosed) "Закрыты" else "Открыты", false) {
                vm.home.update { it.copy(curtainsClosed = !it.curtainsClosed) }
            }
            DeviceTile(Ic.shield, "Охрана", if (home.securityOn) "Включена" else "Выключена", false) {
                vm.home.update { it.copy(securityOn = !it.securityOn) }
            }
        }

        // Круглый плеер
        MusicDisc(vm, media, Modifier.at(540, 104), size = 500.dp, discRadius = 190f, roomLabel = home.roomName)

        // Сценарии
        val night = now.hour >= 18 || now.hour < 5
        Column(Modifier.at(610, 622).width(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (night) OutlinePill("Спокойной ночи", Modifier.weight(1f), Ic.moon, p.warm) { vm.runScenario("night") }
                else OutlinePill("Доброе утро", Modifier.weight(1f), Ic.sun, p.warm) { vm.runScenario("morning") }
                OutlinePill("Ухожу из дома", Modifier.weight(1f), Ic.exit, p.accentSoft) { vm.runScenario("leave") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (night) OutlinePill("Кино на ТВ", Modifier.weight(1f), Ic.tv) { vm.runScenario("cinema") }
                else OutlinePill("Уборка дома", Modifier.weight(1f), Ic.home) { vm.runScenario("cleaning") }
                OutlinePill("Все сценарии", Modifier.weight(1f), Ic.grid) { vm.open(Screen.SCENES) }
            }
        }

        // План на день
        Column(Modifier.at(1030, 124).width(186.dp)) {
            Caps(agenda?.heading ?: "Сегодня", size = 12f, tracking = 0.3f, modifier = Modifier.padding(bottom = 14.dp))
            val items = agenda?.items.orEmpty()
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
                T(
                    if (agenda?.calendarAllowed == false) "Разрешите доступ к календарю в настройках лаунчера"
                    else "Событий нет",
                    15.sp, Modifier.padding(top = 12.dp).tap { vm.open(Screen.SETTINGS) },
                    color = p.muted, weight = FontWeight.Light
                )
            }
            items.forEachIndexed { i, it ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(Modifier)
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
                    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        T(it.time, 14.sp, color = p.accent)
                        T(it.title, 16.sp, weight = FontWeight.Light, maxLines = 2)
                    }
                    if (i == items.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
                }
            }
        }

        // Камера у двери
        Box(
            Modifier
                .at(1030, 544)
                .size(186.dp, 200.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(p.surface)
        ) {
            DoorIllustration(Modifier.fillMaxSize())
            Row(
                Modifier.at(14, 12),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(p.muted))
                T("Дверь · камера", 12.sp)
            }
            T("Не подключена", 13.sp, Modifier.align(Alignment.BottomStart).padding(14.dp), color = p.ink2)
        }
    }
}

@Composable
private fun WeatherStrip(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val weather by vm.weather.collectAsStateWithLifecycle()
    Column(modifier.width(514.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
        Row(Modifier.fillMaxWidth().height(84.dp).padding(vertical = 18.dp)) {
            val slots = weather?.slots.orEmpty()
            if (slots.isEmpty()) {
                T("Загружаю прогноз…", 15.sp, color = p.muted, weight = FontWeight.Light)
            }
            slots.take(5).forEach { s ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    T(s.label, 13.sp, color = p.muted)
                    T("${s.temp}°", 24.sp, color = if (s.highlight) p.warm else p.ink, weight = FontWeight.Light)
                }
            }
            repeat((5 - slots.size).coerceIn(0, 5).let { if (slots.isEmpty()) 0 else it }) { Box(Modifier.weight(1f)) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
    }
}

/**
 * Круглый плеер с орбитами. Управляет тем, что сейчас играет на планшете
 * (через MediaSession), а без активной музыки — возобновляет последний плеер.
 */
@Composable
fun MusicDisc(
    vm: LauncherViewModel,
    media: MediaInfo?,
    modifier: Modifier,
    size: Dp,
    discRadius: Float,
    roomLabel: String,
    minimal: Boolean = false,
) {
    val p = LocalPalette.current
    val progress = rememberProgress(media)
    val half = size.value / 2f
    Box(modifier.size(size)) {
        Orbits(
            size, p.accentSoft, if (p.isDark) p.ink else p.ink,
            alphaA = if (p.isDark) 0.28f else 0.35f, alphaB = if (p.isDark) 0.2f else 0.16f,
        )
        Canvas(Modifier.size(size)) {
            val k = this.size.width / size.value
            val r = discRadius * k
            val c = Offset(this.size.width / 2, this.size.height / 2)
            if (!minimal) drawCircle(p.disc, r, c)
            drawCircle(p.track, r, c, style = Stroke(1.2f))
            if (progress > 0f) {
                drawArc(
                    p.accent, -90f, 360f * progress, false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
                    style = Stroke(2f * k, cap = StrokeCap.Round)
                )
            }
        }
        val box = (discRadius * 1.58f).dp
        Column(
            Modifier
                .offset(x = (half.dp - box / 2), y = (half.dp - box / 2))
                .size(box),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Caps(if (minimal) "Сейчас играет" else roomLabel, size = 12f, tracking = 0.3f)
            T(
                media?.title ?: "Музыка", if (minimal) 36.sp else 30.sp,
                Modifier.tap { vm.open(Screen.PLAYER) },
                weight = FontWeight.Light, tracking = -0.01f, lineHeight = 1.15f,
                align = TextAlign.Center, maxLines = 2
            )
            T(
                when {
                    media == null -> "Ничего не играет"
                    media.artist.isNotBlank() -> "${media.artist} · ${media.appLabel}"
                    else -> media.appLabel
                },
                15.sp, color = p.muted, weight = FontWeight.Light, align = TextAlign.Center, maxLines = 1
            )
            Box(Modifier.height(12.dp))
            TransportControls(
                playing = media?.playing == true,
                onPrev = { vm.media.prev() }, onToggle = { vm.media.toggle() }, onNext = { vm.media.next() },
                big = if (minimal) 72.dp else 64.dp,
            )
        }
    }
}

// ───────────────────────────── Страница 2 · Минимал ─────────────────────────────

@Composable
private fun MinimalPage(vm: LauncherViewModel) {
    val p = LocalPalette.current
    val now by vm.now.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    val media by vm.media.state.collectAsStateWithLifecycle()
    val home by vm.home.state.collectAsStateWithLifecycle()
    val settings by vm.settings.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.at(60, 150).width(560.dp)) {
            T(timeText(now), 196.sp, Modifier.tap { vm.openSaver() }, weight = FontWeight.ExtraLight, tracking = -0.045f, lineHeight = 0.9f)
            Row(Modifier.padding(start = 6.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                T(dateText(now), 19.sp, color = p.ink2, weight = FontWeight.Light)
                weather?.let { T("${settings.city.name} · ${it.temp}° · ${it.condition}", 19.sp, color = p.muted, weight = FontWeight.Light) }
            }
            agenda?.next?.let { ev ->
                OutlinePill(AgendaRepo.chipText(ev), Modifier.padding(start = 6.dp, top = 56.dp), height = 52.dp, fontSize = 16f) {}
            }
        }
        Row(Modifier.at(64, 612), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DeviceTile(Ic.bulb, home.roomName, if (home.lightOn) "${home.brightness}%" else "Выкл.", home.lightOn, 128.dp, 132.dp) { vm.open(Screen.ROOM) }
            DeviceTile(Ic.thermo, "Климат", "${home.climateTemp}°", false, 128.dp, 132.dp) { vm.open(Screen.ROOM) }
            DeviceTile(Ic.curtains, "Шторы", if (home.curtainsClosed) "Закрыты" else "Открыты", false, 128.dp, 132.dp) {
                vm.home.update { it.copy(curtainsClosed = !it.curtainsClosed) }
            }
            DeviceTile(Ic.shield, "Охрана", if (home.securityOn) "Включена" else "Выключена", false, 128.dp, 132.dp) {
                vm.home.update { it.copy(securityOn = !it.securityOn) }
            }
        }
        MusicDisc(vm, media, Modifier.at(590, 65), size = 700.dp, discRadius = 300f, roomLabel = home.roomName, minimal = true)
    }
}

// ───────────────────────────── Страница 3 · Приложения ─────────────────────────────

@Composable
private fun AppsPage(vm: LauncherViewModel) {
    val p = LocalPalette.current
    val apps by vm.apps.apps.collectAsStateWithLifecycle()
    val settings by vm.settings.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query, settings.hidden) {
        apps.filter { it.packageName !in settings.hidden && (query.isBlank() || it.label.contains(query.trim(), ignoreCase = true)) }
    }

    Box(Modifier.fillMaxSize()) {
        T("Приложения", 30.sp, Modifier.at(64, 118), weight = FontWeight.Light)
        Row(Modifier.at(796, 118), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier
                    .width(364.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(p.surface)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LineIcon(Ic.search, p.muted, 16.dp)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) T("Поиск приложения", 14.sp, color = p.muted)
                    BasicTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = TextStyle(color = p.ink, fontSize = 14.sp, fontFamily = LocalFont.current),
                        cursorBrush = SolidColor(p.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) Box(Modifier.tap { query = "" }) { LineIcon(Ic.close, p.muted, 14.dp) }
            }
            RoundIconButton(Ic.settings) { vm.open(Screen.SETTINGS) }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.at(64, 186).size(1152.dp, 614.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(shown, key = { it.component.flattenToString() }) { app ->
                AppTile(app, onClick = { vm.apps.launch(app) }, onLong = { vm.apps.openInfo(app) })
            }
        }
        if (shown.isEmpty()) {
            T(if (apps.isEmpty()) "Загружаю приложения…" else "Ничего не найдено", 16.sp, Modifier.at(64, 200), color = p.muted)
        }
    }
}

@Composable
private fun AppTile(app: AppEntry, onClick: () -> Unit, onLong: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier
            .height(124.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(p.surface)
            .then(if (p.isDark) Modifier else Modifier.border(1.dp, p.line, RoundedCornerShape(22.dp)))
            .tap(onLongClick = onLong, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Image(app.icon, contentDescription = app.label, modifier = Modifier.size(44.dp), contentScale = ContentScale.Fit)
        T(app.label, 14.sp, maxLines = 2, lineHeight = 1.15f)
    }
}
