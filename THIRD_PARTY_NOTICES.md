# 第三方组件与许可

本文件汇总本仓库直接依赖的第三方组件的许可情况，便于分发时履行署名义务。

> 说明：以下为**直接依赖**，不含其传递依赖。AndroidX 系列组件的完整许可文本见
> 各构件包内的 `META-INF/LICENSE`。

## 运行时依赖

| 组件 | 版本 | 许可 |
| --- | --- | --- |
| AndroidX Compose（BOM） | `2026.08.00` | Apache-2.0 |
| AndroidX Activity / Core / Lifecycle | — | Apache-2.0 |
| AndroidX Security Crypto | `1.1.0-alpha06` | Apache-2.0 |
| AndroidX Glance（桌面卡片） | `1.2.0` | Apache-2.0 |
| AndroidX CameraX | `1.6.2` | Apache-2.0 |
| AndroidX AppFunctions | `1.0.0-alpha12` | Apache-2.0 |
| Kotlin 标准库 / kotlinx.coroutines | — | Apache-2.0 |
| Retrofit | `2.11.0` | Apache-2.0 |
| OkHttp / logging-interceptor | `4.12.0` | Apache-2.0 |
| Moshi | `1.15.1` | Apache-2.0 |
| **ML Kit 条码识别（bundled）** | `17.3.0` | ⚠️ **Google ML Kit 服务条款（非开源许可）** |

## 构建期依赖

| 组件 | 版本 | 许可 |
| --- | --- | --- |
| Android Gradle Plugin | `9.3.3` | Apache-2.0 |
| Gradle | `9.5.0` | Apache-2.0 |
| KSP（Kotlin Symbol Processing） | `2.3.12` | Apache-2.0 |
| AppFunctions 编译器 | `1.0.0-alpha12` | Apache-2.0 |

## 需要留意的一点

**ML Kit (`com.google.mlkit:barcode-scanning`) 不是开源组件**，按 Google 的
[ML Kit 服务条款](https://developers.google.com/ml-kit/terms) 提供，其中包含对模型使用、
数据与再分发的限制。本项目使用其 **bundled 版本**（模型随 APK 分发，不依赖 Google Play 服务），
因此再分发本 APK 时，该部分仍受 Google 条款约束，不适用本仓库的 MIT 许可。

## 本仓库结论

- 除 ML Kit 外，全部直接依赖均为宽松许可（Apache-2.0），**无 GPL / AGPL / LGPL 等传染性许可**。
- 本仓库**不包含**任何以 copyleft 许可发布的代码。
