package ltd.fky.devicecontrol.data

import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface DeviceApi {
    @FormUrlEncoded
    @POST("common/sms/sendCode")
    suspend fun sendCode(
        @Field("phone") phone: String,
        @Field("template") template: String = "reg",
    ): ApiEnvelope<EmptyData>

    @FormUrlEncoded
    @POST("user/reg")
    suspend fun login(
        @Field("channel") channel: String = ApiConfig.LOGIN_CHANNEL,
        @Field("phone") phone: String,
        @Field("verify") verify: String,
    ): ApiEnvelope<LoginData>

    @FormUrlEncoded
    @POST("user/balance")
    suspend fun queryBalance(@Field("token") token: String): ApiEnvelope<BalanceData>

    @FormUrlEncoded
    @POST("goods/latestUsed")
    suspend fun getLatestUsed(
        @Field("categoryCode") categoryCode: String = "5",
        @Field("token") token: String,
    ): ApiEnvelope<List<DeviceItem>>

    /**
     * 扫码识别设备（H5 端同款接口）。
     *
     * 注意：请求体**不是**整段二维码文本，而是从中解析出的字段
     * （`SN` / `IMEI` / `NQT` / `qeykid`），所以这里用 @FieldMap。
     * 对应 H5 的 `scanCodeInit()` —— 它先把二维码内容拆成具体字段，再把结果整份发出。
     *
     * 返回值用宽松的 Map 接收：既便于提取关键字段，也能序列化出来供调试核对。
     */
    @FormUrlEncoded
    @POST("goods/scan/v2")
    suspend fun scanDevice(
        @FieldMap params: Map<String, String>,
        @Field("token") token: String,
    ): ApiEnvelope<Map<String, Any?>>

    @FormUrlEncoded
    @POST("goods/normal/skus")
    suspend fun goodsid2sku(
        @Field("goodsId") goodsId: String,
        @Field("token") token: String,
    ): ApiEnvelope<List<SkuData>>

    @FormUrlEncoded
    @POST("goods/normal/details")
    suspend fun getImei(
        @Field("goodsId") goodsId: String,
        @Field("token") token: String,
    ): ApiEnvelope<ImeiData>

    @FormUrlEncoded
    @POST("userIntegral/checkUserIsRisk")
    suspend fun useIntergral(@Field("token") token: String): ApiEnvelope<EmptyData>

    @FormUrlEncoded
    @POST("payChannelRoute/addUserAfterPayChannel")
    suspend fun addUserAfterPayChannel(
        @Field("method") method: String = "15",
        @Field("token") token: String,
    ): ApiEnvelope<EmptyData>

    @FormUrlEncoded
    @POST("orderRisk/isCheckLocation")
    suspend fun isCheckLocation(
        @Field("categoryCode") categoryCode: String = "04",
        @Field("imei") imei: String,
        @Field("token") token: String,
    ): ApiEnvelope<EmptyData>

    @FormUrlEncoded
    @POST("goods/water/unlock")
    suspend fun unlockWater(
        @Field("skuId") skuId: String,
        @Field("promotions") promotions: String,
        @Field("token") token: String,
    ): ApiEnvelope<UnlockData>

    @FormUrlEncoded
    @POST("goods/water/sync")
    suspend fun syncWater(
        @Field("skuId") skuId: String,
        @Field("token") token: String,
    ): ApiEnvelope<SyncData>

    @FormUrlEncoded
    @POST("order/afterPay/creating")
    suspend fun createAfterPay(
        @Field("orderNo") orderNo: String,
        @Field("token") token: String,
    ): ApiEnvelope<AfterPayCreatingData>

    @FormUrlEncoded
    @POST("order/detail")
    suspend fun orderDetail(
        @Field("orderId") orderId: String,
        @Field("token") token: String,
    ): ApiEnvelope<OrderDetailData>
}
