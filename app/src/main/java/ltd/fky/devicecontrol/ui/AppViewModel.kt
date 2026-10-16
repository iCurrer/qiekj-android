package ltd.fky.devicecontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ltd.fky.devicecontrol.data.AppRepository
import ltd.fky.devicecontrol.data.BalanceData
import ltd.fky.devicecontrol.data.DeviceItem
import ltd.fky.devicecontrol.data.DevicePref
import ltd.fky.devicecontrol.data.OrderHistoryItem
import ltd.fky.devicecontrol.data.prefKey
import ltd.fky.devicecontrol.data.PointsTaskRunner
import ltd.fky.devicecontrol.data.ScanDeviceResult
import ltd.fky.devicecontrol.data.UnlockResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 底部导航目的地。只保留两个必用项。 */
enum class DeviceTab { Control, Me }

/**
 * 二级页目的地。
 *
 * 二级页不占底部导航，按需从 Tab 内进入，带返回按钮与系统返回键支持。
 * 积分任务这类「账号级的低频操作」属于这里，而不是与「设备解锁」平级占一个 Tab。
 */
sealed interface AppRoute {
    /** 积分任务执行页：执行按钮 + 终端日志窗。 */
    data object PointsTask : AppRoute

    /** 使用说明页：从登录到解锁的完整流程。 */
    data object Guide : AppRoute

    /** 关于页：应用信息、技术栈、项目链接与免责声明。 */
    data object About : AppRoute

    /** 扫码解锁页：相机取景识别设备二维码。 */
    data object Scan : AppRoute
}

data class DeviceShortcutRequest(
    val goodsId: String?,
    val id: String?,
    val goodsName: String?,
)

/** 设备列表的一行：服务端设备叠加本地偏好后的展示信息。 */
data class DeviceRow(
    /** 本地偏好的 key，见 `DeviceItem.prefKey`。 */
    val key: String,
    val device: DeviceItem,
    /** 别名优先，其次服务端返回的名字。 */
    val displayName: String,
    val favorite: Boolean,
)

data class AppUiState(
    val currentTab: DeviceTab = DeviceTab.Control,
    val currentRoute: AppRoute? = null,
    val hasToken: Boolean = false,
    val phone: String = "",
    val code: String = "",
    val sendingCode: Boolean = false,
    val loggingIn: Boolean = false,
    val loadingDevices: Boolean = false,
    val loadingBalance: Boolean = false,
    val unlocking: Boolean = false,
    val runningPointsTask: Boolean = false,
    val pointsLogs: List<String> = emptyList(),
    val pointsTaskLastResult: String? = null,
    /** 正在用扫码结果换取设备信息。 */
    val scanning: Boolean = false,
    /** 扫码识别出的设备，等待用户确认后解锁。 */
    val scanResult: ScanDeviceResult? = null,
    val devices: List<DeviceItem> = emptyList(),
    /** 设备本地偏好，key 为 [DeviceItem.prefKey]。 */
    val devicePrefs: Map<String, DevicePref> = emptyMap(),
    /** 应用偏好后的展示列表：隐藏项已过滤、收藏置顶、别名生效。 */
    val deviceRows: List<DeviceRow> = emptyList(),
    /** 被隐藏的设备，供「管理」入口恢复。 */
    val hiddenDeviceRows: List<DeviceRow> = emptyList(),
    val balance: BalanceData? = null,
    val orderHistory: List<OrderHistoryItem> = emptyList(),
    val unlockStatus: String? = null,
    val orderDetail: UnlockResult? = null,
    /** 从桌面入口（快捷方式 / 桌面卡片）点选、等待用户二次确认解锁的设备。 */
    val pendingUnlockDevice: DeviceItem? = null,
    val showOrderHistory: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    /** 正在打开操作菜单的设备 key。 */
    val menuDeviceKey: String? = null,
    /** 正在重命名的设备 key。 */
    val renamingDeviceKey: String? = null,
    /** 是否显示「已隐藏设备」管理面板。 */
    val showHiddenManager: Boolean = false,
    val tokenDialogText: String? = null,
    val toastMessage: String? = null,
    val errorMessage: String? = null,
)

