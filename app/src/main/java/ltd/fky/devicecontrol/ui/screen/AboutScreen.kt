package ltd.fky.devicecontrol.ui.screen

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ltd.fky.devicecontrol.BuildConfig
import ltd.fky.devicecontrol.ui.PROJECT_URL
import ltd.fky.devicecontrol.ui.component.ActionRow
import ltd.fky.devicecontrol.ui.component.DetailTopBar
import ltd.fky.devicecontrol.ui.component.InfoRow
import ltd.fky.devicecontrol.ui.component.SectionCard
import ltd.fky.devicecontrol.ui.component.SectionHeader
import ltd.fky.devicecontrol.ui.openProjectHome

/**
 * 关于页（二级页）。
 *
 * 展示应用信息、技术栈、当前运行环境、项目链接与免责声明。
 * 版本相关字段从 BuildConfig 读取，随构建自动更新，避免手写过期。
 */
@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        DetailTopBar("关于", "小乖生活 · v${BuildConfig.VERSION_NAME}", onBack)
        Spacer(Modifier.height(20.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(14.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Devices,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("小乖生活", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "共享设备解锁与积分任务助手",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("应用信息")
        SectionCard {
            InfoRow("版本名", BuildConfig.VERSION_NAME)
            InfoRow("版本号", BuildConfig.VERSION_CODE.toString())
            InfoRow("包名", context.packageName)
            InfoRow("构建类型", if (BuildConfig.DEBUG) "debug（调试版）" else BuildConfig.BUILD_TYPE)
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("技术栈")
        SectionCard {
            InfoRow("语言", "Kotlin")
            InfoRow("界面", "Jetpack Compose · Material 3")
            InfoRow("网络", "Retrofit · OkHttp · Moshi")
            InfoRow("构建", "Gradle 9.5.0 · AGP 9.3.3")
            InfoRow("编译 / 目标 SDK", "37 / 37")
            InfoRow("最低支持", "Android 8.0（API 26）")
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("当前运行环境")
        SectionCard {
            InfoRow("系统", "Android ${Build.VERSION.RELEASE}（API ${Build.VERSION.SDK_INT}）")
            InfoRow("机型", "${Build.MANUFACTURER} ${Build.MODEL}")
            InfoRow("CPU 架构", Build.SUPPORTED_ABIS.firstOrNull() ?: "未知")
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("项目")
        SectionCard(padding = 0.dp) {
            ActionRow(
                icon = Icons.Outlined.Code,
                title = "打开项目主页",
                onClick = { openProjectHome(context) },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            PROJECT_URL,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )

        Spacer(Modifier.height(20.dp))
        SectionHeader("免责声明")
        SectionCard {
            Text(
                "本项目用于复刻个人脚本的请求流程与移动端界面，仅供学习与研究之用。\n\n" +
                    "请自行承担账号、设备、接口变更与平台规则带来的风险；请勿将个人 Token、" +
                    "抓包文件或签名密钥上传到公开仓库。\n\n" +
                    "小票与积分的实际扣减以平台结算为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
