package uz.onetouch.home.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.onetouch.home.ui.theme.LocalPalette
import uz.onetouch.home.ui.theme.T

/** Абсолютное позиционирование в координатах макета 1280×800. */
fun Modifier.at(x: Number, y: Number): Modifier = this.offset(x.toFloat().dp, y.toFloat().dp)

/** Клик без «ряби» — в макете её нет. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tap(onLongClick: (() -> Unit)? = null, onClick: () -> Unit): Modifier =
    if (onLongClick == null) {
        this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
    } else {
        this.combinedClickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick
        )
    }

private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r $r" +
        "v${h - 2 * r}a$r $r 0 0 1 -$r $r" +
        "h-${w - 2 * r}a$r $r 0 0 1 -$r -$r" +
        "v-${h - 2 * r}a$r $r 0 0 1 $r -${r}z"

/** Линейные иконки — SVG-пути взяты прямо из макета (viewBox 24×24, stroke 1.5). */
object Ic {
    const val bulb = "M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.8.8 1 1.5 1 2.5h6c0-1 .2-1.7 1-2.5A6 6 0 0 0 12 3z"
    const val thermo = "M14 14.8V5a2 2 0 1 0-4 0v9.8a4 4 0 1 0 4 0z"
    const val curtains = "M3 4h18M5 4v16M19 4v16M9 4v9M15 4v9"
    const val shield = "M12 3l7 3v6c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z"
    const val moon = "M12 3a6 6 0 1 0 9 9 9 9 0 1 1-9-9z"
    const val exit = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"
    val tv = rr(2f, 5f, 20f, 13f, 2f) + "M8 21h8"
    val grid = rr(3f, 3f, 7f, 7f, 1.5f) + rr(14f, 3f, 7f, 7f, 1.5f) + rr(3f, 14f, 7f, 7f, 1.5f) + rr(14f, 14f, 7f, 7f, 1.5f)
    val mic = rr(9f, 3f, 6f, 11f, 3f) + "M5 11a7 7 0 0 0 14 0M12 18v3"
    const val chevronR = "M9 6l6 6-6 6"
    const val chevronL = "M15 6l-6 6 6 6"
    const val chevronDown = "M6 9l6 6 6-6"
    const val prev = "M19 5v14L8 12zM5 5v14"
    const val next = "M5 5v14l11-7zM19 5v14"
    const val pause = "M9 5v14M15 5v14"
    const val play = "M8 5v14l11-7z"
    const val sun = "M12 8a4 4 0 1 0 0 8 4 4 0 1 0 0-8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"
    const val home = "M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"
    const val users = "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM2 21v-1a6 6 0 0 1 12 0v1M16 3.5a4 4 0 0 1 0 7.5M22 21v-1a6 6 0 0 0-4-5.6"
    const val close = "M6 6l12 12M18 6L6 18"
    const val plus = "M12 5v14M5 12h14"
    const val search = "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM21 21l-4.3-4.3"
    const val volume = "M4 9v6h4l5 4V5L8 9zM16.5 8.5a5 5 0 0 1 0 7M19 6a8.5 8.5 0 0 1 0 12"
    const val settings = "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM12 2v3M12 19v3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M2 12h3M19 12h3M4.9 19.1L7 17M17 7l2.1-2.1"
    const val book = "M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2zM4 21a2 2 0 0 1 2-2h13v2"
    val film = rr(3f, 4f, 18f, 16f, 2f) + "M3 9h18M8 4v5M16 4v5"
}

@Composable
fun LineIcon(
    d: String,
    color: Color,
    size: Dp = 22.dp,
    strokeWidth: Float = 1.5f,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val path = remember(d) { PathParser().parsePathString(d).toPath() }
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            if (filled) drawPath(path, color, style = Fill)
            drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Логотип: три кольца. */
@Composable
fun Logo(size: Dp = 22.dp) {
    val p = LocalPalette.current
    Canvas(Modifier.size(size)) {
        val k = this.size.minDimension / 100f
        val st = Stroke(width = 7f * k)
        drawCircle(p.accentSoft, 38f * k, Offset(54f * k, 47f * k), style = st)
        drawCircle(p.ink, 38f * k, Offset(47f * k, 54f * k), style = st)
        drawCircle(p.accent, 24f * k, Offset(51f * k, 50f * k), style = st)
    }
}

@Composable
fun Header(onLogoLongPress: () -> Unit, page: Int, pages: Int, onPage: (Int) -> Unit, right: @Composable () -> Unit) {
    val p = LocalPalette.current
    Box(Modifier.at(64, 44).width(1152.dp).height(44.dp)) {
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .tap(onLongClick = onLogoLongPress) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Logo()
            T("OneTouch Home", 13.sp, color = p.muted, weight = FontWeight.Medium, tracking = 0.32f, caps = true)
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) { right() }
    }
    PageDots(page, pages, onPage, Modifier.at(574 - (pages - 3) * 22, 44))
}

