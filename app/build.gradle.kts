import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

/**
 * 签名密钥不进仓库。
 *
 * 积分任务需要对请求做 HMAC 签名，签名串里带服务端下发的 appSecret。
 * 该密钥属于第三方服务，**不随源码分发**：从 local.properties（已被 gitignore）
 * 或同名环境变量读取，两者都没有时取空串 —— 构建仍然通过，
 * 但真正发起请求时会在日志里明确提示"未配置"，而不是抛一个看不出原因的"签名错误"。
 *
 * 本机配置方式（写入 local.properties）：
 *   qiekj.appSecret=<android 渠道密钥>
 *   qiekj.alipaySecret=<alipay 渠道密钥>
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localSecret(key: String): String = localProps.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace = "ltd.fky.devicecontrol"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "ltd.fky.devicecontrol"
        minSdk = 26
        targetSdk = 37
        versionCode = 11
        versionName = "5.2"

        ndk {
            // ML Kit 的条码模型是 native 库，每个 ABI 各一份（arm64 约 4.9MB、x86 约 6.1MB），
            // 全打进去会让 APK 近乎翻倍。这里只保留主流 64 位真机架构。
            abiFilters += "arm64-v8a"
        }

        // 见文件顶部说明：密钥来自 local.properties，不进仓库
        buildConfigField("String", "SIGN_SECRET_ANDROID", "\"${localSecret("qiekj.appSecret")}\"")
        buildConfigField("String", "SIGN_SECRET_ALIPAY", "\"${localSecret("qiekj.alipaySecret")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // 关于页需要读取版本名、版本号、包名与构建类型
        buildConfig = true
    }
}

ksp {
    arg("appfunctions:aggregateAppFunctions", "true")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // 桌面卡片（Glance）。注意：1.2.0 的 POM 声明 compose-runtime 1.7.8 / ui-graphics 1.6.0，
    // 会被下方 BOM（2026.08.00）提升到新一代，属跨代混用；AAR 为已编译字节码，
    // 因此编译期不报错，兼容性只能在运行时验证。
    implementation("androidx.glance:glance-appwidget:1.2.0")

    // 扫码解锁：ML Kit 用 bundled 版（模型打进 APK，不依赖 Google Play 服务，国内设备可用）
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("androidx.camera:camera-camera2:1.6.2")
    implementation("androidx.camera:camera-lifecycle:1.6.2")
    implementation("androidx.camera:camera-view:1.6.2")

    // AppFunctions（Android 端 MCP 工具）：运行时 + KSP 代码生成
    implementation("androidx.appfunctions:appfunctions:1.0.0-alpha12")
    ksp("androidx.appfunctions:appfunctions-compiler:1.0.0-alpha12")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
