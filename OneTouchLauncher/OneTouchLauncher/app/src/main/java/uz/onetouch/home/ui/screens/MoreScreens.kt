package uz.onetouch.home.ui.screens

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.onetouch.home.LauncherViewModel
import uz.onetouch.home.data.Cities
import uz.onetouch.home.data.SaverStyle
import uz.onetouch.home.data.Scenario
import uz.onetouch.home.data.ThemeMode
import uz.onetouch.home.ui.components.Caps
import uz.onetouch.home.ui.components.DesignSlider
import uz.onetouch.home.ui.components.DesignSwitch
import uz.onetouch.home.ui.components.Ic
import uz.onetouch.home.ui.components.LineIcon
import uz.onetouch.home.ui.components.OutlinePill
import uz.onetouch.home.ui.components.RoundIconButton
import uz.onetouch.home.ui.components.VoiceRings
import uz.onetouch.home.ui.components.at
import uz.onetouch.home.ui.components.tap
import uz.onetouch.home.ui.theme.LocalPalette
import uz.onetouch.home.ui.theme.T

// ───────────────────────────── Сценарии ─────────────────────────────

@Composable
fun ScenesScreen(vm: LauncherViewModel) {
    val p = LocalPalette.current
    var lastRun by remember { mutableIntStateOf(-1) }
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.at(64, 52), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundIconButton(Ic.chevronL) { vm.goHome() }
            T("Сценарии", 30.sp, weight = FontWeight.Light)
        }
        Column(Modifier.at(64, 124).width(1152.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            vm.home.scenarios.chunked(3).forEachIndexed { row, list ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    list.forEachIndexed { col, sc ->
                        val idx = row * 3 + col
                        ScenarioCard(sc, highlighted = idx == 0, done = lastRun == idx, modifier = Modifier.weight(1f)) {
                            lastRun = idx
                            vm.runScenario(sc.id)
                        }
                    }
                }
            }
        }
        T("Нажмите на карточку, чтобы запустить сценарий", 13.sp, Modifier.at(64, 740), color = p.muted)
    }
}

@Composable
private fun ScenarioCard(sc: Scenario, highlighted: Boolean, done: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val bg = if (highlighted) p.activeBg else p.surface
    val fg = if (highlighted) p.activeFg else p.ink
    val sub = if (highlighted) p.activeSub else p.muted
    val icon = when (sc.icon) {
        "moon" -> Ic.moon; "sun" -> Ic.sun; "exit" -> Ic.exit; "tv" -> Ic.tv; "users" -> Ic.users; else -> Ic.home
    }
    val iconColor = when (sc.icon) { "sun" -> p.warm; "exit" -> p.accentSoft; else -> fg }
    Column(
        modifier
            .height(286.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .then(if (done) Modifier.border(2.dp, p.accent, RoundedCornerShape(22.dp)) else Modifier)
            .tap(onClick = onClick)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            LineIcon(icon, iconColor, 24.dp)
            Box(Modifier.weight(1f))
            T(if (done) "Выполнено" else sc.trigger, 12.sp, color = if (done) p.accent else sub)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T(sc.title, 26.sp, color = fg, weight = FontWeight.Light)
            T(sc.description, 13.sp, color = sub, maxLines = 2)
        }
    }
}

// ───────────────────────────── Плеер ─────────────────────────────

