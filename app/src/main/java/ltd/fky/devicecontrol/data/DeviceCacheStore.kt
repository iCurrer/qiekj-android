package ltd.fky.devicecontrol.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * 设备列表的本地快照。
 *
 * 桌面卡片运行在 RemoteViews 宿主里，**不能做网络请求**，因此必须有一份
 * 可直接读的本地数据。App 每次拉到最新设备后写入这里，卡片只读不写。
 *
 * 沿用 [OrderHistoryStore] 的存储方式（SharedPreferences + Moshi），保持零新依赖。
 */
class DeviceCacheStore(context: Context) {
    private val prefs = context.getSharedPreferences("device_cache", Context.MODE_PRIVATE)
    private val adapter = Moshi.Builder()
        .add(LenientStringJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<DeviceItem>>(
            Types.newParameterizedType(List::class.java, DeviceItem::class.java),
        )

    fun list(): List<DeviceItem> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
    }

    fun save(devices: List<DeviceItem>) {
        prefs.edit().putString(KEY_DEVICES, adapter.toJson(devices)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_DEVICES).apply()
    }

    private companion object {
        const val KEY_DEVICES = "devices"
    }
}
