package ltd.fky.devicecontrol.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 「小乖生活」主题入口。
 *
 * 默认使用 [LightColors] / [DarkColors] 这套 Anthropic 暖色板，
 * **不启用** Material You 动态取色 —— 动态取色会从用户壁纸提取配色，
 * 壁纸偏冷色时整个界面会变成紫 / 蓝色，与既定视觉取向冲突。
 * 如需恢复，可在调用处显式传入 `dynamicColor = true`
 * （后续「设置 → 外观」会把这个开关暴露给用户）。
 *
 * @param darkTheme    是否使用深色主题，默认跟随系统。
 * @param dynamicColor 是否启用动态取色，**默认关闭**。
 */
@Composable
fun XiaoGuaiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
