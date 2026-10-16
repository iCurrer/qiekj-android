package ltd.fky.devicecontrol.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ltd.fky.devicecontrol.ui.component.DetailTopBar
import ltd.fky.devicecontrol.ui.component.InfoRow
import ltd.fky.devicecontrol.ui.component.SectionCard
import ltd.fky.devicecontrol.ui.component.SectionHeader

/**
 * 使用说明页（二级页）。
 *
 * 把「怎么用」集中到一处：主流程、桌面入口、AI 调用方式与常见问题，
 * 避免用户只能靠试错摸索。
 */
@Composable
internal fun GuideScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        DetailTopBar("使用说明", "从登录到解锁的完整流程", onBack)
        Spacer(Modifier.height(20.dp))

        SectionHeader("快速上手")
        SectionCard(padding = 0.dp) {
            GuideStep(1, "登录账号", "进入「我的」，填写手机号并获取验证码。")
            StepDivider()
            GuideStep(2, "解锁设备", "两种方式：点历史设备卡片，或点右上角扫码对准设备二维码。")
            StepDivider()
            GuideStep(3, "查看结算", "解锁结束后自动弹出订单详情，也可在「我的」→「历史订单」回看。")
            StepDivider()
            GuideStep(4, "跑积分任务", "「我的」→「积分任务」→「开始执行」，执行中可随时返回。")
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("扫码解锁")
        SectionCard {
            Text("首次使用无需先绑定", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            InfoRow("入口", "「设备」页右上角的扫码图标")
            InfoRow("对准", "设备屏幕上显示的二维码")
            InfoRow("识别后", "弹出确认框，确认后才扣费")
            Spacer(Modifier.height(12.dp))
            HintText("扫到的设备会自动加入设备列表，之后也能从列表直接点。使用时需要授予相机权限。")
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("桌面入口")
        SectionCard {
            Text("两种方式", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            InfoRow("单个设备", "在设备卡右侧点「添加到桌面」")
            InfoRow("设备卡片", "长按桌面 → 添加小部件 → 小乖生活")
            Spacer(Modifier.height(12.dp))
            HintText("两者都是「打开 App 后确认解锁」，不会在桌面直接扣费。")
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("交给 AI 助手")
        SectionCard {
            Text("可被调用的能力", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            InfoRow("列出设备", "只读")
            InfoRow("查询余额", "只读")
            InfoRow("解锁设备", "产生真实订单与扣费")
            InfoRow("积分任务", "只影响本账号积分")
            Spacer(Modifier.height(12.dp))
            HintText("解锁与积分任务要求你先明确确认；需 Android 16 及以上，且由系统级助手发起。")
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("常见问题")
        SectionCard {
            FaqItem(
                "解锁失败怎么办？",
                "先在设备页点右上角刷新，重查余额与设备列表；若仍失败，多为接口返回结构变化或风控拦截，可到「我的」→「查看当前 Token」确认登录态。",
            )
            FaqDivider()
            FaqItem(
                "为什么余额放在设备页？",
                "解锁要消耗小票，把余额前置到「选择设备之前」，避免点了卡片才发现不够。",
            )
            FaqDivider()
            FaqItem(
                "积分任务能中断吗？",
                "可以随时返回或切到其他页面，任务在后台继续；顶部状态条会持续提示，点击即可回到执行页。",
            )
            FaqDivider()
            FaqItem(
                "换个手机要重新登录吗？",
                "要。Token 存在本机加密存储中，不会随账号漫游。",
            )
            FaqDivider()
            FaqItem(
                "退出登录会丢数据吗？",
                "只清空 Token 与资产数据，本机历史订单会保留。",
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

/** 带编号的步骤行。 */
@Composable
private fun GuideStep(index: Int, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(8.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(3.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 步骤之间的分隔线，左侧留出对齐编号块的缩进。 */
@Composable
private fun StepDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 55.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** 常见问题条目。 */
@Composable
private fun FaqItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(5.dp))
        Text(
            answer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FaqDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** 补充说明文字。 */
@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