@Composable
fun PageDots(page: Int, pages: Int, onPage: (Int) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(modifier) {
        repeat(pages) { i ->
            val w by animateFloatAsState(if (i == page) 22f else 6f, label = "dot")
            val c by animateColorAsState(if (i == page) p.ink else p.dotIdle, label = "dotc")
            Box(Modifier.size(44.dp).tap { onPage(i) }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(w.dp, 6.dp).clip(RoundedCornerShape(3.dp)).background(c))
            }
        }
    }
}

@Composable
fun VoicePill(onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (p.isDark) Color.Transparent else p.surface)
            .border(1.dp, p.border, RoundedCornerShape(22.dp))
            .tap(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LineIcon(Ic.mic, p.ink, 16.dp)
        T("Скажите «OneTouch»", 14.sp)
    }
}

@Composable
fun Avatars(names: List<String>) {
    val p = LocalPalette.current
    val colors = listOf(Color(0xFFE9C58E), Color(0xFF7ACFE0), Color(0xFFB9B6B0), Color(0xFFC9B7E8))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row {
            names.forEachIndexed { i, n ->
                Box(
                    Modifier
                        .offset(x = (-8 * i).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(p.bg)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(colors[i % colors.size]),
                    contentAlignment = Alignment.Center
                ) { T(n.take(1).uppercase(), 13.sp, color = Color(0xFF0D0F10), weight = FontWeight.Medium) }
            }
        }
        val label = when (names.size) {
            1 -> "дома один"; 2 -> "дома двое"; 3 -> "дома трое"; 4 -> "дома четверо"; else -> "дома ${names.size}"
        }
        T(label, 14.sp, Modifier.offset(x = (-8 * (names.size - 1)).dp), color = p.muted, weight = FontWeight.Light)
    }
}

/** Плитка устройства/комнаты 120×124 из главного экрана. */
@Composable
fun DeviceTile(
    icon: String, title: String, subtitle: String, active: Boolean,
    width: Dp = 120.dp, height: Dp = 124.dp,
    onLongClick: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val bg by animateColorAsState(if (active) p.activeBg else p.surface, label = "tile")
    val fg = if (active) p.activeFg else p.ink
    val sub = if (active) p.activeSub else p.muted
    Column(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .tap(onLongClick, onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LineIcon(icon, fg)
        Column {
            T(title, 15.sp, color = fg, weight = FontWeight.Medium, maxLines = 1)
            T(subtitle, 13.sp, color = sub, maxLines = 1)
        }
    }
}

/** Кнопка-«пилюля» с обводкой. */
@Composable
fun OutlinePill(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    iconColor: Color? = null,
    height: Dp = 56.dp,
    fontSize: Float = 15f,
    selected: Boolean = false,
    filledAccent: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val bg = when {
        filledAccent -> p.accent
        selected -> p.activeBg
        p.isDark -> Color.Transparent
        else -> p.surface
    }
    val fg = when {
        filledAccent -> Color(0xFF0D0F10)
        selected -> p.activeFg
        else -> p.ink
    }
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(bg)
            .then(if (selected || filledAccent) Modifier else Modifier.border(1.dp, p.border, shape))
            .tap(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null) LineIcon(icon, iconColor ?: fg, 16.dp)
        T(text, fontSize.sp, color = fg, weight = if (selected || filledAccent) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
fun RoundIconButton(icon: String, size: Dp = 44.dp, iconSize: Dp = 18.dp, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, p.border, CircleShape)
            .tap(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { LineIcon(icon, p.ink, iconSize) }
}

/** Кнопки «назад/вперёд/пауза» из круглого плеера. */
@Composable
fun TransportControls(
    playing: Boolean,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    big: Dp = 64.dp,
    filledOnLight: Boolean = true,
) {
    val p = LocalPalette.current
    val filled = !p.isDark && filledOnLight
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(48.dp).tap(onClick = onPrev), contentAlignment = Alignment.Center) { LineIcon(Ic.prev, p.ink, 20.dp) }
        Box(
            Modifier
                .size(big)
                .clip(CircleShape)
                .background(if (filled) p.ink else Color.Transparent)
                .then(if (filled) Modifier else Modifier.border(1.dp, p.ink, CircleShape))
                .tap(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            val c = if (filled) p.bg else p.ink
            if (playing) LineIcon(Ic.pause, c, 20.dp) else LineIcon(Ic.play, c, 20.dp, filled = true)
        }
        Box(Modifier.size(48.dp).tap(onClick = onNext), contentAlignment = Alignment.Center) { LineIcon(Ic.next, p.ink, 20.dp) }
    }
}

/**
 * Медленно вращающиеся «орбиты» — фирменный элемент макета.
 * Вращение делается через graphicsLayer, поэтому почти не грузит процессор.
 */
@Composable
fun Orbits(
    diameter: Dp,
    ringA: Color,
    ringB: Color,
    alphaA: Float = 0.28f,
    alphaB: Float = 0.2f,
    shift: Float = 0.028f,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "orbits")
    val a1 by t.animateFloat(0f, 360f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "o1")
    val a2 by t.animateFloat(360f, 0f, infiniteRepeatable(tween(180_000, easing = LinearEasing)), label = "o2")
    Box(modifier.size(diameter)) {
        Canvas(Modifier.size(diameter).graphicsLayer { rotationZ = if (animate) a1 else 0f }) {
            val r = size.minDimension * 0.428f
            val c = Offset(size.width * (0.5f + shift / 2), size.height * (0.5f - shift / 2))
            drawCircle(ringA.copy(alpha = alphaA), r, c, style = Stroke(1.2f))
            drawCircle(ringA, size.minDimension * 0.006f, Offset(c.x + r, c.y))
        }
        Canvas(Modifier.size(diameter).graphicsLayer { rotationZ = if (animate) a2 else 0f }) {
            val r = size.minDimension * 0.428f
            val c = Offset(size.width * (0.5f - shift / 2), size.height * (0.5f + shift / 2))
            drawCircle(ringB.copy(alpha = alphaB), r, c, style = Stroke(1.2f))
            drawCircle(ringB, size.minDimension * 0.005f, Offset(c.x, c.y - r))
        }
    }
}

/** Пульсирующие кольца голосового ассистента. */
@Composable
fun VoiceRings(diameter: Dp, listening: Boolean, level: Float) {
    val p = LocalPalette.current
    val t = rememberInfiniteTransition(label = "voice")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "spin")
    val lv by animateFloatAsState(level.coerceIn(0f, 1f), tween(120), label = "lvl")
    Canvas(Modifier.size(diameter)) {
        val m = size.minDimension
        val k = if (listening) 1f + 0.04f * pulse + 0.06f * lv else 1f
        val c = center
        rotate(spin) {
            drawCircle(p.accentSoft, m * 0.40f * k, Offset(c.x + m * 0.03f, c.y - m * 0.03f), style = Stroke(m * 0.012f))
        }
        rotate(-spin * 0.7f) {
            drawCircle(p.ink, m * 0.40f * k, Offset(c.x - m * 0.03f, c.y + m * 0.03f), style = Stroke(m * 0.012f))
        }
        drawCircle(p.accent, m * 0.25f * (1f + 0.1f * lv), c, style = Stroke(m * 0.012f))
    }
}

