package uz.onetouch.home.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import uz.onetouch.home.LauncherViewModel
import uz.onetouch.home.data.VoiceState
import uz.onetouch.home.ui.components.Caps
import uz.onetouch.home.ui.components.DesignSlider
import uz.onetouch.home.ui.components.DesignSwitch
import uz.onetouch.home.ui.components.Ic
import uz.onetouch.home.ui.components.OutlinePill
import uz.onetouch.home.ui.components.RoundIconButton
import uz.onetouch.home.ui.components.VoiceRings
import uz.onetouch.home.ui.components.at
import uz.onetouch.home.ui.components.tap
import uz.onetouch.home.ui.theme.LocalPalette
import uz.onetouch.home.ui.theme.T
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ───────────────────────────── Голосовой ассистент ─────────────────────────────

@Composable
fun VoiceScreen(vm: LauncherViewModel) {
    val p = LocalPalette.current
    val st by vm.voice.state.collectAsStateWithLifecycle()

    // Ответ без подтверждения закрывается сам через 5 секунд.
    LaunchedEffect(st) {
        val s = st
        if (s is VoiceState.Result && s.confirm == null) { delay(5000); vm.goHome() }
        if (s is VoiceState.Error) { delay(7000); vm.goHome() }
    }

    Box(Modifier.fillMaxSize().tap { vm.goHome() }) {
        Box(Modifier.at(540, 120).size(200.dp).tap { vm.voice.start() }, contentAlignment = Alignment.Center) {
            VoiceRings(200.dp, listening = st is VoiceState.Listening, level = (st as? VoiceState.Listening)?.level ?: 0f)
        }
        Column(
            Modifier.at(140, 360).width(1000.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (val s = st) {
                is VoiceState.Listening -> {
                    Caps("Слушаю", size = 12f, tracking = 0.3f)
                    T(
                        if (s.partial.isBlank()) "Говорите…" else "«${s.partial}»",
                        40.sp, weight = FontWeight.Light, align = TextAlign.Center, maxLines = 2
                    )
                }
                is VoiceState.Result -> {
                    Caps("Вы сказали", size = 12f, tracking = 0.3f)
                    T("«${s.heard}»", 40.sp, weight = FontWeight.Light, align = TextAlign.Center, maxLines = 2)
                    Box(Modifier.height(16.dp))
                    ResultCard(s, onConfirm = { s.confirm?.invoke(); vm.goHome() }, onCancel = { vm.goHome() })
                    Box(Modifier.height(8.dp))
                    T(
                        if (s.confirm != null) "Коснитесь «Подтвердить» или кольца, чтобы сказать ещё раз"
                        else "Коснитесь кольца, чтобы сказать ещё раз",
                        13.sp, color = p.muted
                    )
                }
                is VoiceState.Error -> {
                    Caps("OneTouch", size = 12f, tracking = 0.3f, color = p.accent)
                    T(s.message, 30.sp, weight = FontWeight.Light, align = TextAlign.Center)
                    Box(Modifier.height(8.dp))
                    OutlinePill("Ещё раз", height = 48.dp) { vm.voice.start() }
                }
                VoiceState.Idle -> {
                    Caps("Скажите «OneTouch»", size = 12f, tracking = 0.3f)
                    T("Коснитесь кольца и говорите", 34.sp, weight = FontWeight.Light)
                }
            }
        }
    }
}

@Composable
private fun ResultCard(s: VoiceState.Result, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .width(720.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(p.surface)
            .tap {}
            .padding(horizontal = 28.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Caps("OneTouch", size = 11f, tracking = 0.3f, color = p.accent)
            T(s.title, 24.sp, weight = FontWeight.Normal, maxLines = 2)
            if (s.subtitle.isNotBlank()) T(s.subtitle, 15.sp, color = p.muted, maxLines = 2)
        }
        if (s.confirm != null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinePill("Подтвердить", Modifier.width(170.dp), height = 48.dp, selected = true, onClick = onConfirm)
                OutlinePill("Отмена", Modifier.width(170.dp), height = 48.dp, onClick = onCancel)
            }
        }
    }
}

// ───────────────────────────── Комната ─────────────────────────────

