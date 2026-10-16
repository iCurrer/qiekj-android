package ltd.fky.devicecontrol.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ltd.fky.devicecontrol.ui.AppRoute
import ltd.fky.devicecontrol.ui.AppUiState
import ltd.fky.devicecontrol.ui.AppViewModel
import ltd.fky.devicecontrol.ui.DeviceTab
import ltd.fky.devicecontrol.ui.PageContainer
import ltd.fky.devicecontrol.ui.PageWidth
import ltd.fky.devicecontrol.ui.component.BalanceSummaryBar
import ltd.fky.devicecontrol.ui.component.DeviceCard
import ltd.fky.devicecontrol.ui.component.EmptyState
import ltd.fky.devicecontrol.ui.component.LoadingText
import ltd.fky.devicecontrol.ui.component.PageTitle
import ltd.fky.devicecontrol.ui.pinDeviceShortcut

/**
 * 设备控制页（底部导航第一个目的地）。
 *
 * 顶部前置资产速览：解锁要消耗小票，用户需要在选择设备之前
 * 就知道余额够不够，而不是点了解锁才发现不足。
 *
 * 右上角是扫码入口：直接扫设备屏幕上的二维码即可识别并解锁，
 * **不依赖"历史设备"列表**（历史列表只是便利功能）。
 *
 * 列表渲染的是 `deviceRows` —— 服务端数据叠加本地偏好后的结果：
 * 隐藏项已过滤、收藏置顶、别名生效；被隐藏的可在底部入口恢复。
 *
 * 宽屏下设备列表走**自适应网格**：卡片保持接近方形的比例，
 * 而不是被拉伸成贯穿整屏的扁条。
 */
@Composable
internal fun ControlScreen(
    state: AppUiState,
    vm: AppViewModel,
) {
    val context = LocalContext.current

    PageContainer(maxWidth = PageWidth.Grid) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PageTitle(
                    title = "设备控制",
                    subtitle = when {
                        state.unlocking -> state.unlockStatus
                        state.scanning -> "正在识别扫码结果"
                        !state.hasToken -> "未登录"
                        state.deviceRows.isNotEmpty() -> "共 ${state.deviceRows.size} 台设备"
                        else -> null
                    },
                )
            }
            IconButton(
                onClick = { vm.openRoute(AppRoute.Scan) },
                enabled = state.hasToken && !state.scanning && !state.unlocking,
            ) {
                Icon(
                    Icons.Outlined.QrCodeScanner,
                    contentDescription = "扫码解锁",
                    modifier = Modifier.size(24.dp),
                    tint = if (state.hasToken) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!state.hasToken) {
            EmptyState(
                icon = Icons.Outlined.Person,
                text = "登录后即可解锁设备",
                action = {
                    TextButton(onClick = { vm.selectTab(DeviceTab.Me) }) {
                        Text("去登录")
                    }
                },
            )
        } else {
            BalanceSummaryBar(
                ticketText = state.balance?.ticketText,
                pointsText = state.balance?.pointsText,
                loading = state.loadingBalance,
                onRefresh = vm::refreshBalance,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.loadingDevices && state.deviceRows.isEmpty() ->
                    LoadingText("正在查询历史设备")

                state.deviceRows.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Devices,
                    text = if (state.hiddenDeviceRows.isEmpty()) {
                        "暂无设备，可点右上角扫码"
                    } else {
                        "设备都已隐藏"
                    },
                )

                else -> LazyVerticalGrid(
                    // 手机竖屏只够一列（340dp 是卡片的舒适下限），
                    // 平板会自动排到两列，卡片不再被拉扁
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = state.deviceRows, key = { it.key }) { row ->
                        DeviceCard(
                            name = row.displayName,
                            favorite = row.favorite,
                            enabled = !state.unlocking,
                            onClick = { vm.unlock(row.device) },
                            onAddShortcut = { pinDeviceShortcut(context, row.device) },
                            onMenu = { vm.openDeviceMenu(row.key) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            if (state.hiddenDeviceRows.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                HiddenEntry(
                    count = state.hiddenDeviceRows.size,
                    onClick = vm::openHiddenManager,
                )
            }
        }
    }
}

/** 底部「已隐藏 N 台 · 管理」入口。 */
@Composable
private fun HiddenEntry(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onClick) {
            Icon(
                Icons.Outlined.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("已隐藏 $count 台 · 管理")
        }
    }
}
