package ltd.fky.devicecontrol.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ltd.fky.devicecontrol.data.DeviceCacheStore
import ltd.fky.devicecontrol.data.DeviceItem
import ltd.fky.devicecontrol.ui.buildDeviceShortcutIntent

/**
 * 暖色令牌，与 `ui/theme/Color.kt` 保持一致。
 *
 * 桌面卡片渲染在 RemoteViews 里、走不到 Compose 主题，所以色值必须在这里显式写一遍。
 * 改动主 App 色板时，这里要同步。
 */

/** Ivory —— 页面底色 */
private val WidgetSurface = ColorProvider(Color(0xFFFAF9F5))

/** 卡片白 —— 设备行底色 */
private val WidgetRowSurface = ColorProvider(Color(0xFFFFFFFF))

/** Near Black —— 主文字 */
private val WidgetText = ColorProvider(Color(0xFF141413))

/** Mid Gray —— 次要文字 */
private val WidgetHint = ColorProvider(Color(0xFFB0AEA5))

/** Terracotta —— 强调色 */
private val WidgetAccent = ColorProvider(Color(0xFFC96442))

/**
 * 「小乖生活」桌面卡片。
 *
 * 数据来自 [DeviceCacheStore] 的本地快照 —— widget 不能发网络请求，
 * 由 App 在拉到最新设备后写入缓存并主动刷新卡片。
 *
 * **点击设备不会直接解锁**：经 PendingIntent 打开 App 的设备入口，
 * 由 App 内弹确认框二次确认后才执行。理由是解锁会阻塞数十分钟并产生真实扣费，
 * 误触的代价是真金白银。
 */
class DeviceGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp), // 2×2
            DpSize(250.dp, 110.dp), // 4×2
            DpSize(250.dp, 250.dp), // 4×4
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val devices = withContext(Dispatchers.IO) { DeviceCacheStore(context).list() }
        provideContent {
            XiaoGuaiWidgetContent(devices)
        }
    }
}

@Composable
private fun XiaoGuaiWidgetContent(devices: List<DeviceItem>) {
    val size = LocalSize.current
    // 高度不足时收紧行数，避免内容被 RemoteViews 裁掉
    val maxRows = if (size.height >= 200.dp) 6 else 2

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetSurface)
            .cornerRadius(16.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = "小乖生活",
                style = TextStyle(
                    color = WidgetText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = if (devices.isEmpty()) "未同步" else "${devices.size} 台",
                style = TextStyle(color = WidgetHint, fontSize = 11.sp),
            )
        }
        Spacer(GlanceModifier.height(10.dp))

        if (devices.isEmpty()) {
            Text(
                text = "打开 App 刷新设备列表",
                style = TextStyle(color = WidgetHint, fontSize = 12.sp),
            )
        } else {
            devices.take(maxRows).forEach { device ->
                DeviceRow(device)
                Spacer(GlanceModifier.height(6.dp))
            }
            if (devices.size > maxRows) {
                Text(
                    text = "还有 ${devices.size - maxRows} 台",
                    style = TextStyle(color = WidgetHint, fontSize = 11.sp),
                )
            }
        }
    }
}

/** 单个设备行：点击进入 App 的设备入口。 */
@Composable
private fun DeviceRow(device: DeviceItem) {
    val context = LocalContext.current
    val intent = buildDeviceShortcutIntent(
        context = context,
        goodsId = device.goodsId,
        id = device.id,
        goodsName = device.goodsName,
    )

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetRowSurface)
            .cornerRadius(10.dp)
            .clickable(actionStartActivity(intent))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = device.goodsName.ifBlank { "未命名设备" },
            style = TextStyle(color = WidgetText, fontSize = 13.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = "解锁",
            style = TextStyle(
                color = WidgetAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** 卡片接收器，在 AndroidManifest 中注册。 */
class DeviceGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceGlanceWidget()
}
