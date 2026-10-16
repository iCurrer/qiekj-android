package ltd.fky.devicecontrol.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 「小乖生活」色板 —— Anthropic 暖色系。
 *
 * 色值来源为 Anthropic 官方品牌规范（`anthropics/skills` → `brand-guidelines`）：
 *   Orange `#D97757`（主强调）、Light `#FAF9F5`（浅色背景）、
 *   Dark `#141413`（主文字）、Light Gray `#E8E6DC`、Mid Gray `#B0AEA5`。
 * 另取 Claude 官方编辑器主题实测的 Terracotta `#C96442` 作为 `primary`，
 * 其与白字的对比度为 3.9:1，达 Material 3 对填充按钮的要求。
 *
 * 设计取向：低饱和暖色 + 纸感底色，避免高饱和渐变与大面积色块，
 * 刻意不使用冷色（紫 / 蓝）作为主色。
 */

// ---------------------------------------------------------------------------
// 原始色值（便于跨文件引用与后续设置页扩展）
// ---------------------------------------------------------------------------

/** Anthropic Orange，官方主强调色。 */
val AnthropicOrange = Color(0xFFD97757)

/** Terracotta，用于主按钮填充。 */
val TerraCotta = Color(0xFFC96442)

/** Ivory，浅色模式页面底色。 */
val AnthropicIvory = Color(0xFFFAF9F5)

/** Near Black，主文字与深色模式底色。 */
val AnthropicInk = Color(0xFF141413)

/** Warm Sand，浅色模式次级背景。 */
val WarmSand = Color(0xFFE8E6DC)

/** Mid Gray，次要文字。 */
val AnthropicMidGray = Color(0xFFB0AEA5)

/** Olive Gray，次级文字（浅色模式）。 */
val OliveGray = Color(0xFF5E5D59)

/** Dark Intermediate，深色模式次级表面。 */
val DarkIntermediate = Color(0xFF1F1F1D)

// ---------------------------------------------------------------------------
// 浅色方案
// ---------------------------------------------------------------------------

val LightColors = lightColorScheme(
    primary = TerraCotta,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF7DFD4),
    onPrimaryContainer = Color(0xFF4A1F10),
    inversePrimary = Color(0xFFE8A88C),

    secondary = Color(0xFF6B6B69),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECEAE3),
    onSecondaryContainer = Color(0xFF2A2A28),

    tertiary = AnthropicOrange,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF8E2D8),
    onTertiaryContainer = Color(0xFF4A1B0C),

    error = Color(0xFFB53333),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7C1C1),
    onErrorContainer = Color(0xFF501313),

    background = AnthropicIvory,
    onBackground = AnthropicInk,

    surface = AnthropicIvory,
    onSurface = AnthropicInk,
    surfaceVariant = Color(0xFFECEAE3),
    onSurfaceVariant = OliveGray,

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F4EF),
    surfaceContainer = Color(0xFFF0EEE6),
    surfaceContainerHigh = WarmSand,
    surfaceContainerHighest = Color(0xFFE2E0D5),

    outline = Color(0xFF87867F),
    outlineVariant = Color(0xFFE0DED3),

    inverseSurface = Color(0xFF2C2C2A),
    inverseOnSurface = Color(0xFFF5F4EF),

    surfaceTint = TerraCotta,
    scrim = Color(0xFF000000),
)

// ---------------------------------------------------------------------------
// 深色方案
// ---------------------------------------------------------------------------

val DarkColors = darkColorScheme(
    primary = AnthropicOrange,
    onPrimary = Color(0xFF3A1408),
    primaryContainer = Color(0xFF5A2A16),
    onPrimaryContainer = Color(0xFFF8DCCF),
    inversePrimary = TerraCotta,

    secondary = AnthropicMidGray,
    onSecondary = Color(0xFF2C2C2A),
    secondaryContainer = Color(0xFF3D3D3A),
    onSecondaryContainer = WarmSand,

    tertiary = Color(0xFFE8A88C),
    onTertiary = Color(0xFF4A1B0C),
    tertiaryContainer = Color(0xFF6B3320),
    onTertiaryContainer = Color(0xFFF8DCCF),

    error = Color(0xFFE24B4A),
    onError = Color(0xFF501313),
    errorContainer = Color(0xFF791F1F),
    onErrorContainer = Color(0xFFF7C1C1),

    background = AnthropicInk,
    onBackground = AnthropicIvory,

    surface = AnthropicInk,
    onSurface = AnthropicIvory,
    surfaceVariant = Color(0xFF3D3D3A),
    onSurfaceVariant = AnthropicMidGray,

    surfaceContainerLowest = Color(0xFF0E0E0D),
    surfaceContainerLow = DarkIntermediate,
    surfaceContainer = Color(0xFF262624),
    surfaceContainerHigh = Color(0xFF30302E),
    surfaceContainerHighest = Color(0xFF3D3D3A),

    outline = Color(0xFF5E5D59),
    outlineVariant = Color(0xFF30302E),

    inverseSurface = WarmSand,
    inverseOnSurface = Color(0xFF2C2C2A),

    surfaceTint = AnthropicOrange,
    scrim = Color(0xFF000000),
)

// ---------------------------------------------------------------------------
// 终端日志区令牌
// ---------------------------------------------------------------------------

/**
 * 终端日志窗专用配色。
 *
 * 日志窗口固定使用暖深色底 + 暖白文字，贴近 Claude Code 的终端观感；
 * 该区块不随明暗主题切换，以保证日志阅读的一致性。
 */
val TerminalSurface = Color(0xFF1F1E1D)
val TerminalText = Color(0xFFE8E6DC)
val TerminalHint = Color(0xFF87867F)
val TerminalAccent = AnthropicOrange
val TerminalDivider = Color(0xFF30302E)

/** 终端窗口顶部信号灯（纯装饰，沿用 macOS 传统配色）。 */
val TerminalDotRed = Color(0xFFE0695F)
val TerminalDotYellow = Color(0xFFE0B15F)
val TerminalDotGreen = Color(0xFF7BA05B)
