package uz.onetouch.home.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.onetouch.home.LauncherViewModel
import uz.onetouch.home.data.SaverStyle
import uz.onetouch.home.ui.components.Caps
import uz.onetouch.home.ui.components.Ic
import uz.onetouch.home.ui.components.LineIcon
import uz.onetouch.home.ui.components.Orbits
import uz.onetouch.home.ui.components.VoiceRings
import uz.onetouch.home.ui.components.at
import uz.onetouch.home.ui.components.tap
import uz.onetouch.home.ui.theme.LocalPalette
import uz.onetouch.home.ui.theme.NightOrange
import uz.onetouch.home.ui.theme.T
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.sin

private val RU = Locale.forLanguageTag("ru")
private fun capsDate(t: LocalDateTime) = t.format(DateTimeFormatter.ofPattern("EEEE · d MMMM", RU)).uppercase(RU)

/** Фон заставки — нужен MainActivity, чтобы залить поля вокруг макета. */
fun saverBackground(style: SaverStyle, dark: Boolean): Color = when (style) {
    SaverStyle.NIGHT -> Color.Black
    SaverStyle.LIGHT -> Color(0xFF14110E)
    else -> if (dark) Color(0xFF06070A) else Color(0xFFF4F2EE)
}

/**
 * Заставки «Сон 1–4». Касание в любом месте — возврат на главный экран.
 * Позиция содержимого медленно «плавает», чтобы не выжигать экран.
 */
@Composable
fun SaverScreen(vm: LauncherViewModel, style: SaverStyle) {
    val now by vm.now.collectAsStateWithLifecycle()
    // Сдвиг против выгорания: несколько пикселей, меняется раз в минуту.
    val drift = Offset((sin(now.minute / 9.5) * 10).toFloat(), (sin(now.minute / 6.3) * 8).toFloat())
    Box(
        Modifier
            .fillMaxSize()
            .tap { vm.goHome() }
    ) {
        Box(Modifier.fillMaxSize().offset(drift.x.dp, drift.y.dp)) {
            when (style) {
                SaverStyle.LIGHT -> SaverLight(vm, now)
                SaverStyle.MUSIC -> SaverMusic(vm, now)
                SaverStyle.NIGHT -> SaverNight(vm, now)
                else -> SaverOrbits(vm, now)
            }
        }
    }
}

@Composable
private fun SaverOrbits(vm: LauncherViewModel, now: LocalDateTime) {
    val p = LocalPalette.current
    val weather by vm.weather.collectAsStateWithLifecycle()
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        Orbits(
            700.dp, p.accentSoft, p.ink, alphaA = 0.35f, alphaB = 0.25f, shift = 0.05f,
            modifier = Modifier.at(290, 50)
        )
        Canvas(Modifier.at(330, 90).size(620.dp)) {
            drawCircle(p.accent.copy(alpha = 0.3f), size.minDimension / 2 - 2, style = Stroke(1.2f))
        }
        Column(
            Modifier.at(0, 290).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            T(timeText(now), 200.sp, weight = FontWeight.ExtraLight, tracking = -0.04f, lineHeight = 0.9f)
            Box(Modifier.height(18.dp))
            val w = weather
            T(capsDate(now) + (if (w != null) " · ${w.temp}°" else ""), 15.sp, color = p.ink2, weight = FontWeight.Medium, tracking = 0.3f)
        }
        agenda?.nextAlarm?.let {
            T("Будильник $it", 15.sp, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp), color = p.muted, weight = FontWeight.Light)
        }
    }
}

