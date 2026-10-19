package ltd.fky.devicecontrol.ui.screen

import android.webkit.WebSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ltd.fky.devicecontrol.ui.AppUiState
import ltd.fky.devicecontrol.ui.AppViewModel
import ltd.fky.devicecontrol.ui.component.DetailTopBar
import ltd.fky.devicecontrol.ui.component.TerminalLogWindow

/**
 * 积分任务执行页（二级页）。
 *
 * 从「我的」页的积分任务卡进入，不占底部导航。
 * 执行按钮与终端日志窗保持原有行为；执行期间可随时返回，
 * 任务在后台继续，顶部状态条会持续提示。
 */
@Composable
internal fun PointsTaskScreen(
    state: AppUiState,
    vm: AppViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    // 日志单独订阅：任务执行时它高频刷新，不该牵动本页其余部分（按钮、顶栏）一起重组
    val logs by vm.pointsLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        DetailTopBar(
            title = "积分任务",
            subtitle = if (state.runningPointsTask) "任务执行中…" else "自动执行每日积分任务",
            onBack = onBack,
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val userAgent = WebSettings.getDefaultUserAgent(context)
                vm.startPointsTask(userAgent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !state.runningPointsTask && state.hasToken,
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.runningPointsTask) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(if (state.runningPointsTask) "任务执行中" else "开始执行自动化任务")
        }

        Spacer(Modifier.height(16.dp))

        TerminalLogWindow(
            lines = logs,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(Modifier.height(16.dp))
    }
}
