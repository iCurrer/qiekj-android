package ltd.fky.devicecontrol.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ltd.fky.devicecontrol.MainActivity
import ltd.fky.devicecontrol.R

/**
 * 长任务结果通知。
 *
 * 应用的两种「长任务」都要跑十几到二十几分钟（积分任务串行刷视频、解锁轮询等设备用完），
 * 用户必然会离开 App。没有这条通道时，「跑完了没有」只能靠回到 App 里翻日志 ——
 * 本类补上的就是这一点。
 *
 * **权限策略**：Android 13（API 33）起发通知需要运行时授权
 * [Manifest.permission.POST_NOTIFICATIONS]。未授权时 [post] 直接返回、静默跳过，
 * 不抛异常 —— 通知是锦上添花，不该反过来把主流程弄挂。
 */
class AppNotifier(private val context: Context) {

    /**
     * 创建通知渠道。
     *
     * Android 8 起通知必须归属某个渠道，否则发不出去。渠道一旦创建，
     * 后续对名称/重要级的修改会被系统忽略（用户可能已在设置里改过），
     * 所以这里只在缺失时创建，不做覆盖。
     */
    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESC
            },
        )
    }

    /** 是否已获得通知权限。Android 13 以下无需授权，恒为 true。 */
    fun canNotify(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    /** 积分任务跑完。积分数拿不到时只报完成，不编造数字。 */
    fun notifyTaskFinished(gained: Int?, total: Int?) {
        val text = buildString {
            append("今日积分 ")
            append(if (gained != null) "＋$gained" else "已完成结算")
            if (total != null) append("，当前共 $total")
        }
        post(ID_TASK, "积分任务已完成", text)
    }

    fun notifyTaskFailed(reason: String) {
        post(ID_TASK, "积分任务未完成", "中断原因：$reason")
    }

    fun notifyUnlockFinished(deviceName: String, ticketCost: String) {
        post(ID_UNLOCK, "「$deviceName」使用结束", "本次消耗小票 $ticketCost")
    }

    fun notifyUnlockFailed(deviceName: String, reason: String) {
        post(ID_UNLOCK, "「$deviceName」解锁失败", reason)
    }

    @SuppressLint("MissingPermission")
    private fun post(id: Int, title: String, text: String) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            // 内容可能长于一行（失败原因），用展开样式完整展示
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** 点通知回到 App 主界面；已在栈里就复用，不再叠一个新实例。 */
    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "task_result"

        private const val CHANNEL_NAME = "任务提醒"
        private const val CHANNEL_DESC = "积分任务与设备解锁的结果提醒"

        /** 同一类通知复用 ID：多次执行只留最新一条，不堆叠成通知列表。 */
        private const val ID_TASK = 1001
        private const val ID_UNLOCK = 1002
    }
}