@Composable
private fun SaverLight(vm: LauncherViewModel, now: LocalDateTime) {
    val t = rememberInfiniteTransition(label = "light")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF14110E))
            val w = size.width
            val h = size.height
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF1C6F78).copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(w * (0.68f + 0.08f * a), h * (0.30f - 0.08f * a)), radius = w * 0.42f
                ),
                radius = w * 0.42f, center = Offset(w * (0.68f + 0.08f * a), h * (0.30f - 0.08f * a))
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF8A6A35).copy(alpha = 0.75f), Color.Transparent),
                    center = Offset(w * (0.32f - 0.06f * a), h * (0.78f + 0.04f * a)), radius = w * 0.36f
                ),
                radius = w * 0.36f, center = Offset(w * (0.32f - 0.06f * a), h * (0.78f + 0.04f * a))
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF3A5A4C).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f), radius = w * 0.3f
                ),
                radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.5f)
            )
        }
        Column(Modifier.at(64, 620)) {
            T(timeText(now), 92.sp, color = Color(0xFFF2EFEA), weight = FontWeight.ExtraLight, tracking = -0.03f, lineHeight = 0.95f)
            T(capsDate(now), 12.sp, color = Color(0xFFB9B6B0), weight = FontWeight.Medium, tracking = 0.3f)
        }
    }
}

@Composable
private fun SaverMusic(vm: LauncherViewModel, now: LocalDateTime) {
    val p = LocalPalette.current
    val media by vm.media.state.collectAsStateWithLifecycle()
    val home by vm.home.state.collectAsStateWithLifecycle()
    val progress = rememberProgress(media)
    Box(Modifier.fillMaxSize()) {
        // Диск с обложкой
        Box(Modifier.at(100, 180).size(440.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(440.dp)) {
                drawCircle(Brush.radialGradient(listOf(p.ink.copy(alpha = 0.06f), Color.Transparent)))
                for (i in 1..5) drawCircle(p.ink.copy(alpha = 0.04f), size.minDimension / 2 * i / 5f, style = Stroke(1f))
            }
            val art = media?.art
            if (art != null) {
                Image(
                    art.asImageBitmap(), null,
                    Modifier.size(150.dp).clip(CircleShape), contentScale = ContentScale.Crop
                )
            }
            VoiceRings(240.dp, listening = media?.playing == true, level = 0f)
        }
        Column(Modifier.at(760, 250).width(470.dp)) {
            Caps("${home.roomName} · сейчас играет", size = 11f, tracking = 0.3f)
            Box(Modifier.height(12.dp))
            T(media?.title ?: "Тишина", 56.sp, weight = FontWeight.Light, tracking = -0.02f, lineHeight = 1.05f, maxLines = 2)
            T(media?.artist ?: "", 18.sp, color = p.muted, weight = FontWeight.Light, maxLines = 1)
            Box(Modifier.height(44.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val m = media
                T(fmtMs(m?.position() ?: 0), 11.sp, color = p.muted)
                Box(Modifier.weight(1f).height(2.dp).background(p.track)) {
                    Box(Modifier.fillMaxWidth(progress).height(2.dp).background(p.ink))
                }
                T(fmtMs(m?.durationMs ?: 0), 11.sp, color = p.muted)
            }
            Box(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color.Transparent)
                        .then(Modifier.tap { vm.media.toggle() }),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(72.dp)) { drawCircle(p.ink.copy(alpha = 0.5f), size.minDimension / 2 - 1, style = Stroke(1.5f)) }
                    if (media?.playing == true) LineIcon(Ic.pause, p.ink, 20.dp) else LineIcon(Ic.play, p.ink, 20.dp, filled = true)
                }
                Box(Modifier.width(20.dp))
                Box(Modifier.size(48.dp).tap { vm.media.next() }, contentAlignment = Alignment.Center) { LineIcon(Ic.next, p.ink, 20.dp) }
                Box(Modifier.weight(1f))
                T(timeText(now), 40.sp, color = p.ink2, weight = FontWeight.ExtraLight)
            }
        }
    }
}

@Composable
private fun SaverNight(vm: LauncherViewModel, now: LocalDateTime) {
    val agenda by vm.agenda.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.at(0, 290).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            T(timeText(now), 200.sp, color = NightOrange, weight = FontWeight.ExtraLight, tracking = -0.04f, lineHeight = 0.9f)
            Box(Modifier.height(18.dp))
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(3.dp)).background(NightOrange))
            agenda?.nextAlarm?.let {
                Box(Modifier.height(14.dp))
                T(it, 15.sp, color = NightOrange.copy(alpha = 0.85f), weight = FontWeight.Medium, tracking = 0.3f)
            }
        }
    }
}

fun fmtMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
