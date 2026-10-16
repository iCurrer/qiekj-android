package ltd.fky.devicecontrol.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 形状令牌。
 *
 * 整体走「方正偏柔」路线：卡片圆角收敛到中小尺寸，
 * 避免 Material 3 默认的饱满圆角带来的「果冻感」，
 * 更贴近工程工具类应用的克制观感。
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)
