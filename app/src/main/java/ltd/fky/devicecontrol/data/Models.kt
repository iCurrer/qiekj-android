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
) {
    fun toUnlockResult(): UnlockResult = UnlockResult(
        orderNo = orderNo,
        orderId = orderId,
        originPrice = originPrice,
        ticketCost = ticketCost,
        integralCost = integralCost,
        otherPromotions = otherPromotions,
        completedAt = completedAt,
    )
}
