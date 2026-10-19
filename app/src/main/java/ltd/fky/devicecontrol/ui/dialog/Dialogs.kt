package ltd.fky.devicecontrol.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import ltd.fky.devicecontrol.data.OrderHistoryItem
import ltd.fky.devicecontrol.data.UnlockResult
import ltd.fky.devicecontrol.data.UsageStats
import ltd.fky.devicecontrol.data.usageStatsSince
import ltd.fky.devicecontrol.ui.DeviceRow

/** Token 展示对话框（等宽字体便于核对）。 */
@Composable
internal fun TokenDialog(token: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("当前 Token") },
        text = {
            Text(
                text = token,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

/** 订单结算详情对话框。 */
@Composable
internal fun OrderDetailDialog(
    detail: UnlockResult,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("订单详情") },
        text = {
            Column {
                DetailLine("订单号", detail.orderNo)
                DetailLine("订单 ID", detail.orderId)
                DetailLine("订单原价", detail.originPrice)
                DetailLine("花费小票", detail.ticketCost)
                DetailLine("积分抵扣", detail.integralCost)
                if (detail.otherPromotions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("其他优惠", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    detail.otherPromotions.forEach { promotion ->
                        DetailLine(
                            label = "类型 ${promotion.promotionType ?: "-"}",
                            value = promotion.discountAmount ?: "-",
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

/** 二次确认对话框，用于退出登录等有副作用、需要用户确认的操作。 */
@Composable
internal fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

/** 本地历史订单列表对话框。 */
@Composable
internal fun OrderHistoryDialog(
    orders: List<OrderHistoryItem>,
    onDismiss: () -> Unit,
    onOpenOrder: (OrderHistoryItem) -> Unit,
) {
    // 只在订单集变化时重算，避免每次重组都把整份历史遍历一遍
    val stats = remember(orders) { orders.usageStatsSince(startOfThisMonth()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("历史订单") },
        text = {
            if (orders.isEmpty()) {
                Text("暂无历史订单", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    // 汇总做成列表的第一项，而不是套在 LazyColumn 外面：
                    // AlertDialog 的高度约束只落在可滚动区域上，外面再包一层 Column
                    // 会让 LazyColumn 拿到无限高度约束而崩掉
                    item {
                        UsageSummary(stats)
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    items(orders) { item ->
                        OrderHistoryRow(item = item, onClick = { onOpenOrder(item) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

/** 本月使用汇总：解锁次数 / 消耗小票 / 原价合计。 */
@Composable
private fun UsageSummary(stats: UsageStats) {
    Column {
        Text("本月使用", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryCell("解锁次数", "${stats.count} 次")
            SummaryCell("消耗小票", stats.ticketText)
            SummaryCell("原价合计", stats.originText)
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OrderHistoryRow(item: OrderHistoryItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.goodsName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            // 清一色已完成的列表里出现这一条，说明上次解锁没走到结算
            // （通常是等待期间进程被系统回收）
            if (item.isRunning) {
                Text(
                    "进行中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${HISTORY_TIME_FORMAT.format(Instant.ofEpochMilli(item.completedAt))} · 订单 ${item.orderNo}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "原价：${item.originPrice}  小票：${item.ticketCost}  积分：${item.integralCost}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** 历史记录的时间显示；精确到分钟足够，也不至于把列表挤变形。 */
private val HISTORY_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault())

/** 本月 1 日 0 点的毫秒时间戳，用于「本月使用」统计口径。 */
private fun startOfThisMonth(): Long = LocalDate.now()
    .withDayOfMonth(1)
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

/** 设备操作菜单：重命名 / 收藏 / 隐藏。 */
@Composable
internal fun DeviceActionDialog(
    title: String,
    favorite: Boolean,
    onRename: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, maxLines = 1) },
        text = {
            Column {
                MenuAction(
                    icon = Icons.Outlined.Edit,
                    label = "重命名",
                    onClick = onRename,
                )
                MenuAction(
                    icon = if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    label = if (favorite) "取消收藏" else "收藏（置顶）",
                    onClick = onToggleFavorite,
                )
                MenuAction(
                    icon = Icons.Outlined.VisibilityOff,
                    label = "隐藏",
                    onClick = onHide,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 重命名对话框；留空表示恢复服务端原名。 */
@Composable
internal fun RenameDeviceDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设备名称") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("显示名称") },
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "只在你的手机上生效，留空可恢复服务端原名。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

/** 已隐藏设备管理：可逐台恢复。 */
@Composable
internal fun HiddenDevicesDialog(
    rows: List<DeviceRow>,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已隐藏的设备") },
        text = {
            if (rows.isEmpty()) {
                Text("暂无隐藏设备", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(rows) { row ->
                        HiddenDeviceRow(row = row, onRestore = { onRestore(row.key) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}

@Composable
private fun HiddenDeviceRow(row: DeviceRow, onRestore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
        TextButton(onClick = onRestore) { Text("恢复") }
    }
}