class AppViewModel(
    private val repository: AppRepository,
) : ViewModel() {
    private val pointsTaskRunner = PointsTaskRunner { repository.localToken() }
    private var pendingShortcutRequest: DeviceShortcutRequest? = null
    private val _state = MutableStateFlow(
        AppUiState(
            hasToken = repository.localToken() != null,
            orderHistory = repository.orderHistory(),
        ),
    )
    val state: StateFlow<AppUiState> = _state

    init {
        _state.update { it.copy(devicePrefs = repository.devicePrefs()) }
        if (repository.localToken() != null) {
            refreshDevices()
            refreshBalance()
        }
    }

    fun selectTab(tab: DeviceTab) {
        _state.update { it.copy(currentTab = tab) }
        if (tab == DeviceTab.Control && state.value.hasToken) {
            if (state.value.devices.isEmpty()) refreshDevices()
            if (state.value.balance == null) refreshBalance()
        }
    }

    /** 进入二级页。 */
    fun openRoute(route: AppRoute) {
        _state.update { it.copy(currentRoute = route) }
    }

    /** 关闭二级页，回到进入前的 Tab。 */
    fun closeRoute() {
        _state.update { it.copy(currentRoute = null) }
    }

    fun updatePhone(value: String) {
        _state.update { it.copy(phone = value) }
    }

    fun updateCode(value: String) {
        _state.update { it.copy(code = value) }
    }

    fun sendCode() = viewModelScope.launch {
        val phone = state.value.phone.trim()
        if (phone.isBlank()) {
            showError("请输入手机号")
            return@launch
        }
        runCatching {
            _state.update { it.copy(sendingCode = true) }
            repository.sendCode(phone)
        }.onSuccess {
            showToast("验证码已发送")
        }.onFailure {
            showError(it.message ?: "验证码发送失败")
        }
        _state.update { it.copy(sendingCode = false) }
    }

    fun login() = viewModelScope.launch {
        val phone = state.value.phone.trim()
        val code = state.value.code.trim()
        if (phone.isBlank() || code.isBlank()) {
            showError("请输入手机号和验证码")
            return@launch
        }
        runCatching {
            _state.update { it.copy(loggingIn = true) }
            repository.login(phone, code)
        }.onSuccess {
            _state.update { it.copy(hasToken = true, loggingIn = false, code = "") }
            showToast("登录成功")
            refreshBalance()
            refreshDevices()
        }.onFailure {
            _state.update { it.copy(loggingIn = false) }
            showError(it.message ?: "登录失败")
        }
    }

    fun requestLogout() {
        _state.update { it.copy(showLogoutConfirm = true) }
    }

    fun dismissLogoutConfirm() {
        _state.update { it.copy(showLogoutConfirm = false) }
    }

    /**
     * 确认退出登录。
     *
     * 清空本地 token 与所有依赖登录的数据，并停留在「我的」页，
     * 方便用户立即换账号登录。
     */
    fun confirmLogout() {
        viewModelScope.launch {
            repository.logout()
            pendingShortcutRequest = null
            _state.update {
                AppUiState(
                    currentTab = DeviceTab.Me,
                    hasToken = false,
                    orderHistory = it.orderHistory,
                )
            }
            showToast("已退出登录")
        }
    }

    fun refreshDevices() = viewModelScope.launch {
        if (!state.value.hasToken) return@launch
        runCatching {
            _state.update { it.copy(loadingDevices = true) }
            repository.latestDevices()
        }.onSuccess { devices ->
            _state.update { it.copy(devices = devices, loadingDevices = false) }
            rebuildDeviceRows()
            consumePendingShortcut(devices)
        }.onFailure {
            _state.update { it.copy(loadingDevices = false) }
            showError(it.message ?: "查询历史设备失败")
        }
    }

    fun openDeviceShortcut(request: DeviceShortcutRequest) {
        pendingShortcutRequest = request
        _state.update { it.copy(currentTab = DeviceTab.Control, currentRoute = null) }
        if (!state.value.hasToken) {
            showError("请先登录后再使用桌面设备快捷方式")
            return
        }
        val devices = state.value.devices
        if (devices.isEmpty()) {
            refreshDevices()
        } else {
            consumePendingShortcut(devices)
        }
    }

    private fun consumePendingShortcut(devices: List<DeviceItem>) {
        val request = pendingShortcutRequest ?: return
        val target = devices.firstOrNull { device ->
            (!request.goodsId.isNullOrBlank() && device.goodsId == request.goodsId) ||
                (!request.id.isNullOrBlank() && device.id == request.id) ||
                (!request.goodsName.isNullOrBlank() && device.goodsName == request.goodsName)
        }
        pendingShortcutRequest = null
        if (target == null) {
            showError("未找到对应的历史设备，请刷新设备列表后重试")
            return
        }
        // 不直接解锁：解锁会阻塞数十分钟并产生真实扣费，必须由用户在 App 内二次确认
        _state.update { it.copy(pendingUnlockDevice = target) }
    }

    /** 确认解锁刚从桌面入口（快捷方式 / 卡片）点选的设备。 */
    fun confirmPendingUnlock() {
        val device = state.value.pendingUnlockDevice ?: return
        _state.update { it.copy(pendingUnlockDevice = null) }
        unlock(device)
    }

    /** 放弃本次解锁。 */
    fun dismissPendingUnlock() {
        _state.update { it.copy(pendingUnlockDevice = null) }
    }

    /**
     * 扫码页回调：用扫到的原始内容换取设备信息。
     *
     * 成功后自动关闭扫码页并弹出确认框 —— 解锁会产生真实扣费，
     * 不能"扫到就执行"。
     */
    fun handleScannedCode(rawCode: String) {
        if (state.value.scanning) return
        viewModelScope.launch {
            runCatching {
                _state.update { it.copy(scanning = true) }
                repository.scanDevice(rawCode)
            }.onSuccess { result ->
                _state.update {
                    it.copy(scanning = false, scanResult = result, currentRoute = null)
                }
            }.onFailure {
                _state.update { it.copy(scanning = false) }
                showError(it.message ?: "扫码识别失败")
            }
        }
    }

    /** 确认解锁扫码识别到的设备。 */
    fun confirmScanUnlock() {
        val result = state.value.scanResult ?: return
        val device = DeviceItem(
            goodsId = result.goodsId,
            goodsName = result.goodsName?.takeIf { it.isNotBlank() } ?: "扫码设备",
            id = result.goodsId,
        )
        _state.update {
            it.copy(
                scanResult = null,
                // 立刻并入内存列表，界面不必等下次刷新
                devices = listOf(device) +
                    it.devices.filterNot { existing -> existing.goodsId == device.goodsId },
            )
        }
        rebuildDeviceRows()
        // 同时并入本地快照，桌面卡片也能看到
        viewModelScope.launch { repository.rememberDevice(device) }
        unlock(device)
    }

    /** 放弃本次扫码结果。 */
    fun dismissScanResult() {
        _state.update { it.copy(scanResult = null) }
    }

    // ---------- 设备列表自定义 ----------

    /** 重新计算展示列表：过滤隐藏、收藏置顶、别名覆盖显示名。 */
    private fun rebuildDeviceRows() {
        val snapshot = _state.value
        val prefs = snapshot.devicePrefs
        val rows = snapshot.devices.map { device ->
            val pref = prefs[device.prefKey] ?: DevicePref()
            DeviceRow(
                key = device.prefKey,
                device = device,
                displayName = pref.alias?.takeIf { it.isNotBlank() }
                    ?: device.goodsName.ifBlank { "未命名设备" },
                favorite = pref.favorite,
            )
        }
        val hidden = rows.filter { prefs[it.key]?.hidden == true }
        val shown = rows
            .filterNot { prefs[it.key]?.hidden == true }
            .sortedByDescending { if (it.favorite) 1 else 0 }
        _state.update { it.copy(deviceRows = shown, hiddenDeviceRows = hidden) }
    }

    fun openDeviceMenu(key: String) {
        _state.update { it.copy(menuDeviceKey = key) }
    }

    fun closeDeviceMenu() {
        _state.update { it.copy(menuDeviceKey = null) }
    }

    fun startRenameDevice(key: String) {
        _state.update { it.copy(menuDeviceKey = null, renamingDeviceKey = key) }
    }

    fun cancelRenameDevice() {
        _state.update { it.copy(renamingDeviceKey = null) }
    }

    /** 保存别名；传空字符串表示恢复服务端原名。 */
    fun renameDevice(key: String, alias: String) {
        updateDevicePref(key) {
            it.copy(alias = alias.trim().takeIf { text -> text.isNotBlank() })
        }
        _state.update { it.copy(renamingDeviceKey = null) }
        showToast("已更新名称")
    }

    fun toggleFavorite(key: String) {
        updateDevicePref(key) { it.copy(favorite = !it.favorite) }
        _state.update { it.copy(menuDeviceKey = null) }
    }

    fun setDeviceHidden(key: String, hidden: Boolean) {
        updateDevicePref(key) { it.copy(hidden = hidden) }
        // 只关操作菜单；管理面板保持打开，方便连续恢复多台
        _state.update { it.copy(menuDeviceKey = null) }
    }

    fun openHiddenManager() {
        _state.update { it.copy(showHiddenManager = true) }
    }

    fun closeHiddenManager() {
        _state.update { it.copy(showHiddenManager = false) }
    }

    /**
     * 更新单台设备的偏好并落盘。
     *
     * 更新后若等同于默认值，就把这条记录删掉，避免存储里堆积无意义的空项。
     */
    private fun updateDevicePref(key: String, transform: (DevicePref) -> DevicePref) {
        val next = _state.value.devicePrefs.toMutableMap()
        val updated = transform(next[key] ?: DevicePref())
        if (updated == DevicePref()) next.remove(key) else next[key] = updated
        repository.saveDevicePrefs(next)
        _state.update { it.copy(devicePrefs = next) }
        rebuildDeviceRows()
    }

    fun refreshBalance() = viewModelScope.launch {
        if (!state.value.hasToken) return@launch
        runCatching {
            _state.update { it.copy(loadingBalance = true) }
            repository.queryBalance()
        }.onSuccess { balance ->
            _state.update { it.copy(balance = balance, loadingBalance = false) }
        }.onFailure {
            _state.update { it.copy(loadingBalance = false) }
            showError(it.message ?: "查询资产失败")
        }
    }

    fun unlock(device: DeviceItem) = viewModelScope.launch {
        if (state.value.unlocking) return@launch
        runCatching {
            _state.update { it.copy(unlocking = true, unlockStatus = "准备解锁") }
            repository.unlockDevice(device) { step ->
                _state.update { it.copy(unlockStatus = step) }
            }
        }.onSuccess { result ->
            _state.update {
                it.copy(
                    unlocking = false,
                    unlockStatus = null,
                    orderDetail = result,
                    orderHistory = repository.orderHistory(),
                )
            }
            showToast("解锁成功！订单原价：${result.originPrice}，花费小票：${result.ticketCost}")
            refreshBalance()
        }.onFailure {
            _state.update { it.copy(unlocking = false, unlockStatus = null) }
            showError(it.message ?: "解锁失败")
        }
    }


    fun startPointsTask(userAgent: String) = viewModelScope.launch {
        if (state.value.runningPointsTask) return@launch
        runCatching {
            _state.update {
                it.copy(
                    runningPointsTask = true,
                    pointsLogs = listOf("准备执行自动化任务"),
                )
            }
            pointsTaskRunner.run(userAgent) { line ->
                appendPointLog(line)
            }
        }.onSuccess {
            appendPointLog("任务流程结束")
            _state.update { it.copy(pointsTaskLastResult = "上次执行：已完成") }
        }.onFailure {
            appendPointLog("任务失败：${it.message ?: "未知错误"}")
            _state.update { it.copy(pointsTaskLastResult = "上次执行：失败") }
        }
        _state.update { it.copy(runningPointsTask = false) }
    }

    private fun appendPointLog(line: String) {
        _state.update { state ->
            val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.CHINA).format(java.util.Date())
            state.copy(pointsLogs = (state.pointsLogs + "[$now] $line").takeLast(500))
        }
    }

    fun showCurrentToken() {
        val token = repository.localToken()?.takeIf { it.isNotBlank() }
        _state.update { it.copy(tokenDialogText = token ?: "当前未登录，暂无 Token") }
    }

    fun dismissCurrentToken() {
        _state.update { it.copy(tokenDialogText = null) }
    }
    fun consumeToast() {
        _state.update { it.copy(toastMessage = null) }
    }

    fun consumeError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun dismissOrderDetail() {
        _state.update { it.copy(orderDetail = null) }
    }

    fun showOrderHistory() {
        _state.update { it.copy(showOrderHistory = true, orderHistory = repository.orderHistory()) }
    }

    fun dismissOrderHistory() {
        _state.update { it.copy(showOrderHistory = false) }
    }

    fun showHistoricalOrder(item: OrderHistoryItem) {
        _state.update { it.copy(orderDetail = item.toUnlockResult(), showOrderHistory = false) }
    }

    private fun showToast(message: String) {
        _state.update { it.copy(toastMessage = message) }
    }

    private fun showError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }
}

class AppViewModelFactory(
    private val repository: AppRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(repository) as T
    }
}
