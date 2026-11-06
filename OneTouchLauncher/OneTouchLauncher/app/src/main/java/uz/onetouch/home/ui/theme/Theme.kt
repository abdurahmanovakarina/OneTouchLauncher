package uz.onetouch.home.ui.theme

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** Палитры сняты 1:1 с макета OneTouch Home OS. */
@Immutable
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val ink: Color,
    val ink2: Color,
    val muted: Color,
    val line: Color,
    val border: Color,
    val borderStrong: Color,
    val track: Color,
    val disc: Color,
    val accent: Color,
    val accentSoft: Color,
    val warm: Color,
    val live: Color,
    val dotIdle: Color,
    val activeBg: Color,
    val activeFg: Color,
    val activeSub: Color,
)

val DarkPalette = Palette(
    isDark = true,
    bg = Color(0xFF0D0F10),
    surface = Color(0xFF17191B),
    ink = Color(0xFFF2EFEA),
    ink2 = Color(0xFFB9B6B0),
    muted = Color(0xFF8C918F),
    line = Color(0xFF222527),
    border = Color(0xFF2A2D2F),
    borderStrong = Color(0xFF3A3D3F),
    track = Color(0xFF26292B),
    disc = Color(0xFF121416),
    accent = Color(0xFF3FB8C9),
    accentSoft = Color(0xFF7ACFE0),
    warm = Color(0xFFE9C58E),
    live = Color(0xFFE8604C),
    dotIdle = Color(0xFF5A5F61),
    activeBg = Color(0xFFF2EFEA),
    activeFg = Color(0xFF0D0F10),
    activeSub = Color(0xFF55524D),
)

val LightPalette = Palette(
    isDark = false,
    bg = Color(0xFFF4F2EE),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF141516),
    ink2 = Color(0xFF3A3936),
    muted = Color(0xFF6E6B66),
    line = Color(0xFFE2DED8),
    border = Color(0xFFDAD5CE),
    borderStrong = Color(0xFFDAD5CE),
    track = Color(0xFFE2DED8),
    disc = Color(0xFFFFFFFF),
    accent = Color(0xFF138A9A),
    accentSoft = Color(0xFF7ACFE0),
    warm = Color(0xFFA36A1F),
    live = Color(0xFFC0392B),
    dotIdle = Color(0xFFB5AFA6),
    activeBg = Color(0xFF141516),
    activeFg = Color(0xFFFFFFFF),
    activeSub = Color(0xFFB9B6B0),
)

/** Цвет «ночных» часов из макета «Сон 4 · Ночь». */
val NightOrange = Color(0xFFC8642F)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

object AppFonts {
    /**
     * Inter Tight из assets/fonts (если файлы положены), иначе системный шрифт.
     * Проверяем наличие файлов заранее, чтобы не упасть при отрисовке текста.
     */
    fun load(context: Context): FontFamily {
        val files = runCatching { context.assets.list("fonts")?.toSet() }.getOrNull() ?: emptySet()
        val map = listOf(
            "InterTight-ExtraLight.ttf" to FontWeight.ExtraLight,
            "InterTight-Light.ttf" to FontWeight.Light,
            "InterTight-Regular.ttf" to FontWeight.Normal,
            "InterTight-Medium.ttf" to FontWeight.Medium,
        ).filter { it.first in files }
        if (map.isEmpty()) return FontFamily.Default
        return FontFamily(map.map { (file, w) -> Font("fonts/$file", context.assets, w) })
    }
}

/** Размеры макета: всё рисуется в координатах 1280×800 и масштабируется под экран. */
const val DESIGN_W = 1280f
const val DESIGN_H = 800f

@Composable
fun DesignFrame(background: Color, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(background)
    ) {
        val scale = min(maxWidth.value / DESIGN_W, maxHeight.value / DESIGN_H)
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density * scale, 1f)) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(DESIGN_W.dp, DESIGN_H.dp),
                content = content
            )
        }
    }
}

/** Короткий помощник для текста в стиле макета. */
@Composable
fun T(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = LocalPalette.current.ink,
    weight: FontWeight = FontWeight.Normal,
    tracking: Float = 0f,
    lineHeight: Float = 1.25f,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    caps: Boolean = false,
) {
    Text(
        text = if (caps) text.uppercase() else text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = LocalFont.current,
            fontSize = size,
            fontWeight = weight,
            color = color,
            letterSpacing = tracking.em,
            lineHeight = (size.value * lineHeight).sp,
            textAlign = align ?: TextAlign.Unspecified,
        )
    )
}
