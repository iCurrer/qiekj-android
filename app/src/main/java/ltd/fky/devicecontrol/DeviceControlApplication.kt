package ltd.fky.devicecontrol

import android.app.Application
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ltd.fky.devicecontrol.data.AppRepository
import ltd.fky.devicecontrol.data.DeviceCacheStore
import ltd.fky.devicecontrol.data.DevicePrefsStore
import ltd.fky.devicecontrol.data.OrderHistoryStore
import ltd.fky.devicecontrol.data.TokenStore
import ltd.fky.devicecontrol.notification.AppNotifier
import ltd.fky.devicecontrol.ui.AppViewModelFactory
import ltd.fky.devicecontrol.widget.DeviceGlanceWidget

/**
 * 应用级容器：持有全进程唯一的 [repository]。
 *
 * **为什么必须是单例**，而不放在 Activity 的 `remember` 里：
 *
 * 1. `OkHttpClient` / `Retrofit` / `Moshi` 都是重量级对象。屏幕旋转、切换深浅色、
 *    进出分屏都会让 Activity 重建，`remember` 随之重算 —— 网络栈被反复重建。
 * 2. 那个时机还会产生**两份并存**：`viewModel()` 在配置变更时返回的是**已有 ViewModel**
 *    （内部仍持有旧仓库），刚新建的仓库无人使用却已占住一份连接池。
 * 3. 桌面卡片（Glance）与 AppFunctions（Android MCP）服务各自也需要仓库，
 *    它们与 Activity 生命周期无关。各建一份就会出现「同一账号两套缓存」的数据分裂。
 *
 * 放在 Application 后，上述三处共用同一个实例。
 */
class DeviceControlApplication : Application() {

    /**
     * 应用级协程作用域。
     *
     * 只用于「不需要随任何界面存亡」的轻量任务（当前是预热），
     * 用 `SupervisorJob` 保证单个任务失败不会拖垮其他任务。
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: AppRepository by lazy {
        AppRepository(
            tokenStore = TokenStore(this),
            orderHistoryStore = OrderHistoryStore(this),
            deviceCacheStore = DeviceCacheStore(this),
            devicePrefsStore = DevicePrefsStore(this),
            // 设备数据变化后刷新桌面卡片，让卡片与 App 数据保持一致
            onDevicesChanged = { DeviceGlanceWidget().updateAll(this) },
        )
    }

    /** 长任务结果通知。渠道的创建与权限判定都在 [AppNotifier] 内部。 */
    val notifier: AppNotifier by lazy { AppNotifier(this) }

    val viewModelFactory: AppViewModelFactory by lazy {
        AppViewModelFactory(repository, notifier)
    }

    override fun onCreate() {
        super.onCreate()
        // 通知渠道要在第一次发通知之前建好 —— Android 8 起，没有归属渠道的通知发不出去
        runCatching { notifier.ensureChannel() }
        // 后台预热加密存储：`EncryptedSharedPreferences` 首次创建要走 AndroidKeyStore
        // （生成 / 读取 AES 密钥），耗时可达几十到几百毫秒。放在这里异步做，
        // 别让它在冷启动的首帧路径上被撞上。
        applicationScope.launch { runCatching { repository.warmUp() } }
    }
}
