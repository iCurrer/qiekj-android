package ltd.fky.devicecontrol.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class OrderHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("order_history", Context.MODE_PRIVATE)
    private val adapter = Moshi.Builder()
        .add(LenientStringJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<OrderHistoryItem>>(
            Types.newParameterizedType(List::class.java, OrderHistoryItem::class.java),
        )

    fun list(): List<OrderHistoryItem> {
        val raw = prefs.getString(KEY_ORDERS, null) ?: return emptyList()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
    }

    fun add(item: OrderHistoryItem) {
        val next = (list().filterNot { it.orderNo == item.orderNo } + item)
            .sortedByDescending { it.completedAt }
            .take(MAX_HISTORY)
        prefs.edit().putString(KEY_ORDERS, adapter.toJson(next)).apply()
    }

    /**
     * 用最终结算结果替换掉先前那条「进行中」记录。
     *
     * 不能直接用 [add] 覆盖：解锁结束时服务端可能下发新的订单号
     * （链路里的 `identify`），与受理时那条记录的 `orderNo` **不同** ——
     * 只按最终订单号写入，会留下一条永远停在「进行中」的孤儿记录。
     * 这里把两个号都清掉再落最终结果。
     */
    fun complete(runningOrderNo: String, item: OrderHistoryItem) {
        val next = (
            list().filterNot { it.orderNo == runningOrderNo || it.orderNo == item.orderNo } +
                item
            )
            .sortedByDescending { it.completedAt }
            .take(MAX_HISTORY)
        prefs.edit().putString(KEY_ORDERS, adapter.toJson(next)).apply()
    }

    private companion object {
        const val KEY_ORDERS = "orders"
        const val MAX_HISTORY = 50
    }
}
