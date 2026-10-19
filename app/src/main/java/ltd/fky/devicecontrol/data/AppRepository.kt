package ltd.fky.devicecontrol.data

import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URLDecoder
import java.util.concurrent.TimeUnit
import ltd.fky.devicecontrol.BuildConfig

class AppRepository(
    private val tokenStore: TokenStore,
    private val orderHistoryStore: OrderHistoryStore,
    private val deviceCacheStore: DeviceCacheStore,
    private val devicePrefsStore: DevicePrefsStore,
    /**
     * 设备数据变化后的回调，用于刷新桌面卡片。
     *
     * 用回调而不是直接依赖 widget 包，是为了保持 data 层不认识 UI 层；
     * 同时给了默认空实现，不传也不影响其他场景。
     */
    private val onDevicesChanged: suspend () -> Unit = {},
) {
    private val api: DeviceApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC 只记方法 / URL / 耗时，开销很小，但生产包不该带任何请求日志
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor { tokenStore.readToken() })
            .addInterceptor(logging)
            .build()
        val moshi = Moshi.Builder()
            .add(EmptyDataJsonAdapter())
            .add(LenientStringJsonAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()

        api = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DeviceApi::class.java)
    }

    fun localToken(): String? = tokenStore.readToken()

    /**
     * 预热加密存储。
     *
     * 首次 [localToken] 会触发 `EncryptedSharedPreferences` 的创建（走 AndroidKeyStore），
     * 这一步可能耗时几十到几百毫秒。由 Application 在后台线程调用，
     * 把这份开销从冷启动的主线程路径上挪走；调用之后 token 也会进内存缓存，
     * 后续（含 OkHttp 拦截器的每次请求）都不再触碰磁盘。
     */
    fun warmUp() {
        tokenStore.readToken()
    }

    /**
     * 退出登录：清空本地 token 与设备快照。
     *
     * 设备列表属于账号数据，退出后必须清掉，否则桌面卡片会继续展示上一个账号的设备。
     * 订单历史是本机记录，予以保留。
     */
    suspend fun logout() {
        tokenStore.clear()
        deviceCacheStore.clear()
        onDevicesChanged()
    }

    fun orderHistory(): List<OrderHistoryItem> = orderHistoryStore.list()

    /** 读取设备本地偏好（别名 / 收藏 / 隐藏）。 */
    fun devicePrefs(): Map<String, DevicePref> = devicePrefsStore.all()

    /** 保存设备本地偏好。 */
    fun saveDevicePrefs(prefs: Map<String, DevicePref>) = devicePrefsStore.save(prefs)

    suspend fun sendCode(phone: String) {
        api.sendCode(phone = phone).throwIfFailed()
    }

    suspend fun login(phone: String, code: String): String {
        val token = api.login(phone = phone, verify = code).requireData().token
            ?: error("登录成功但未返回 token")
        tokenStore.saveToken(token)
        return token
    }

    suspend fun queryBalance(): BalanceData {
        val token = requireToken()
        return api.queryBalance(token).requireData()
    }

    suspend fun latestDevices(): List<DeviceItem> {
        val token = requireToken()
        val devices = api.getLatestUsed(token = token).requireData()
        // 落一份本地快照：桌面卡片不能发网络请求，读的就是这里的数据
        deviceCacheStore.save(devices)
        onDevicesChanged()
        return devices
    }

    /**
     * 扫码识别设备。
     *
     * 与 H5 端同一套流程：把扫码得到的原始内容交给 `goods/scan/v2`，
     * 服务端返回设备信息，其中 `id` 字段即解锁所需的 goodsId。
     *
     * 返回结构用宽松的 Map 接收，既能兼容字段命名差异，也能把原始结构
     * 序列化进 [ScanDeviceResult.rawJson] 供调试核对。
     */
    suspend fun scanDevice(qrCode: String): ScanDeviceResult {
        val token = requireToken()
        val envelope = api.scanDevice(params = parseQrPayload(qrCode), token = token)
        val map = envelope.data
            ?: error(envelope.message ?: envelope.msg ?: "扫码失败：服务端未返回设备信息")

        val goodsId = readField(map, "id", "goodsId", "goods_id")
            ?: error("已连通服务端但未识别出设备编号（返回字段：${map.keys.joinToString()}）")

        return ScanDeviceResult(
            goodsId = goodsId,
            goodsName = readField(map, "goodsName", "name", "machineName"),
            shopId = readField(map, "shopId"),
            imei = readField(map, "imei"),
            rawJson = runCatching { scanRawAdapter().toJson(map) }.getOrDefault(""),
        )
    }

    /**
     * 解析二维码内容，得到服务端期望的请求参数。
     *
     * 对应 H5 的 `scanCodeInit()`：
     * - 内容不含 `http` → 视为设备裸码，参数为 `IMEI`
     * - 内容含 `http` → 取 `?` 之后的 query，提取 `SN` / `IMEI` / `NQT` / `qeykid`
     *
     * 注意：请求体是这些**具体字段**，不是整段二维码文本 —— 传整段会得到「参数错误」。
     */
    private fun parseQrPayload(raw: String): Map<String, String> {
        val text = raw.trim()
        if (text.isEmpty()) error("二维码内容为空")

        if (!text.contains("http", ignoreCase = true)) {
            return mapOf("IMEI" to text)
        }

        val query = text.substringAfter('?', "")
        if (query.isBlank()) error("二维码地址缺少参数")

        val params = linkedMapOf<String, String>()
        query.replace('?', '&').split('&').forEach { pair ->
            val index = pair.indexOf('=')
            if (index <= 0) return@forEach
            val key = pair.substring(0, index)
            val rawValue = pair.substring(index + 1)
            val value = runCatching { URLDecoder.decode(rawValue, "UTF-8") }.getOrDefault(rawValue)
            if (value.isNotBlank() && key in QR_PARAM_KEYS) params[key] = value
        }

        if (params.isEmpty()) error("二维码内容无法识别（未找到 SN / IMEI / NQT）")
        return params
    }

    /**
     * 把扫码得到的设备并入本地快照。
     *
     * 目的：扫到的设备可能从未出现在 `goods/latestUsed` 里，
     * 先写进缓存，桌面卡片与历史列表就能立刻看到它。
     */
    suspend fun rememberDevice(device: DeviceItem) {
        val merged = (listOf(device) + deviceCacheStore.list().filterNot { it.goodsId == device.goodsId })
            .take(20)
        deviceCacheStore.save(merged)
        onDevicesChanged()
    }

    /** 把扫码返回的宽松结构序列化成 JSON，供调试面板核对服务端真实字段。 */
    private fun scanRawAdapter() = Moshi.Builder().build()
        .adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        )

    /** 宽松读字段：服务端可能把数字返回成 Double，也可能改了命名，这里逐个兜底。 */
    private fun readField(map: Map<String, Any?>, vararg keys: String): String? {
        keys.forEach { key ->
            val value = map[key] ?: return@forEach
            val text = when (value) {
                is String -> value
                is Number -> value.toLong().toString()
                else -> value.toString()
            }
            if (text.isNotBlank()) return text
        }
        return null
    }

    suspend fun unlockDevice(
        device: DeviceItem,
        onStep: suspend (String) -> Unit,
    ): UnlockResult {
        val token = requireToken()
        val goodsId = device.goodsId ?: device.id ?: error("设备缺少 goodsId")

        onStep("正在获取 SKU")
        val skuId = api.goodsid2sku(goodsId = goodsId, token = token).requireData().firstOrNull()?.skuId
            ?: error("未获取到 skuId")

        onStep("正在获取 IMEI")
        val imei = api.getImei(goodsId = goodsId, token = token).requireData().imei
            ?: error("未获取到 imei")

        onStep("正在检查积分")
        api.useIntergral(token).throwIfFailed()

        onStep("正在开通后付")
        api.addUserAfterPayChannel(token = token).throwIfFailed()

        onStep("正在检查位置风控")
        api.isCheckLocation(imei = imei, token = token).throwIfFailed()

        onStep("正在启动解锁")
        val unlock = api.unlockWater(
            skuId = skuId,
            promotions = PROMOTIONS,
            token = token,
        ).requireData()

        val orderNo = unlock.orderNo ?: error("未获取到订单号")
        onStep("设备已启动，等待使用结束")

        // 先落一条「进行中」记录。
        //
        // 小票在解锁被服务端受理的那一刻就已扣除，而结算要等设备跑完 ——
        // 这段等待可能长达二十多分钟。若进程在此期间被系统回收（切后台 + 锁屏进 Doze
        // 完全可能），没有这条记录的话，用户看到的就是「钱花了、订单历史里什么都没有」。
        // 先占位，至少留下「哪台设备、什么时候」。
        orderHistoryStore.add(
            OrderHistoryItem(
                orderNo = orderNo,
                orderId = "",
                goodsName = device.goodsName.ifBlank { "未命名设备" },
                originPrice = "-",
                ticketCost = "-",
                integralCost = "-",
                otherPromotions = emptyList(),
                completedAt = System.currentTimeMillis(),
                status = OrderHistoryItem.STATUS_RUNNING,
                skuId = skuId,
            ),
        )

        var lastStatus: SyncData
        do {
            delay(1000)
            lastStatus = api.syncWater(skuId = skuId, token = token).requireData()
            onStep("设备工作中，正在等待完成")
        } while (lastStatus.workStatus == 2)

        val finalOrderNo = lastStatus.identify ?: orderNo
        onStep("正在创建后付订单")
        val orderId = api.createAfterPay(orderNo = finalOrderNo, token = token).requireData().orderId
            ?: error("未获取到 orderId")

        onStep("正在查询订单详情")
        val detail = api.orderDetail(orderId = orderId, token = token).requireData()
        val ticketCost = detail.promotionList.firstOrNull { it.promotionType == 4 }?.discountAmount ?: "-"
        val integralCost = detail.promotionList.firstOrNull { it.promotionType == 8 }?.discountAmount ?: "-"
        val otherPromotions = detail.promotionList
            .filter { it.promotionType != 4 && it.promotionType != 8 }
            .map {
                PromotionSummary(
                    promotionType = it.promotionType,
                    discountAmount = it.discountAmount,
                )
            }

        val result = UnlockResult(
            orderNo = finalOrderNo,
            orderId = orderId,
            originPrice = detail.tradeOrderItem.firstOrNull()?.originPrice ?: "-",
            ticketCost = ticketCost,
            integralCost = integralCost,
            otherPromotions = otherPromotions,
            completedAt = System.currentTimeMillis(),
        )
        // 用最终结果替换掉开始那条「进行中」：结算后服务端可能下发新的订单号
        // （上面的 identify），只按最终号覆盖会留下永远「进行中」的孤儿记录
        orderHistoryStore.complete(
            runningOrderNo = orderNo,
            item = OrderHistoryItem(
                orderNo = result.orderNo,
                orderId = result.orderId,
                goodsName = device.goodsName.ifBlank { "未命名设备" },
                originPrice = result.originPrice,
                ticketCost = result.ticketCost,
                integralCost = result.integralCost,
                otherPromotions = result.otherPromotions,
                completedAt = result.completedAt,
                status = OrderHistoryItem.STATUS_DONE,
                skuId = skuId,
            ),
        )
        return result
    }

    private fun requireToken(): String = tokenStore.readToken()?.takeIf { it.isNotBlank() }
        ?: error("请先登录")

    private fun ApiEnvelope<*>.throwIfFailed() {
        if (code != null && code != 0 && code != 200) {
            error(message ?: msg ?: "请求失败")
        }
    }

    private companion object {
        /** 二维码 query 里可能出现的参数名，对应 H5 的 scanCodeInit()。 */
        val QR_PARAM_KEYS = setOf("SN", "IMEI", "NQT", "qeykid")

        const val PROMOTIONS =
            """[{"assetId":"0","oldPromotionId":"","orgId":"0","promotionId":"0","promotionType":"-6"},{"assetId":"0","oldPromotionId":"","orgId":"0","promotionId":"0","promotionType":"-7"},{"assetId":"0","oldPromotionId":"0","orgId":"0","promotionId":"0","promotionType":"8"}]"""
    }
}
