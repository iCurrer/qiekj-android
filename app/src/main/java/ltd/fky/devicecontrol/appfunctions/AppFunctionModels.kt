package ltd.fky.devicecontrol.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * 一台可供解锁的共享设备，由 listDevices 返回。
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionDevice(
    /** 设备名称。解锁设备时必须把该名称原样传回 unlockDevice。 */
    val name: String,
)

/**
 * 一次设备解锁的结果，包含订单与扣费信息。
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionUnlockResult(
    /** 解锁是否成功。 */
    val success: Boolean,
    /** 本次操作的设备名称。 */
    val deviceName: String,
    /** 订单号；失败时为空。 */
    val orderNo: String? = null,
    /** 订单原价；失败时为空。 */
    val originPrice: String? = null,
    /** 实际扣除的小票金额；失败时为空。 */
    val ticketCost: String? = null,
    /** 积分抵扣金额；失败时为空。 */
    val pointsDeduction: String? = null,
    /** 失败原因；成功时为空。 */
    val errorMessage: String? = null,
)

/**
 * 账号当前资产：小票余额、积分余额与积分可抵扣金额。
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionBalance(
    /** 查询是否成功。 */
    val success: Boolean,
    /** 小票余额，单位为元。 */
    val ticketBalance: String? = null,
    /** 积分余额。 */
    val pointsBalance: String? = null,
    /** 积分可抵扣的金额。 */
    val pointsDeduction: String? = null,
    /** 失败原因；成功时为空。 */
    val errorMessage: String? = null,
)

/**
 * 积分任务的执行结果。
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionTaskResult(
    /** 任务是否执行成功。 */
    val success: Boolean,
    /** 执行结果摘要，可直接转述给用户。 */
    val summary: String,
)
