package ltd.fky.devicecontrol.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.widget.Toast
import ltd.fky.devicecontrol.MainActivity
import ltd.fky.devicecontrol.R
import ltd.fky.devicecontrol.data.DeviceItem

/**
 * 桌面快捷方式、桌面卡片入口与项目主页支持。
 *
 * 这里集中放置与 Intent / ShortcutManager 相关的常量与工具函数，
 * 避免 Activity、屏幕组件与桌面卡片各自持有重复的字符串字面量。
 */

/** 点击桌面设备快捷方式（或桌面卡片里的设备）时发送的 action。 */
const val ACTION_OPEN_DEVICE_SHORTCUT = "ltd.fky.devicecontrol.OPEN_DEVICE_SHORTCUT"

/** 本项目（fork 后）的仓库地址。 */
const val PROJECT_URL = "https://github.com/iCurrer/qiekj-android"

private const val EXTRA_GOODS_ID = "goods_id"
private const val EXTRA_DEVICE_ID = "device_id"
private const val EXTRA_GOODS_NAME = "goods_name"

/** 从启动 Intent 解析设备快捷方式请求；非快捷方式启动返回 null。 */
fun shortcutRequestFromIntent(intent: Intent?): DeviceShortcutRequest? {
    if (intent?.action != ACTION_OPEN_DEVICE_SHORTCUT) return null
    return DeviceShortcutRequest(
        goodsId = intent.getStringExtra(EXTRA_GOODS_ID),
        id = intent.getStringExtra(EXTRA_DEVICE_ID),
        goodsName = intent.getStringExtra(EXTRA_GOODS_NAME),
    )
}

/**
 * 构建设备入口 Intent。
 *
 * 桌面快捷方式与桌面卡片共用同一份契约：都经 [ACTION_OPEN_DEVICE_SHORTCUT]
 * 启动 MainActivity，由它解析 extra 并进入「在 App 内确认后解锁」的流程。
 * 也就是说**卡片本身不会扣费**，误触不会造成资金损失。
 */
fun buildDeviceShortcutIntent(
    context: Context,
    goodsId: String?,
    id: String?,
    goodsName: String?,
): Intent = Intent(context, MainActivity::class.java).apply {
    action = ACTION_OPEN_DEVICE_SHORTCUT
    putExtra(EXTRA_GOODS_ID, goodsId)
    putExtra(EXTRA_DEVICE_ID, id)
    putExtra(EXTRA_GOODS_NAME, goodsName)
}

/** 请求把某个设备固定为桌面快捷方式。 */
fun pinDeviceShortcut(context: Context, device: DeviceItem) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        Toast.makeText(context, "当前系统不支持添加桌面快捷方式", Toast.LENGTH_LONG).show()
        return
    }
    val shortcutManager = context.getSystemService(ShortcutManager::class.java)
    if (shortcutManager?.isRequestPinShortcutSupported != true) {
        Toast.makeText(context, "当前桌面不支持添加快捷方式", Toast.LENGTH_LONG).show()
        return
    }

    val label = device.goodsName.ifBlank { "历史设备" }
    val shortcutIntent = buildDeviceShortcutIntent(
        context = context,
        goodsId = device.goodsId,
        id = device.id,
        goodsName = device.goodsName,
    )
    val shortcut = ShortcutInfo.Builder(
        context,
        "device-${device.goodsId ?: device.id ?: device.goodsName.hashCode()}",
    )
        .setShortLabel(label.take(10))
        .setLongLabel(label)
        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
        .setIntent(shortcutIntent)
        .build()

    shortcutManager.requestPinShortcut(shortcut, null)
}

/** 用浏览器打开项目主页。 */
fun openProjectHome(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)))
}