@Composable
fun PlayerScreen(vm: LauncherViewModel) {
    val media by vm.media.state.collectAsStateWithLifecycle()
    val home by vm.home.state.collectAsStateWithLifecycle()
    val progress = rememberProgress(media)
    val ink = Color(0xFFF2EFEA)
    val muted = Color(0xFFB9AE9F)
    var volume by remember { mutableFloatStateOf(vm.media.volume()) }
    val ctx = LocalContext.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF2E261E), Color(0xFF100E0C))))
    ) {
        Row(Modifier.at(32, 22), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).tap { vm.goHome() }, contentAlignment = Alignment.Center) { LineIcon(Ic.chevronDown, ink, 18.dp) }
            Caps(media?.appLabel ?: "Музыка", color = muted, size = 12f, tracking = 0.3f)
        }
        // Пластинка
        Box(Modifier.at(96, 120).size(380.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(380.dp)) {
                drawCircle(Color(0xFF221C16))
                for (i in 3..9) drawCircle(Color.White.copy(alpha = 0.035f), size.minDimension / 2 * i / 10f, style = Stroke(1f))
            }
            val art = media?.art
            if (art != null) {
                Image(art.asImageBitmap(), null, Modifier.size(180.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                VoiceRings(200.dp, listening = media?.playing == true, level = 0f)
            }
        }
        Column(Modifier.at(96, 540).width(500.dp)) {
            T(media?.title ?: "Ничего не играет", 30.sp, color = ink, weight = FontWeight.Light, maxLines = 1)
            T(media?.artist ?: "Включите музыку в любом приложении", 15.sp, color = muted, maxLines = 1)
            Box(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                T(fmtMs(media?.position() ?: 0), 11.sp, color = muted)
                DesignSlider(progress, { f -> media?.let { vm.media.seekTo((it.durationMs * f).toLong()) } }, ink, Modifier.weight(1f))
                T(fmtMs(media?.durationMs ?: 0), 11.sp, color = muted)
            }
            Box(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).tap { vm.media.prev() }, contentAlignment = Alignment.Center) { LineIcon(Ic.prev, ink, 20.dp) }
                Box(Modifier.width(24.dp))
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(ink).tap { vm.media.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (media?.playing == true) LineIcon(Ic.pause, Color(0xFF100E0C), 22.dp)
                    else LineIcon(Ic.play, Color(0xFF100E0C), 22.dp, filled = true)
                }
                Box(Modifier.width(24.dp))
                Box(Modifier.size(56.dp).tap { vm.media.next() }, contentAlignment = Alignment.Center) { LineIcon(Ic.next, ink, 20.dp) }
            }
        }
        // Правая панель
        Column(
            Modifier
                .at(640, 40)
                .size(600.dp, 720.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF3A322A).copy(alpha = 0.55f))
                .padding(36.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Caps("Источник", color = muted, size = 12f, tracking = 0.3f)
            T(media?.appLabel ?: "Нет активного плеера", 34.sp, color = ink, weight = FontWeight.Light)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                media?.let { m ->
                    OutlinePill("Открыть приложение", height = 48.dp) {
                        ctx.packageManager.getLaunchIntentForPackage(m.packageName)?.let {
                            ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
                if (!vm.media.hasAccess()) {
                    OutlinePill("Разрешить управление музыкой", height = 48.dp, filledAccent = true) {
                        ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            Box(Modifier.weight(1f))
            Caps("Играет в комнатах", color = muted, size = 12f, tracking = 0.3f)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinePill(home.roomName, height = 44.dp, selected = true) {}
                OutlinePill("Планшет", height = 44.dp, selected = true) {}
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LineIcon(Ic.volume, ink, 20.dp)
                    T("Громкость ${(volume * 100).toInt()}%", 15.sp, color = ink)
                }
                DesignSlider(volume, { volume = it; vm.media.setVolume(it) }, ink, Modifier.fillMaxWidth())
            }
        }
    }
}

// ───────────────────────────── Настройки лаунчера ─────────────────────────────

@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val p = LocalPalette.current
    val s by vm.settings.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) } // перерисовать статусы разрешений
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        tick++; vm.refreshAgenda()
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.at(64, 52), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoundIconButton(Ic.chevronL) { vm.goHome() }
            T("Настройки OneTouch Home", 30.sp, weight = FontWeight.Light)
        }
        Row(Modifier.at(64, 130).size(1152.dp, 640.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
            // Левая колонка — внешний вид
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(30.dp)) {
                Choice("Тема", ThemeMode.entries.map { it.title }, s.theme.ordinal) { i ->
                    vm.settings.update { it.copy(theme = ThemeMode.entries[i]) }
                }
                Choice("Заставка", SaverStyle.entries.map { it.title }, s.saver.ordinal) { i ->
                    vm.settings.update { it.copy(saver = SaverStyle.entries[i]) }
                }
                val mins = listOf(1, 2, 5, 10, 0)
                Choice("Заставка через", mins.map { if (it == 0) "Никогда" else "$it мин" }, mins.indexOf(s.saverMinutes).coerceAtLeast(0)) { i ->
                    vm.settings.update { it.copy(saverMinutes = mins[i]) }
                }
                Choice("Город для погоды", Cities.map { it.name }, s.cityIndex, columns = 4) { i ->
                    vm.settings.update { it.copy(cityIndex = i) }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        T("Приглушать экран ночью", 17.sp, weight = FontWeight.Light)
                        T("На заставке «Ночь» яркость снижается до минимума", 13.sp, color = p.muted)
                    }
                    DesignSwitch(s.dimAtNight) { v -> vm.settings.update { it.copy(dimAtNight = v) } }
                }
                OutlinePill("Показать заставку сейчас", height = 48.dp) { vm.openSaver() }
            }
            // Правая колонка — разрешения
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Caps("Доступы", modifier = Modifier.padding(bottom = 10.dp))
                val isHome = remember(tick, vm.resumeCount) { isDefaultLauncher(ctx) }
                PermRow("Лаунчер по умолчанию", "Кнопка «Домой» будет открывать OneTouch", isHome) {
                    val rm = ctx.getSystemService(RoleManager::class.java)
                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    } else {
                        ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                PermRow("Календарь", "События дня на главном экране", remember(tick, vm.resumeCount) { granted(ctx, Manifest.permission.READ_CALENDAR) }) {
                    permLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
                }
                PermRow("Микрофон", "Голосовые команды", remember(tick, vm.resumeCount) { granted(ctx, Manifest.permission.RECORD_AUDIO) }) {
                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
                PermRow("Управление музыкой", "Доступ к уведомлениям — чтобы видеть и переключать треки", remember(tick, vm.resumeCount) { vm.media.hasAccess() }) {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                PermRow("Системные настройки", "Wi-Fi, Bluetooth, экран, звук", null) {
                    ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                Box(Modifier.height(24.dp))
                T(
                    "Умный дом работает в демо-режиме: плитки и сценарии меняют состояние на экране, " +
                        "но не управляют реальными устройствами. Подключение — через интерфейс HomeBackend.",
                    13.sp, color = p.muted, lineHeight = 1.4f
                )
                Box(Modifier.height(10.dp))
                T("Версия 1.0.0 · долгое нажатие на логотип открывает этот экран", 12.sp, color = p.muted)
            }
        }
    }
}