/** Ползунок в стиле макета (тонкая дорожка + круглая ручка 28 dp). */
@Composable
fun DesignSlider(
    value: Float,
    onChange: (Float) -> Unit,
    fill: Color,
    modifier: Modifier = Modifier,
    onChangeFinished: () -> Unit = {},
) {
    val p = LocalPalette.current
    val change by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onChangeFinished)
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { o -> change((o.x / size.width).coerceIn(0f, 1f)); finished() }
            }
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { finished() }) { ch, _ ->
                    change((ch.position.x / size.width).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val w = maxWidth
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(p.track))
        Box(Modifier.width(w * value).height(4.dp).clip(RoundedCornerShape(2.dp)).background(fill))
        Box(
            Modifier
                .offset(x = w * value - 14.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(p.ink)
        )
    }
}

/** Переключатель 44×26 как в макете «Комната». */
@Composable
fun DesignSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val x by animateFloatAsState(if (checked) 20f else 2f, label = "sw")
    val bg by animateColorAsState(if (checked) p.ink else p.track, label = "swbg")
    Box(
        Modifier
            .size(46.dp, 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .tap { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = x.dp, y = 3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) p.bg else p.muted)
        )
    }
}

/** Заголовок секции капсом с разрядкой. */
@Composable
fun Caps(text: String, color: Color = LocalPalette.current.muted, size: Float = 13f, tracking: Float = 0.24f, modifier: Modifier = Modifier) {
    T(text, size.sp, modifier, color = color, weight = FontWeight.Medium, tracking = tracking, caps = true)
}

@Composable
fun HDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.line))
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

/** Иллюстрация «камеры у двери» из макета (пока камера не подключена). */
@Composable
fun DoorIllustration(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val sx = size.width / 206f
        val sy = size.height / 200f
        val dark = p.isDark
        drawRect(if (dark) Color(0xFF1A1D1F) else Color(0xFFE8E4DE))
        drawRect(
            if (dark) Color(0xFF24292B) else Color(0xFFD9D2C8),
            topLeft = Offset(62 * sx, 34 * sy), size = Size(82 * sx, 126 * sy)
        )
        drawRect(
            if (dark) Color(0xFF33393C) else Color(0xFFC9C1B5),
            topLeft = Offset(62 * sx, 34 * sy), size = Size(82 * sx, 126 * sy), style = Stroke(1f)
        )
        drawCircle(p.muted, 3 * sx, Offset(132 * sx, 100 * sy))
    }
}
