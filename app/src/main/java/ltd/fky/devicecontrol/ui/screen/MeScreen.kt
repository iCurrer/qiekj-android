package ltd.fky.devicecontrol.ui.screen

import android.webkit.WebSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ltd.fky.devicecontrol.ui.AppRoute
import ltd.fky.devicecontrol.ui.AppUiState
import ltd.fky.devicecontrol.ui.AppViewModel
import ltd.fky.devicecontrol.ui.component.ActionRow
import ltd.fky.devicecontrol.ui.component.BalanceRow
import ltd.fky.devicecontrol.ui.component.PageTitle
import ltd.fky.devicecontrol.ui.component.PointsTaskCard
import ltd.fky.devicecontrol.ui.component.SectionCard
import ltd.fky.devicecontrol.ui.openProjectHome

/**
 * 我的页（底部导航第二个目的地，账号域）。
 *
 * 承载所有以「账号」为粒度的内容：登录、积分任务、订单、资产。
 * 积分任务从原来的独立 Tab 下沉到这里 —— 它与订单、资产同级，
 * 不该占用一个常驻的底部入口。
 */
@Composable
internal fun MeScreen(
    state: AppUiState,
    vm: AppViewModel,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageTitle("我的", if (state.hasToken) null else "未登录")
            IconButton(onClick = { openProjectHome(context) }) {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = "打开项目主页",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        if (state.hasToken) {
            AccountCard(
                accountLabel = accountLabel(state.phone),
                onLogout = vm::requestLogout,
            )
        } else {
            LoginCard(state = state, vm = vm)
        }

        Spacer(Modifier.height(12.dp))

        PointsTaskCard(
            loggedIn = state.hasToken,
            running = state.runningPointsTask,
            statusText = when {
                !state.hasToken -> "登录后可执行"
                state.runningPointsTask -> "任务执行中，可切换到其他页面"
                state.pointsTaskLastResult != null -> state.pointsTaskLastResult
                else -> "自动执行每日积分任务"
            },
            onAction = {
                if (!state.runningPointsTask) {
                    val userAgent = WebSettings.getDefaultUserAgent(context)
                    vm.startPointsTask(userAgent)
                }
                vm.openRoute(AppRoute.PointsTask)
            },
        )

        Spacer(Modifier.height(12.dp))

        SectionCard(padding = 0.dp) {
            ActionRow(
                icon = Icons.Outlined.Key,
                title = "查看当前 Token",
                onClick = vm::showCurrentToken,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ActionRow(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = "历史订单",
                onClick = vm::showOrderHistory,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 帮助与信息
        SectionCard(padding = 0.dp) {
            ActionRow(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = "使用说明",
                onClick = { vm.openRoute(AppRoute.Guide) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ActionRow(
                icon = Icons.Outlined.Info,
                title = "关于",
                onClick = { vm.openRoute(AppRoute.About) },
            )
        }

        Spacer(Modifier.height(12.dp))

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("资产", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = vm::refreshBalance,
                    enabled = state.hasToken && !state.loadingBalance,
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("刷新")
                }
            }
            Spacer(Modifier.height(4.dp))
            when {
                !state.hasToken -> Text(
                    "登录后展示资产信息",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.loadingBalance -> Text(
                    "正在查询资产…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                state.balance != null -> {
                    BalanceRow("小票", state.balance.ticketText)
                    BalanceRow("积分", state.balance.pointsText)
                    BalanceRow("抵扣金额", state.balance.integralAmount ?: "-")
                }

                else -> Text(
                    "暂无资产信息",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/**
 * 已登录时的账号卡。
 *
 * 折叠为一行：登录表单只应在「需要登录」时出现，
 * 已登录还摆着手机号/验证码输入框，只会让人以为要重新登录。
 */
@Composable
private fun AccountCard(
    accountLabel: String,
    onLogout: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("账号", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    accountLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onLogout) { Text("退出登录") }
        }
    }
}

/** 未登录时的登录表单。 */
@Composable
private fun LoginCard(
    state: AppUiState,
    vm: AppViewModel,
) {
    SectionCard {
        Text("账号登录", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = vm::updatePhone,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("手机号") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.code,
                onValueChange = vm::updateCode,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("验证码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.width(10.dp))
            FilledTonalButton(
                onClick = vm::sendCode,
                enabled = !state.sendingCode,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.height(56.dp),
            ) {
                Text(if (state.sendingCode) "发送中" else "获取验证码")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = vm::login,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !state.loggingIn,
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(if (state.loggingIn) "登录中" else "确认登录")
        }
    }
}

/** 手机号掩码：本次会话拿得到原号码就显示掩码，否则退化为「已登录」。 */
private fun accountLabel(phone: String): String =
    if (phone.length >= 7) "${phone.take(3)}****${phone.takeLast(4)}" else "已登录"
