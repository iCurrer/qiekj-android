package ltd.fky.devicecontrol.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ltd.fky.devicecontrol.ui.theme.TerminalDivider
import ltd.fky.devicecontrol.ui.theme.TerminalDotGreen
import ltd.fky.devicecontrol.ui.theme.TerminalDotRed
import ltd.fky.devicecontrol.ui.theme.TerminalDotYellow
import ltd.fky.devicecontrol.ui.theme.TerminalHint
import ltd.fky.devicecontrol.ui.theme.TerminalSurface
import ltd.fky.devicecontrol.ui.theme.TerminalText

/**
 * 终端风格日志窗。
 *
 * 固定使用暖深色底 + 暖白等宽文字，不随明暗主题切换，
 * 以保证日志阅读的一致性。新日志到达时自动滚动到底部。
 */
@Composable
internal fun TerminalLogWindow(
    lines: List<String>,
    modifier: Modifier = Modifier,
    emptyHint: String = "等待执行任务…",
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TerminalSurface),
        border = BorderStroke(1.dp, TerminalDivider),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TerminalDot(TerminalDotRed)
            Spacer(Modifier.width(6.dp))
            TerminalDot(TerminalDotYellow)
            Spacer(Modifier.width(6.dp))
            TerminalDot(TerminalDotGreen)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "task.log",
                color = TerminalHint,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider(color = TerminalDivider)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            if (lines.isEmpty()) {
                item {
                    Text(
                        text = emptyHint,
                        color = TerminalHint,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(lines) { line ->
                    Text(
                        text = "› $line",
                        color = TerminalText,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalDot(color: Color) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(color, CircleShape),
    )
}
