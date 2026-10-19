package ltd.fky.devicecontrol.data

import com.squareup.moshi.Json

data class ApiEnvelope<T>(
    val code: Int? = null,
    val msg: String? = null,
    val message: String? = null,
    val data: T? = null,
) {
    fun requireData(): T = data ?: error(message ?: msg ?: "接口未返回 data")
}

data class EmptyData(
    val ignored: String? = null,
)

data class LoginData(
    val token: String? = null,
)

data class DeviceItem(
    val goodsId: String? = null,
    val goodsName: String = "",
    val id: String? = null,
)

/**
 * 扫码识别结果。
 *
 * 字段是按 H5 侧的用法推断的 —— H5 里写作 `goodsId: deviceInfo.id`，
 * 即服务端返回的 `id` 就是解锁所需的 goodsId。
 * [rawJson] 保留服务端原始返回，便于在调试面板里核对真实字段名。
 */
data class ScanDeviceResult(
    /** 解锁用的 goodsId（取自服务端返回的 `id`）。 */
    val goodsId: String,
    /** 设备名；服务端可能不返回，此时由调用方给个兜底名。 */
    val goodsName: String?,
    val shopId: String?,
    val imei: String?,
    /** 原始响应的 JSON 序列化结果，仅供调试展示。 */
    val rawJson: String,
)

data class BalanceData(
    val tokenCoin: String? = null,
    @param:Json(name = "integral") val integral: String? = null,
    val integralAmount: String? = null,
) {
    val ticketText: String
        get() = tokenCoin?.toDoubleOrNull()?.let { "%.2f".format(it / 100.0) } ?: "-"

    val pointsText: String get() = integral ?: "-"
}

data class SkuData(val skuId: String? = null)

data class ImeiData(
    val imei: String? = null,
)

data class UnlockData(
    val msgId: String? = null,
    val orderNo: String? = null,
)

data class SyncData(
    val workStatus: Int? = null,
    val identify: String? = null,
)

data class AfterPayCreatingData(
    val orderId: String? = null,
)

data class OrderDetailData(
    val tradeOrderItem: List<TradeOrderItem> = emptyList(),
    val promotionList: List<PromotionItem> = emptyList(),
)

data class TradeOrderItem(
    val originPrice: String? = null,
)

data class PromotionItem(
    val promotionType: Int? = null,
    val discountAmount: String? = null,
)

data class UnlockResult(
    val orderNo: String,
    val orderId: String,
    val originPrice: String,
    val ticketCost: String,
    val integralCost: String,
    val otherPromotions: List<PromotionSummary>,
    val completedAt: Long,
)

data class PromotionSummary(
    val promotionType: Int?,
    val discountAmount: String?,
)

data class OrderHistoryItem(
    val orderNo: String,
    val orderId: String,
    val goodsName: String,
    val originPrice: String,
    val ticketCost: String,
    val integralCost: String,
    val otherPromotions: List<PromotionSummary>,
    val completedAt: Long,
    /**
     * 订单状态。`"running"` 表示「服务端已受理解锁、但设备还没跑完」。
     *
     * **必须可空**：这是后加的字段，本机已存的历史记录里没有它。
     * Moshi 对**可空**字段允许 JSON 中缺失（填 null），对**非可空**字段会直接抛
     * `JsonDataException`；而 `OrderHistoryStore.list()` 是
     * `runCatching { ... }.getOrDefault(emptyList())` —— 一旦解析抛异常，
     * 用户已有的整段历史会被静默读成空列表。可空是这里唯一安全的写法。
     */
    val status: String? = null,
    /** 解锁时用的 skuId，留作中断后按订单对账。 */
    val skuId: String? = null,
) {
    /** 是否为「已受理、未结算」的进行中订单。 */
    val isRunning: Boolean get() = status == STATUS_RUNNING

    fun toUnlockResult(): UnlockResult = UnlockResult(
        orderNo = orderNo,
        orderId = orderId,
        originPrice = originPrice,
        ticketCost = ticketCost,
        integralCost = integralCost,
        otherPromotions = otherPromotions,
        completedAt = completedAt,
    )

    companion object {
        /** 已受理、等待设备跑完。 */
        const val STATUS_RUNNING = "running"

        /** 已结算完成；也是历史数据缺失 [status] 时的语义。 */
        const val STATUS_DONE = "done"
    }
}

/**
 * 使用统计。
 *
 * 只统计**已完成**的订单：进行中那条的花费还是未知的（`ticketCost` 为 `"-"`），
 * 计进去会把金额算小，反而失真。
 */
data class UsageStats(
    val count: Int,
    val ticketTotal: Double,
    val originTotal: Double,
) {
    val ticketText: String get() = "%.2f".format(ticketTotal)
    val originText: String get() = "%.2f".format(originTotal)
}

/**
 * 统计 [sinceMillis] 之后完成的订单。
 *
 * 金额在服务端是字符串、且未结算时为 `"-"`，所以统一 `toDoubleOrNull` 兜底成 0，
 * 不对格式做任何假设。
 */
fun List<OrderHistoryItem>.usageStatsSince(sinceMillis: Long): UsageStats {
    val done = filter { !it.isRunning && it.completedAt >= sinceMillis }
    return UsageStats(
        count = done.size,
        ticketTotal = done.sumOf { it.ticketCost.toDoubleOrNull() ?: 0.0 },
        originTotal = done.sumOf { it.originPrice.toDoubleOrNull() ?: 0.0 },
    )
}
