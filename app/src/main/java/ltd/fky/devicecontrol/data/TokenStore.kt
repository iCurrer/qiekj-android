package ltd.fky.devicecontrol.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * token 的加密本地存储。
 *
 * 两处刻意的设计，都是为了别拖慢启动与网络：
 *
 * 1. **懒创建**：`EncryptedSharedPreferences.create()` 首次会走 AndroidKeyStore
 *    生成 / 读取 AES 密钥，耗时可达几十到几百毫秒。若放在构造函数里，
 *    则「谁构造 TokenStore 谁就得等」—— 冷启动路径很容易踩上。
 * 2. **内存缓存**：token 会被 OkHttp 拦截器**在每个请求里**读一次。
 *    解锁链路有 7 个以上请求，轮询阶段每秒 1 次 —— 每次都做一遍 AES 解密纯属浪费。
 *    缓存随写入同步更新，因此不会读到过期值。
 */
class TokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val lock = Any()

    @Volatile
    private var prefs: SharedPreferences? = null

    /** 缓存值；[loaded] 为 false 时不可信（用于区分「没读过」与「读过但为空」）。 */
    @Volatile
    private var cached: String? = null

    @Volatile
    private var loaded = false

    fun readToken(): String? {
        if (loaded) return cached
        return synchronized(lock) {
            if (!loaded) {
                cached = runCatching { prefsOrCreate().getString(KEY_TOKEN, null) }.getOrNull()
                loaded = true
            }
            cached
        }
    }

    fun saveToken(token: String) {
        synchronized(lock) {
            cached = token
            loaded = true
        }
        prefsOrCreate().edit().putString(KEY_TOKEN, token).apply()
    }

    fun clear() {
        synchronized(lock) {
            cached = null
            loaded = true
        }
        prefsOrCreate().edit().remove(KEY_TOKEN).apply()
    }

    /** 双重检查：并发首次访问时只创建一次（`synchronized` 可重入，嵌套调用安全）。 */
    private fun prefsOrCreate(): SharedPreferences {
        prefs?.let { return it }
        return synchronized(lock) {
            prefs ?: EncryptedSharedPreferences.create(
                appContext,
                "secure_token",
                MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            ).also { prefs = it }
        }
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
