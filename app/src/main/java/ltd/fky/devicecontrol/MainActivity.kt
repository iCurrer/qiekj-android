package ltd.fky.devicecontrol

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewmodel.compose.viewModel
import ltd.fky.devicecontrol.data.AppRepository
import ltd.fky.devicecontrol.data.DeviceCacheStore
import ltd.fky.devicecontrol.data.DevicePrefsStore
import ltd.fky.devicecontrol.data.OrderHistoryStore
import ltd.fky.devicecontrol.data.TokenStore
import ltd.fky.devicecontrol.widget.DeviceGlanceWidget
import ltd.fky.devicecontrol.ui.AppRoute
import ltd.fky.devicecontrol.ui.AppUiState
import ltd.fky.devicecontrol.ui.AppViewModel
import ltd.fky.devicecontrol.ui.AppViewModelFactory
import ltd.fky.devicecontrol.ui.DeviceTab
import ltd.fky.devicecontrol.ui.component.RunningTaskBar
import ltd.fky.devicecontrol.ui.dialog.ConfirmDialog
import ltd.fky.devicecontrol.ui.dialog.DeviceActionDialog
import ltd.fky.devicecontrol.ui.dialog.HiddenDevicesDialog
import ltd.fky.devicecontrol.ui.dialog.OrderDetailDialog
import ltd.fky.devicecontrol.ui.dialog.OrderHistoryDialog
import ltd.fky.devicecontrol.ui.dialog.RenameDeviceDialog
import ltd.fky.devicecontrol.ui.dialog.TokenDialog
import ltd.fky.devicecontrol.ui.screen.AboutScreen
import ltd.fky.devicecontrol.ui.screen.ControlScreen
import ltd.fky.devicecontrol.ui.screen.GuideScreen
import ltd.fky.devicecontrol.ui.screen.MeScreen
import ltd.fky.devicecontrol.ui.screen.PointsTaskScreen
import ltd.fky.devicecontrol.ui.screen.ScanScreen
import ltd.fky.devicecontrol.ui.shortcutRequestFromIntent
import ltd.fky.devicecontrol.ui.theme.XiaoGuaiTheme

