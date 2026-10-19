package com.fidelityone.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens de tema — o coração do sistema visual.
 * Layouts-base alteram tokens (cantos/espaçamentos/tipografia/cards/barra),
 * temas de cor alteram a paleta Material 3. 5 × 4 = 20 presets.
 */
data class SpacingTokens(
    val card: Dp, val gutter: Dp, val itemHeight: Dp, val gridColumns: Int
)

data class ShapeTokens(val card: Shape, val bar: Shape, val button: Shape)

data class TypeTokens(val display: TextStyle, val title: TextStyle, val body: TextStyle, val caption: TextStyle)

enum class CardStyle { COVER_LARGE, COVER_GRID, ROW_COMPACT, TEXT_EDITORIAL }
enum class PlayerBarStyle { SLIM, FLOATING, FULL_WIDTH_GLASS }
enum class BackgroundStyle { SOLID, GRADIENT_DEEP, MUTED_WASH }

data class FidelityTokens(
    val spacing: SpacingTokens,
    val shapes: ShapeTokens,
    val type: TypeTokens,
    val cardStyle: CardStyle,
    val playerBar: PlayerBarStyle,
    val background: BackgroundStyle,
    val generousPadding: Boolean
)

val LocalFidelityTokens = staticCompositionLocalOf { defaultTokens() }

fun defaultTokens(): FidelityTokens = FidelityTokens(
    spacing = SpacingTokens(card = 12.dp, gutter = 16.dp, itemHeight = 64.dp, gridColumns = 2),
    shapes = ShapeTokens(
        card = RoundedCornerShape(16.dp),
        bar = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        button = RoundedCornerShape(50)
    ),
    type = TypeTokens(
        display = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = 0.3.sp),
        title = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 17.sp),
        body = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        caption = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.5.sp)
    ),
    cardStyle = CardStyle.COVER_GRID,
    playerBar = PlayerBarStyle.FLOATING,
    background = BackgroundStyle.SOLID,
    generousPadding = true
)

/** Mapeia os 5 layouts-base em conjuntos de tokens distintos. */
object LayoutTokens {
    fun forLayout(id: String): FidelityTokens {
        val base = defaultTokens()
        return when (id) {
            "classic_library" -> base // grid padrão

            "minimal_player" -> base.copy(
                spacing = SpacingTokens(16.dp, 24.dp, 56.dp, 1),
                type = base.type.copy(display = base.type.display.copy(fontSize = 24.sp, fontWeight = FontWeight.Normal)),
                cardStyle = CardStyle.ROW_COMPACT,
                playerBar = PlayerBarStyle.SLIM,
                background = BackgroundStyle.SOLID,
                generousPadding = false
            )

            "audiophile_dashboard" -> base.copy(
                spacing = SpacingTokens(14.dp, 14.dp, 72.dp, 3),
                shapes = base.shapes.copy(card = RoundedCornerShape(10.dp)),
                type = base.type.copy(caption = base.type.caption.copy(fontSize = 12.sp)),
                cardStyle = CardStyle.COVER_LARGE,
                playerBar = PlayerBarStyle.FULL_WIDTH_GLASS,
                background = BackgroundStyle.GRADIENT_DEEP,
                generousPadding = true
            )

            "editorial_premium" -> base.copy(
                spacing = SpacingTokens(20.dp, 28.dp, 88.dp, 1),
                shapes = base.shapes.copy(card = RoundedCornerShape(4.dp)),
                type = base.type.copy(
                    display = base.type.display.copy(fontFamily = FontFamily.Serif, fontSize = 36.sp, fontWeight = FontWeight.Bold),
                    title = base.type.title.copy(fontFamily = FontFamily.Serif, fontSize = 19.sp)
                ),
                cardStyle = CardStyle.TEXT_EDITORIAL,
                playerBar = PlayerBarStyle.FULL_WIDTH_GLASS,
                background = BackgroundStyle.MUTED_WASH,
                generousPadding = true
            )

            "compact_hifi" -> base.copy(
                spacing = SpacingTokens(8.dp, 10.dp, 52.dp, 4),
                shapes = base.shapes.copy(card = RoundedCornerShape(8.dp)),
                type = base.type.copy(title = base.type.title.copy(fontSize = 14.sp), body = base.type.body.copy(fontSize = 12.sp)),
                cardStyle = CardStyle.COVER_GRID,
                playerBar = PlayerBarStyle.SLIM,
                background = BackgroundStyle.SOLID,
                generousPadding = false
            )

            else -> base
        }
    }
}

/** Tipografia Material derivada dos tokens, para manter consistência. */
fun typographyFrom(t: TypeTokens): Typography = Typography(
    headlineMedium = t.display,
    titleMedium = t.title,
    bodyMedium = t.body,
    labelSmall = t.caption
)
