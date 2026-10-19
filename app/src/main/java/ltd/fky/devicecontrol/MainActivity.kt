package ltd.fky.devicecontrol

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.coroutines.cancellation.CancellationException
import ltd.fky.devicecontrol.ui.AppRoute
import ltd.fky.devicecontrol.ui.AppUiState
import ltd.fky.devicecontrol.ui.AppViewModel
import ltd.fky.devicecontrol.ui.DeviceTab
import ltd.fky.devicecontrol.ui.PageWidth
import ltd.fky.devicecontrol.ui.PageWidthBox
import ltd.fky.devicecontrol.ui.WindowWidthClass
import ltd.fky.devicecontrol.ui.component.RunningTaskBar
import ltd.fky.devicecontrol.ui.rememberWindowWidthClass
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
 * - **二级层**：按需进入的页面（积分任务执行页等），不占底部导航，带返回按钮
 *
 * 二级层支持**预测式返回**：从屏幕边缘拖动时页面实时跟手，中途松手会弹回原位。
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
                // 仓库由 Application 持有：全进程唯一，屏幕旋转 / 切换深浅色时
                // 不会重建 OkHttp + Retrofit + Moshi，也不会与 ViewModel 里那份并存
                val app = applicationContext as DeviceControlApplication
                val vm: AppViewModel = viewModel(factory = app.viewModelFactory)
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

    // ---------- 通知权限 ----------
    // 两种长任务（积分任务 / 解锁）都要跑二十分钟量级，结果靠通知送达，
    // 所以第一次进来就把权限要了。用户拒绝也不影响任何流程 ——
    // AppNotifier 在无权限时静默跳过，只是送不到通知。
    val notifier = remember {
        (context.applicationContext as DeviceControlApplication).notifier
    }
    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 拒绝无需处理：所有功能都不依赖通知 */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifier.canNotify()) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---------- 预测式返回 ----------

    /**
     * 二级层的返回进度：0 = 完全就位，1 = 完全退场。
     *
     * 由手势进度直接驱动（`snapTo`），所以页面是**跟手**的；
     * 中途取消则弹回 0；走完则提交并关闭路由。
     */
    val backProgress = remember { Animatable(0f) }

    LaunchedEffect(route) {
        if (route != null) {
            // 进入：从右侧滑入
            backProgress.snapTo(1f)
            backProgress.animateTo(0f, tween(240))
        } else {
            backProgress.snapTo(0f)
        }
    }

    // 兜底：三键导航的返回键 / 键盘返回键**不会**走预测式手势回调，
    // 只靠 PredictiveBackHandler 会导致按返回键时无人接管 → 系统默认行为 → 直接退出 App。
    // 这里补一个普通处理器，保证返回键同样能关掉二级页。
    // 注册顺序 = 优先级反序（同组内后注册者优先），所以它写在 PredictiveBackHandler 之前，
    // 手势返回仍由 PredictiveBackHandler 优先接管（保留跟手动画），按键返回则落到这里。
    BackHandler(enabled = route != null) { vm.closeRoute() }

    PredictiveBackHandler(enabled = route != null) { progress ->
        // 记录事件流是否已推进到提交进度（按键返回可能「提交后立刻取消」）
        var committed = false
        try {
            // 新版 activity 回调给的是 BackEventCompat，进度要从 event.progress 取
            progress.collect { event ->
                backProgress.snapTo(event.progress)
                if (event.progress >= 1f) committed = true
            }
            vm.closeRoute()
        } catch (cancellation: CancellationException) {
            backProgress.animateTo(0f, animationSpec = tween(220))
            // 已推进到 1f 说明是「提交」而非「中途取消」，同样要关闭
            if (committed) vm.closeRoute()
            throw cancellation
        }
    }

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

    val useNavRail = rememberWindowWidthClass() != WindowWidthClass.Compact

    // 宽屏把导航从底部挪到左侧。两个理由：
    // ① 平板横置时底部栏离拇指很远，够不着；
    // ② 底部栏会白吃一整条横向空间，而宽屏恰恰是横向空间最富余的场景。
    Row(modifier = Modifier.fillMaxSize()) {
        if (useNavRail) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Spacer(Modifier.height(12.dp))
                DeviceRailItem(
                    selected = state.currentTab == DeviceTab.Control,
                    onClick = {
                        if (route != null) vm.closeRoute()
                        vm.selectTab(DeviceTab.Control)
                    },
                    icon = Icons.Outlined.Devices,
                    label = "设备",
                )
                DeviceRailItem(
                    selected = state.currentTab == DeviceTab.Me,
                    onClick = {
                        if (route != null) vm.closeRoute()
                        vm.selectTab(DeviceTab.Me)
                    },
                    icon = Icons.Outlined.Person,
                    label = "我的",
                )
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (route == null && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
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
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val progress = backProgress.value
                                // 跟手右移：progress 到 1 时整页移出屏幕，
                                // 露出的正是下面的 Tab 层 —— 返回的目标是可见的
                                translationX = size.width * progress
                                alpha = 1f - progress * 0.25f
                            },
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when (route) {
                            // 二级页按内容类型限宽：长文阅读窄、日志宽、相机取景最窄
                            AppRoute.PointsTask -> PageWidthBox(PageWidth.Wide) {
                                PointsTaskScreen(
                                    state = state,
                                    vm = vm,
                                    onBack = vm::closeRoute,
                                )
                            }

                            AppRoute.Guide -> PageWidthBox(PageWidth.Regular) {
                                GuideScreen(onBack = vm::closeRoute)
                            }

                            AppRoute.About -> PageWidthBox(PageWidth.Regular) {
                                AboutScreen(onBack = vm::closeRoute)
                            }

                            AppRoute.Scan -> PageWidthBox(PageWidth.Narrow) {
                                ScanScreen(
                                    onBack = vm::closeRoute,
                                    onScanned = vm::handleScannedCode,
                                )
                            }
                        }
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

/**
 * 底部导航项。
 *
 * 与宽屏 Rail 项沿用同一套配色令牌，保证窗口跨过 600dp 断点、
 * 导航在「底部 ⇄ 左侧」之间切换时视觉是连续的。
 */
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

/**
 * 宽屏左侧导航项。
 *
 * 与底部导航沿用同一套配色令牌，保证窗口跨过 600dp 断点、
 * 导航在「底部 ⇄ 左侧」之间切换时视觉是连续的。
 */
@Composable
private fun ColumnScope.DeviceRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
