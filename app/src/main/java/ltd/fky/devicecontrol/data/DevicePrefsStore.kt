package ltd.fky.devicecontrol.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * 单台设备的本地偏好。
 *
 * 这些字段**只影响界面显示**，不参与解锁 —— 解锁链路唯一的输入是 goodsId，
 * 因此改名、隐藏、排序都不会影响功能。
 */
data class DevicePref(
    /** 自定义显示名；为空时回落到服务端返回的名字。 */
    val alias: String? = null,
    /** 收藏：在列表中置顶。 */
    val favorite: Boolean = false,
    /** 隐藏：不在主列表显示，可在「已隐藏」里恢复。 */
    val hidden: Boolean = false,
)

/**
 * 设备在本地偏好里的稳定标识。
 *
 * 优先用 goodsId（与解锁用的是同一个值），退化到 id，最后用名字兜底，
 * 这样即使服务端某些字段缺失，偏好也不会串到别的设备上。
 */
val DeviceItem.prefKey: String
    get() = goodsId?.takeIf { it.isNotBlank() }
        ?: id?.takeIf { it.isNotBlank() }
        ?: "name:$goodsName"

/**
 * 设备偏好的本地存储。
 *
 * 只存在本机（SharedPreferences + Moshi，沿用项目既有模式，零新依赖）。
 * 换机或重装会丢失 —— 这是刻意的：服务端并没有存放用户自定义备注的地方，
 * 想跨设备同步就得另建账号侧存储。
 */
class DevicePrefsStore(context: Context) {
    private val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
    private val adapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter<Map<String, DevicePref>>(
            Types.newParameterizedType(Map::class.java, String::class.java, DevicePref::class.java),
        )

    fun all(): Map<String, DevicePref> {
        val raw = prefs.getString(KEY_PREFS, null) ?: return emptyMap()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyMap())
    }

    fun save(items: Map<String, DevicePref>) {
        prefs.edit().putString(KEY_PREFS, adapter.toJson(items)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PREFS).apply()
    }

    private companion object {
        const val KEY_PREFS = "prefs"
    }
}