@Composable
fun RoomScreen(vm: LauncherViewModel) {
    val p = LocalPalette.current
    val home by vm.home.state.collectAsStateWithLifecycle()
    var live by remember { mutableFloatStateOf(-1f) }
    val value = if (live >= 0f) live else home.brightness / 100f

    Box(Modifier.fillMaxSize()) {
        Row(Modifier.at(64, 52), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RoundIconButton(Ic.chevronL) { vm.goHome() }
            Caps("${home.roomName} · ${home.climateTemp}° · ${home.humidity}% влажность", tracking = 0.32f)
        }

        // Кольцо яркости — тянется пальцем по кругу
        val ringR = 230f
        Box(
            Modifier
                .at(80, 170)
                .size(520.dp)
                .pointerInput(Unit) {
                    fun angleToValue(o: Offset): Float {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        var a = atan2(o.x - c.x, -(o.y - c.y)) // 0 — сверху, по часовой
                        if (a < 0) a += (2 * PI).toFloat()
                        return (a / (2 * PI)).toFloat()
                    }
                    detectDragGestures(
                        onDragStart = { live = angleToValue(it) },
                        onDragEnd = { vm.home.setBrightness((live * 100).roundToInt()); live = -1f },
                        onDragCancel = { live = -1f },
                    ) { ch, _ -> live = angleToValue(ch.position) }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        vm.home.setBrightness(if (vm.home.state.value.lightOn) 0 else 70)
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(520.dp)) {
                val k = size.width / 520f
                val c = Offset(size.width / 2, size.height / 2)
                val r = ringR * k
                drawCircle(p.track, r, c, style = Stroke(2f * k))
                drawArc(
                    p.warm, -90f, 360f * value, false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r),
                    style = Stroke(6f * k, cap = StrokeCap.Round)
                )
                val ang = (value * 2 * PI - PI / 2).toFloat()
                drawCircle(p.ink, 14f * k, Offset(c.x + r * cos(ang), c.y + r * sin(ang)))
                drawCircle(p.accentSoft.copy(alpha = 0.25f), 170f * k, Offset(c.x + 12 * k, c.y - 12 * k), style = Stroke(1.2f))
                drawCircle(p.ink.copy(alpha = 0.15f), 170f * k, Offset(c.x - 12 * k, c.y + 12 * k), style = Stroke(1.2f))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    T("${(value * 100).roundToInt()}", 136.sp, weight = FontWeight.ExtraLight, tracking = -0.05f, lineHeight = 0.9f)
                    T("%", 56.sp, Modifier.padding(bottom = 10.dp), color = p.muted, weight = FontWeight.ExtraLight)
                }
                T(if (value > 0f) "${home.tempName} · ${home.kelvin} K" else "Свет выключен", 16.sp, color = p.ink2, weight = FontWeight.Light)
            }
        }

        // Правая колонка
        Column(Modifier.at(700, 150).width(516.dp), verticalArrangement = Arrangement.spacedBy(34.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caps("Сцены")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Вечер", "Кино", "Чтение", "Гости").forEach { name ->
                        OutlinePill(
                            name, Modifier.weight(1f), height = 52.dp, fontSize = 16f,
                            selected = home.roomScene == name
                        ) { vm.home.roomScene(name) }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Caps("Температура света")
                var temp by remember(home.colorTemp) { mutableFloatStateOf(home.colorTemp) }
                DesignSlider(
                    temp, { temp = it }, p.warm, Modifier.fillMaxWidth(),
                    onChangeFinished = { vm.home.update { it.copy(colorTemp = temp) } }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    T("Тёплый", 13.sp, color = p.muted)
                    T("Холодный", 13.sp, color = p.muted)
                }
            }
            Column {
                Caps("Устройства", modifier = Modifier.padding(bottom = 6.dp))
                home.devices.forEach { d ->
                    Row(
                        Modifier.fillMaxWidth().height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        T(d.name, 17.sp, Modifier.weight(1f), weight = FontWeight.Light)
                        if (d.note != null && d.id == "ac") {
                            T(if (d.on) d.note else "выкл.", 15.sp, Modifier.padding(end = 16.dp), color = p.muted)
                        }
                        DesignSwitch(d.on) { vm.home.setDevice(d.id, it) }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
                }
            }
        }
        T(
            "Демо-режим: устройства не подключены",
            12.sp, Modifier.at(80, 740), color = p.muted
        )
    }
}
