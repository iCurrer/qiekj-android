package ltd.fky.devicecontrol.appfunctions

import android.os.Build
import android.webkit.WebSettings
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.fky.devicecontrol.DeviceControlApplication
import ltd.fky.devicecontrol.data.AppRepository
import ltd.fky.devicecontrol.data.PointsTaskRunner

/**
 * 「小乖生活」对外暴露的 AppFunctions（Android MCP 工具）。
 *
 * 该抽象类把 App 的核心能力声明为 AI 助手可发现、可调用的工具；
 * KSP 会依据本类在编译期生成具体 Service 子类与对应的 XML schema。
 * 每个函数的 KDoc 都会被编译进工具描述，供模型理解用途与参数。
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "XiaoGuaiAppFunctionService",
    appFunctionXmlFileName = "xiaoguai_app_functions",
)
abstract class BaseXiaoGuaiAppFunctionService : AppFunctionService() {

    /**
     * 复用 Application 持有的全进程唯一仓库。
     *
     * 本服务与 Activity 生命周期无关；若各自新建一份，就会出现
     * 「同一个账号、两套缓存、两份连接池」—— token 与设备快照都可能不同步。
     */
    private val repository: AppRepository
        get() = (applicationContext as DeviceControlApplication).repository

    /**
     * 列出当前账号最近使用过的共享设备及其名称。
     * 解锁任何设备前，先调用本函数取得准确的设备名称。
     *
     * @return 历史设备列表；未登录或没有历史记录时返回空列表。
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listDevices(): List<AppFunctionDevice> = withContext(Dispatchers.IO) {
        repository.latestDevices().map { device ->
            AppFunctionDevice(name = device.goodsName.ifBlank { "未命名设备" })
        }
    }

    /**
     * 解锁指定名称的共享设备，并等待本次使用结束后返回订单结算信息。
     * 该操作会产生真实订单与扣费，且不可撤销；调用前必须先获得用户明确确认。
     *
     * Required workflow: 先调用 listDevices 取得有效设备名称。
     *
     * @param deviceName 要解锁的设备名称，必须取自 listDevices 的返回值。
     * @return 解锁结果，包含订单号、原价与实际扣费；失败时 errorMessage 说明原因。
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun unlockDevice(deviceName: String): AppFunctionUnlockResult =
        withContext(Dispatchers.IO) {
            val target = repository.latestDevices()
                .firstOrNull { it.goodsName == deviceName }
                ?: return@withContext AppFunctionUnlockResult(
                    success = false,
                    deviceName = deviceName,
                    errorMessage = "未找到名为「$deviceName」的设备，请先调用 listDevices 确认名称。",
                )

            runCatching { repository.unlockDevice(target) {} }.fold(
                onSuccess = { result ->
                    AppFunctionUnlockResult(
                        success = true,
                        deviceName = deviceName,
                        orderNo = result.orderNo,
                        originPrice = result.originPrice,
                        ticketCost = result.ticketCost,
                        pointsDeduction = result.integralCost,
                    )
                },
                onFailure = { error ->
                    AppFunctionUnlockResult(
                        success = false,
                        deviceName = deviceName,
                        errorMessage = error.message ?: "解锁失败",
                    )
                },
            )
        }

    /**
     * 查询当前账号的资产：小票余额、积分余额与积分可抵扣金额。
     *
     * @return 资产信息；未登录或请求失败时 success 为 false，errorMessage 说明原因。
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun queryBalance(): AppFunctionBalance = withContext(Dispatchers.IO) {
        runCatching { repository.queryBalance() }.fold(
            onSuccess = { balance ->
                AppFunctionBalance(
                    success = true,
                    ticketBalance = balance.ticketText,
                    pointsBalance = balance.pointsText,
                    pointsDeduction = balance.integralAmount,
                )
            },
            onFailure = { error ->
                AppFunctionBalance(
                    success = false,
                    errorMessage = error.message ?: "查询资产失败",
                )
            },
        )
    }

    /**
     * 执行平台上的每日积分任务（自动签到与领取积分），并返回执行结果摘要。
     * 该操作只影响当前账号的积分，不产生任何费用。
     *
     * @return 任务是否成功以及一段可直接转述的结果摘要。
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun runPointsTask(): AppFunctionTaskResult = withContext(Dispatchers.IO) {
        val userAgent = WebSettings.getDefaultUserAgent(applicationContext)
        val logs = mutableListOf<String>()
        val runner = PointsTaskRunner { repository.localToken() }
        runCatching { runner.run(userAgent) { line -> logs += line } }.fold(
            onSuccess = {
                AppFunctionTaskResult(
                    success = true,
                    summary = logs.takeLast(3).joinToString("；").ifBlank { "积分任务已完成" },
                )
            },
            onFailure = { error ->
                AppFunctionTaskResult(
                    success = false,
                    summary = error.message ?: "积分任务执行失败",
                )
            },
        )
    }
}