/**
 * 应用入口。
 *
 * 界面层级只有两层：
 * - **Tab 层**：底部导航的两个目的地（设备 / 我的）
 * - **二级层**：按需进入的页面（积分任务执行页等），不占底部导航，
 *   带返回按钮并响应系统返回键
 *
 * 本文件只负责：主题装配、依赖构建、Intent 短路、导航壳与全局对话框。
 * 页面实现在 `ui/screen/`，通用组件在 `ui/component/`，对话框在 `ui/dialog/`。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XiaoGuaiTheme {
                val appContext = applicationContext
                val repository = remember {
                    AppRepository(
                        tokenStore = TokenStore(appContext),
                        orderHistoryStore = OrderHistoryStore(appContext),
                        deviceCacheStore = DeviceCacheStore(appContext),
                        devicePrefsStore = DevicePrefsStore(appContext),
                        // 设备数据变化后刷新桌面卡片，让卡片与 App 数据保持一致
                        onDevicesChanged = { DeviceGlanceWidget().updateAll(appContext) },
                    )
                }
                val vm: AppViewModel = viewModel(
                    factory = AppViewModelFactory(repository),
                )
                LaunchedEffect(vm) {
                    shortcutRequestFromIntent(intent)?.let(vm::openDeviceShortcut)
                }
                DeviceControlApp(vm)
            }
        }
    }
}

@Composable
private fun DeviceControlApp(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val route = state.currentRoute

    LaunchedEffect(state.toastMessage) {
        val message = state.toastMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        vm.consumeToast()
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        vm.consumeError()
    }

    state.tokenDialogText?.let { token ->
        TokenDialog(token = token, onDismiss = vm::dismissCurrentToken)
    }

    if (state.showOrderHistory) {
        OrderHistoryDialog(
            orders = state.orderHistory,
            onDismiss = vm::dismissOrderHistory,
            onOpenOrder = vm::showHistoricalOrder,
        )
    }

    state.orderDetail?.let { detail ->
        OrderDetailDialog(
            detail = detail,
            onDismiss = vm::dismissOrderDetail,
        )
    }

    if (state.showLogoutConfirm) {
        ConfirmDialog(
            title = "退出登录",
            message = "退出后需要重新用手机号登录，本机历史订单会保留。",
            confirmText = "退出登录",
            onConfirm = vm::confirmLogout,
            onDismiss = vm::dismissLogoutConfirm,
        )
    }

    // 桌面入口（快捷方式 / 桌面卡片）点选设备后，回到 App 内二次确认才真正解锁
    state.pendingUnlockDevice?.let { device ->
        ConfirmDialog(
            title = "确认解锁",
            message = "即将解锁「${device.goodsName.ifBlank { "未命名设备" }}」。\n\n" +
                "解锁会产生真实订单并扣减小票，设备启动后将持续运行至使用结束。",
            confirmText = "解锁",
            onConfirm = vm::confirmPendingUnlock,
            onDismiss = vm::dismissPendingUnlock,
        )
    }

    // 扫码识别成功后的确认：同样是真实扣费，不能"扫到就执行"
    state.scanResult?.let { result ->
        ConfirmDialog(
            title = "识别到设备",
            message = "设备：${result.goodsName?.takeIf { it.isNotBlank() } ?: "未命名设备"}\n" +
                "编号：${result.goodsId}\n\n" +
                "解锁会产生真实订单并扣减小票，设备启动后将持续运行至使用结束。",
            confirmText = "解锁",
            onConfirm = vm::confirmScanUnlock,
            onDismiss = vm::dismissScanResult,
        )
    }

    // ---------- 设备列表自定义 ----------

    /** 按 key 在当前列表中找一行（隐藏项也参与查找）。 */
    fun rowsLookup(key: String) = state.deviceRows.firstOrNull { it.key == key }
        ?: state.hiddenDeviceRows.firstOrNull { it.key == key }

    state.menuDeviceKey?.let { key ->
        rowsLookup(key)?.let { row ->
            DeviceActionDialog(
                title = row.displayName,
                favorite = row.favorite,
                onRename = { vm.startRenameDevice(key) },
                onToggleFavorite = { vm.toggleFavorite(key) },
                onHide = { vm.setDeviceHidden(key, true) },
                onDismiss = vm::closeDeviceMenu,
            )
        }
    }

    state.renamingDeviceKey?.let { key ->
        rowsLookup(key)?.let { row ->
            RenameDeviceDialog(
                initial = row.displayName,
                onConfirm = { alias -> vm.renameDevice(key, alias) },
                onDismiss = vm::cancelRenameDevice,
            )
        }
    }

    if (state.showHiddenManager) {
        HiddenDevicesDialog(
            rows = state.hiddenDeviceRows,
            onRestore = { key -> vm.setDeviceHidden(key, false) },
            onDismiss = vm::closeHiddenManager,
        )
    }

    BackHandler(enabled = route != null) { vm.closeRoute() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (route == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    DeviceNavItem(
                        selected = state.currentTab == DeviceTab.Control,
                        onClick = { vm.selectTab(DeviceTab.Control) },
                        icon = Icons.Outlined.Devices,
                        label = "设备",
                    )
                    DeviceNavItem(
                        selected = state.currentTab == DeviceTab.Me,
                        onClick = { vm.selectTab(DeviceTab.Me) },
                        icon = Icons.Outlined.Person,
                        label = "我的",
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabLayer(state = state, vm = vm)

            if (route != null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (route) {
                        AppRoute.PointsTask -> PointsTaskScreen(
                            state = state,
                            vm = vm,
                            onBack = vm::closeRoute,
                        )

                        AppRoute.Guide -> GuideScreen(onBack = vm::closeRoute)
                        AppRoute.About -> AboutScreen(onBack = vm::closeRoute)

                        AppRoute.Scan -> ScanScreen(
                            onBack = vm::closeRoute,
                            onScanned = vm::handleScannedCode,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 层内容。
 *
 * 执行中状态条挂在两个目的地之上：积分任务会跑很久，
 * 期间用户切到别的 Tab 也要能看见「任务还在跑」并一键回去。
 */
@Composable
private fun TabLayer(
    state: AppUiState,
    vm: AppViewModel,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.runningPointsTask) {
            RunningTaskBar(
                onClick = { vm.openRoute(AppRoute.PointsTask) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        AnimatedContent(
            targetState = state.currentTab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val offset = if (forward) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 * offset })
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "tab-content",
            modifier = Modifier.weight(1f),
        ) { tab ->
            when (tab) {
                DeviceTab.Control -> ControlScreen(state, vm)
                DeviceTab.Me -> MeScreen(state, vm)
            }
        }
    }
}

@Composable
private fun RowScope.DeviceNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
