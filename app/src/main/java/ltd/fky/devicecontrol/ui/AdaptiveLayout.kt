package ltd.fky.devicecontrol.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口宽度档位。
 *
 * 断点沿用 Material 3 的 window size class（600 / 840 dp），
 * 与官方 `material3.adaptive` 的判定口径一致，但不额外引入依赖：
 * `LocalConfiguration.screenWidthDp` 在多窗口 / 分屏下反映的就是**当前窗口**宽度，
 * 对本项目的需求（挑断点、限宽、决定列数）已经够用。
 */
internal enum class WindowWidthClass {
    /** < 600dp：手机竖屏。沿用原来的单列 + 底部导航。 */
    Compact,

    /** 600–839dp：手机横屏、小折叠展开、小平板。导航改左侧，内容限宽。 */
    Medium,

    /** ≥ 840dp：平板、桌面模式。导航左侧，设备列表并排两列。 */
    Expanded,
}

@Composable
internal fun rememberWindowWidthClass(): WindowWidthClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WindowWidthClass.Compact
        widthDp < 840 -> WindowWidthClass.Medium
        else -> WindowWidthClass.Expanded
    }
}

/**
 * 页面内容宽度上限。
 *
 * 宽屏上元素被无限拉伸是「又宽又扁」的根源 —— 一行文字跨越 1000dp 会极难扫读，
 * 卡片也会变成扁条。按内容类型分档限宽，比统一一个值更贴合各自的信息密度。
 */
internal object PageWidth {
    /** 表单与强交互内容：手机竖屏的最大宽度，再宽输入框就失去焦点感 */
    val Narrow = 560.dp

    /** 通用列表、设置、说明类的阅读宽度 */
    val Regular = 760.dp

    /** 终端日志这类偏宽的内容，窄了反而难读 */
    val Wide = 900.dp

    /** 设备网格：留出并排两列的余地 */
    val Grid = 1040.dp
}

/**
 * 页面容器：宽屏下把内容**限宽并水平居中**，窄屏下等价于原来的整屏布局。
 *
 * 各页面原本都是 `Column(Modifier.fillMaxSize().padding(horizontal = 20.dp))`，
 * 在平板上会一路铺满 1200dp。换成这个容器后，内容宽度被约束在 [maxWidth] 内，
 * 两侧留白，视觉重心回到中间。
 *
 * @param scrollable 页面自身需要滚动时置 true（会把垂直滚动装进容器）。
 *   列表类页面用 LazyColumn / LazyVerticalGrid 自带滚动，保持 false。
 */
@Composable
internal fun PageContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = PageWidth.Regular,
    scrollable: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(
                    if (scrollable) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = horizontalPadding),
            content = content,
        )
    }
}

/**
 * 给「二级页」限宽。
 *
 * 二级页（使用说明 / 关于 / 积分任务 / 扫码）自带顶栏与滚动逻辑，
 * 这里只做居中限宽并保持整屏尺寸，不动它们各自的内容结构。
 */
@Composable
internal fun PageWidthBox(
    maxWidth: Dp = PageWidth.Regular,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize(),
        ) {
            content()
        }
    }
}