private fun granted(ctx: Context, perm: String) = ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED

private fun isDefaultLauncher(ctx: Context): Boolean {
    val rm = ctx.getSystemService(RoleManager::class.java)
    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) return rm.isRoleHeld(RoleManager.ROLE_HOME)
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return ctx.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == ctx.packageName
}

@Composable
private fun Choice(title: String, options: List<String>, selected: Int, columns: Int = 5, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Caps(title)
        options.chunked(columns).forEachIndexed { r, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chunk.forEachIndexed { c, o ->
                    val i = r * columns + c
                    OutlinePill(o, Modifier.weight(1f), height = 44.dp, fontSize = 14f, selected = i == selected) { onSelect(i) }
                }
                repeat(columns - chunk.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PermRow(title: String, subtitle: String, ok: Boolean?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column {
        Row(
            Modifier.fillMaxWidth().height(72.dp).tap(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                T(title, 17.sp, weight = FontWeight.Light)
                T(subtitle, 13.sp, color = p.muted, maxLines = 1)
            }
            when (ok) {
                true -> T("Готово", 14.sp, color = p.accent)
                false -> T("Разрешить", 14.sp, color = p.warm, weight = FontWeight.Medium)
                null -> LineIcon(Ic.chevronR, p.muted, 16.dp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
    }
}
