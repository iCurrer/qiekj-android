# 小乖生活 · XiaoGuai Life

> **共享饮水设备的非官方 Android 客户端。** 扫设备屏幕上的二维码即可解锁，
> 不必先在官方 App 里扫码绑定；并附带桌面卡片、积分任务自动化与 AI 助手（Android MCP）调用能力。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](#技术栈)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)](#技术栈)
[![compileSdk](https://img.shields.io/badge/compileSdk-37-2496ED)](#技术栈)

> ⚠️ **本项目是非官方客户端**，与设备厂商及其运营方**没有任何关联**，未获授权、未获背书。
> 代码用于复刻个人脚本的请求流程与移动端界面，仅供个人学习与自用。
> 请自行承担账号、设备、接口变更与平台规则风险。
>
> **不要把个人 Token、抓包记录、签名密钥提交到公开仓库。** 本仓库已做密钥外置处理，见
> [仓库里没有什么](#仓库里没有什么)。

## 目录

- [功能一览](#功能一览)
- [快速开始](#快速开始)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [设计说明](#设计说明)
  - [视觉设计](#视觉设计)
  - [界面层级](#界面层级)
  - [扫码解锁](#扫码解锁)
  - [桌面卡片](#桌面卡片)
  - [设备列表自定义](#设备列表自定义)
  - [AppFunctions（Android MCP）](#appfunctionsandroid-mcp)
- [仓库里没有什么](#仓库里没有什么)
- [许可与归属](#许可与归属)
- [注意事项](#注意事项)

---

## 功能一览

| 功能 | 说明 |
| --- | --- |
| 🔓 **扫码解锁** | 设备页直接扫设备屏幕二维码解锁，**不需要先绑定的历史设备** |
| 📋 **设备列表** | 历史设备自动加载；支持别名 / 收藏置顶 / 隐藏 |
| 💳 **资产速览** | 设备页顶部常驻小票与积分余额，选设备前就能看到够不够 |
| 🧩 **桌面卡片** | Glance 小组件展示设备列表，点击打开 App 二次确认后解锁 |
| ⚡ **积分任务** | 每日签到与积分任务自动化，带终端风格实时日志；执行中可切页，顶部状态条一键回到执行页 |
| 🧾 **历史订单** | 解锁结束自动弹出结算详情，本地留存可回看 |
| 📖 **使用说明 / 关于** | 应用内完整文档；关于页运行时读取版本、机型与 ABI |
| 🤖 **AI 助手调用** | 通过 AppFunctions（Android 端 MCP）把 4 项能力暴露给系统级 AI 助手 |
| 🎨 **暖色设计** | 自建 Anthropic 暖色系设计系统，默认关闭 Material You 动态取色 |
| 📐 **大屏自适应** | 平板 / 折叠屏展开后导航移至左侧、内容限宽居中、设备列表自动并排两列 |

## 快速开始

### 1. 克隆与配置

```bash
git clone https://github.com/iCurrer/qiekj-android.git
cd qiekj-android
```

在项目根目录创建 `local.properties`（该文件已被 gitignore）：

```properties
# Android SDK 路径
sdk.dir=<你的 Android SDK 路径>

# 积分任务签名密钥（不随仓库分发，缺失时积分任务会提示「未配置签名密钥」）
qiekj.appSecret=<android 渠道密钥>
qiekj.alipaySecret=<alipay 渠道密钥>
```

> **为什么密钥不在仓库里**：积分任务要对请求做签名，签名串里带服务端下发的 `appSecret`。
> 该密钥属于第三方服务，不应随源码分发，因此改由 `local.properties` 或同名环境变量注入
> `BuildConfig`。详见 [`app/build.gradle.kts`](app/build.gradle.kts) 顶部注释。

### 2. 构建

```bash
# 需要 JDK 17+ 与 Android SDK（compileSdk 37、build-tools 36.0.0）
./gradlew assembleDebug
```

Windows / PowerShell：

```powershell
$env:JAVA_HOME = "<JDK 路径>"    # 例如 Android Studio 自带的 jbr
.\gradlew.bat assembleDebug --console=plain
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

### 3. 安装

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> debug 包**只保留 `arm64-v8a`**（ML Kit 的 native 模型每架构各一份，全打进去会让 APK 近乎翻倍）。
> 因此 x86 模拟器装不上，真机不受影响。

## 技术栈

| 项 | 版本 |
| --- | --- |
| 语言 / UI | Kotlin `2.3.21` · Jetpack Compose（BOM `2026.08.00`） |
| 构建 | Gradle `9.5.0` · AGP `9.3.3` · KSP `2.3.12` |
| SDK | compileSdk `37` · targetSdk `37` · minSdk `26`（Android 8.0） |
| 网络 / 序列化 | Retrofit `2.11.0` · OkHttp · Moshi `1.15.1` |
| 桌面卡片 | AndroidX Glance `1.2.0` |
| 扫码 | CameraX `1.6.2` + ML Kit 条码识别 `17.3.0`（bundled） |
| AI 助手 | AndroidX AppFunctions `1.0.0-alpha12` |

> 工具链已全链升到 **Android 17（API 37）**。注意 AGP 9.0 起内建 Kotlin 支持，
> 因此本项目**不引入** `org.jetbrains.kotlin.android` 插件。

## 项目结构

```
app/src/main/java/ltd/fky/devicecontrol/
├── MainActivity.kt                应用入口：主题装配、依赖构建、导航壳、全局对话框
├── DeviceControlApplication.kt
├── appfunctions/                  Android 17 AppFunctions（MCP 工具）暴露层
│   ├── AppFunctionModels.kt         边界 DTO（刻意与 data/Models.kt 分离）
│   └── DeviceControlAppFunctions.kt
├── data/                          网络与本地存储
│   ├── AppRepository.kt             接口编排，解锁流程主逻辑
│   ├── DeviceApi.kt                 Retrofit 接口定义
│   ├── Models.kt                    数据模型
│   ├── PointsTaskRunner.kt          积分任务自动化（签名与逐步执行）
│   ├── TokenStore.kt                Token 加密存储（EncryptedSharedPreferences）
│   ├── OrderHistoryStore.kt         本地订单历史
│   ├── DeviceCacheStore.kt          设备列表本地快照（供桌面卡片读取）
│   └── DevicePrefsStore.kt          设备本地偏好：别名 / 收藏 / 隐藏
├── widget/                        桌面卡片（Glance）
│   └── DeviceGlanceWidget.kt        GlanceAppWidget、设备行渲染与点击跳转
└── ui/
    ├── AppViewModel.kt             单一状态容器（StateFlow）：DeviceTab + AppRoute
    ├── AdaptiveLayout.kt           窗口宽度档位判定与页面限宽容器
    ├── ShortcutSupport.kt          桌面快捷方式与 Intent 常量
    ├── screen/                     设备 / 我的（Tab 层）；积分任务 / 使用说明 / 关于（二级页）
    ├── component/                  卡片 / 资产速览条 / 列表行 / 日志窗 / 状态条 / 二级页顶栏
    ├── dialog/                     Token / 订单详情 / 历史订单 / 二次确认 / 设备操作菜单
    └── theme/                      设计系统：色板 / 字体 / 形状 / 主题
```

## 设计说明

### 视觉设计

色板取自 [Anthropic 官方品牌规范](https://github.com/anthropics/skills)（`brand-guidelines`），
并以 Claude 官方编辑器主题的实测值补全 Material 3 角色：

| 用途 | 色值 | 来源 |
| --- | --- | --- |
| `primary` 主色（按钮、选中态） | `#C96442` Terracotta | Claude 编辑器主题 |
| 强调色 | `#D97757` Anthropic Orange | 官方 `brand-guidelines` |
| 页面底色 | `#FAF9F5` Ivory | 官方 `brand-guidelines` |
| 主文字 | `#141413` Near Black | 官方 `brand-guidelines` |
| 次级背景 | `#E8E6DC` Warm Sand | 官方 `brand-guidelines` |
| 次要文字 | `#B0AEA5` Mid Gray | 官方 `brand-guidelines` |

设计取向：**低饱和暖色 + 纸感表面**。卡片用 **1dp 描边**而非阴影区分层级，圆角收敛在 10–14dp，
标题字重压到 Medium 一档，避免高饱和色块与"果冻感"。

**关于动态取色**：`XiaoGuaiTheme` 的 `dynamicColor` 参数**默认为 `false`**。
Material You 会从用户壁纸提取配色，壁纸偏冷色时整个界面会变成紫 / 蓝色，与上述视觉取向冲突。
如需恢复，显式传入 `dynamicColor = true`。

### 界面层级

只有两层，避免"什么都能占一个 Tab"：

| 层 | 内容 | 特征 |
| --- | --- | --- |
| **Tab 层** | 设备控制、我的 | 常驻底部导航；进入频率高、内容可反复浏览 |
| **二级层** | 积分任务执行页、使用说明、关于 | 不占底部导航；带返回按钮，响应系统返回键 |

> 积分任务原先与「设备解锁」平级占一个常驻 Tab，属于**层级错放** ——
> 它是账号级操作，与「历史订单」「资产查询」同级，现已下沉为二级页。

### 大屏与折叠屏适配

Android 16（API 36）起，系统会**忽略**应用声明的方向与尺寸限制，
大屏上不主动适配就会被拉伸。本项目按下述规则自适应，
断点沿用 Material 3 window size class 的 **600 / 840 dp**：

| 宽度档位 | 导航 | 内容 |
| --- | --- | --- |
| Compact（< 600dp） | 底部导航栏 | 整屏，设备列表单列 |
| Medium（600–839dp） | **左侧导航栏** | 限宽居中，设备列表单列 |
| Expanded（≥ 840dp） | **左侧导航栏** | 限宽居中，设备列表**并排两列** |

**导航为什么换到左侧**：平板横置时底部栏离拇指很远，且它会白吃一整条横向空间，
而宽屏恰恰是横向空间最富余的场景。两种形态共用同一套配色令牌，跨断点切换时视觉是连续的。

**内容为什么要限宽**：宽屏上把元素拉到 1200dp 正是「又宽又扁」的根源 ——
一行文字跨越整屏极难扫读，卡片也会变成扁条。因此按内容类型分档限制最大宽度：

| 内容 | 上限 | 理由 |
| --- | --- | --- |
| 设备网格 | `1040dp` | 留出并排两列的余地 |
| 终端日志 | `900dp` | 偏宽的内容，窄了反而难读 |
| 列表与说明 | `760dp` | 阅读舒适区 |
| 扫码取景 | `560dp` | 取景框过大会失去「对准」的聚焦感 |

**实现**：`ui/AdaptiveLayout.kt` 提供 `rememberWindowWidthClass()` 判定档位，
以及 `PageContainer`（Tab 页，可带垂直滚动）与 `PageWidthBox`（二级页，只限宽不干预内容）
两个容器；各页面只需声明自己的宽度档位。

> 断点判定用 `LocalConfiguration.screenWidthDp`，与官方 `material3.adaptive` 的口径一致
> （多窗口 / 分屏下反映的都是**当前窗口**宽度），因此**没有为此引入额外依赖**。
> 若之后需要折叠屏半折叠姿态（`WindowPosture`）或标准的列表-详情双栏
> （`ListDetailPaneScaffold`），再换用该库即可。

### 扫码解锁

「设备」页右上角扫码图标 → 对准设备屏幕上的二维码 → 确认 → 解锁。

**这条路不依赖"历史设备"。** 原先必须先在官方 App 里扫码支付一次，设备才会出现在
`goods/latestUsed` 里；扫码解锁直接从二维码取设备标识，跳过了这一步。

**二维码里是什么** —— 设备屏幕上的二维码内容形如：

```
https://h5.qiekj.com/skip?SN=869451081089869
```

`SN` 是 15 位数字，**符合 IMEI 规范**（Luhn 校验通过），是设备内置通信模组的 IMEI。

**请求参数（关键）** —— 服务端期望的是**从二维码里拆出来的字段**，不是整段 URL。
这对应 H5 扫码页 `pages_skip` 里 `scanCodeInit()` 的分支逻辑：

| 二维码内容 | 请求参数 |
| --- | --- |
| 含 `http` | 取 `?` 之后的 query，提取 `SN` / `IMEI` / `NQT` / `qeykid` |
| 不含 `http` | 视为设备裸码，参数为 `IMEI` |

```http
POST goods/scan/v2
  SN    = 869451081089869      # 或 IMEI / NQT / qeykid
  token = <登录 Token>
```

响应中的 `id` 字段即解锁所需的 `goodsId`，「参数错误」通常意味着传错了整段 URL。

**实现要点**

| 项 | 说明 |
| --- | --- |
| 扫码 | CameraX 取景 + ML Kit 实时识别（bundled 版，不依赖 Google Play 服务） |
| 识别 | `goods/scan/v2` 用宽松的 `Map` 接收响应，兼容服务端字段命名差异 |
| 确认 | 识别成功后弹确认框，确认才解锁（与桌面卡片共用同一条安全边界） |
| 落库 | 解锁后设备写入 `DeviceCacheStore`，桌面卡片与历史列表立即可见 |

> 接口与参数来自对官方 H5 站点（Vue SPA）的静态分析，关键依据是其扫码页里的
> `{ slotKey: "h5_ad_skip", goodsId: this.deviceInfo.id, shopId: this.deviceInfo.shopId }`。

### 桌面卡片

在主屏长按 → 添加小部件 → 找到「小乖生活」，**默认 2×2，可自由拉伸**（含 4×2 / 4×4）。

| 行为 | 说明 |
| --- | --- |
| 展示内容 | 历史设备的本地快照（App 最近一次拉取的结果） |
| 点设备 | 打开 App 并弹出确认框，**确认后**才真正解锁 |
| 数据同步 | App 刷新设备列表成功后，主动推送刷新卡片 |
| 尺寸适配 | 高度不足 200dp 时只渲染 2 行，避免被 RemoteViews 裁切 |

**为什么卡片自己不去取数据** —— widget 运行在 RemoteViews 宿主中，无法可靠地做网络请求。
因此 App 在 `latestDevices()` 成功后把结果写入 `DeviceCacheStore`（SharedPreferences + Moshi），
卡片只读这一份本地快照。

**为什么点击不直接解锁** —— 解锁会阻塞数十分钟（`do { delay(1000); syncWater() } while (workStatus == 2)`）
并产生真实扣费，误触代价是真金白银。因此卡片只负责"打开 App 并定位到该设备"，
真正的执行必须由用户在 App 内二次确认。

**兼容性说明** —— Glance `1.2.0` 的 POM 声明 `compose-runtime 1.7.8` / `ui-graphics 1.6.0`，
而本项目 BOM 为 `2026.08.00`（compose-runtime 实际解析为 **1.12.0**），属**跨代混用**，
已通过「字节码符号比对 + 真机实测」双重验证可用。

> ⚠️ Glance 的 AAR 是**已编译字节码**，跨代问题在**编译期不会报错**，只会在运行时暴露为
> `NoSuchMethodError`。这类依赖升级必须做运行时验证，不能只看编译是否通过。

### 设备列表自定义

| 能力 | 说明 |
| --- | --- |
| **别名** | 本地改名，如把"3楼饮水机"改成"我的小蓝杯" |
| **收藏** | 置顶显示，名称前带星标 |
| **隐藏** | 从主列表收起；底部「已隐藏 N 台 · 管理」可逐台恢复 |

入口在设备卡右侧的 **⋮** 按钮。排序规则：收藏置顶 → 其余保持服务端返回顺序（即最近使用）。

**为什么这些操作都安全** —— 解锁链路唯一的输入是 `goodsId`，`goodsName` 只用于界面显示与
本地订单记录，不参与任何网络请求。所以改名、隐藏、排序都不会影响解锁能否成功。

**存储** —— `DevicePrefsStore`（SharedPreferences + Moshi），**只存本机**，换机或重装会丢失。
服务端没有存放用户自定义名称的地方，想跨设备同步需要另建账号侧存储。

### AppFunctions（Android MCP）

应用对外声明了 4 个可供 AI 助手调用的工具，函数与模型可见的描述均来自 KDoc：

| 工具 | 作用 | 风险 |
| --- | --- | --- |
| `listDevices` | 列出历史共享设备名称 | 只读 |
| `unlockDevice` | 解锁指定设备并返回订单结算 | **产生真实订单与扣费，不可撤销** |
| `queryBalance` | 查询小票余额、积分余额与可抵扣金额 | 只读 |
| `runPointsTask` | 执行每日签到与积分任务 | 只影响本账号积分 |

**实现要点**

- `appfunctions/DeviceControlAppFunctions.kt`：抽象类 `BaseXiaoGuaiAppFunctionService`，用
  `@AppFunctionServiceEntryPoint(serviceName, appFunctionXmlFileName)` 声明入口，每个函数标 `@AppFunction`。
  KSP 在编译期生成具体服务类 `XiaoGuaiAppFunctionService` 与元数据 XML。
- `appfunctions/AppFunctionModels.kt`：`@AppFunctionSerializable` 的**边界 DTO**。
  不要把内部数据模型直接暴露给模型，这里刻意与 `data/Models.kt` 分离，只保留对 AI 有意义的字段。
- `res/xml/app_metadata.xml`：应用级描述，说明调用顺序与「解锁会产生真实扣费」这一硬约束。
- KSP 同时产出 `assets/xiaoguai_app_functions.xml`（含全部中文元数据）。

**安全边界** —— 清单中服务声明了 `android.permission.BIND_APP_FUNCTION_SERVICE`，**只有系统能绑定**该服务；
`EXECUTE_APP_FUNCTIONS` 是调用方（AI 助手）需要的权限，App 自身无需申请。

**限制** —— AppFunctions 依赖 `android.app.appfunctions`（平台 API **36+**），在 Android 15 及以下不可用；
`androidx.appfunctions` 目前为 `1.0.0-alpha12`（编译器只发布到 alpha12，运行时已到 alpha13，故两件须锁同版本）。
`unlockDevice` 与 `runPointsTask` 会阻塞到本次会话结束，耗时可能达数十分钟。

## 仓库里没有什么

clone 之后**不会**拿到以下内容，这是有意的：

| 缺失项 | 原因 | 如何补齐 |
| --- | --- | --- |
| 签名密钥（`appSecret`） | 属第三方服务，不随源码分发 | 写入 `local.properties`，见[快速开始](#1-克隆与配置) |
| `local.properties` | 含本机 SDK 路径，已被 gitignore | 自行创建 |
| Android SDK / build-tools | 体积大且与平台绑定 | 用 Android Studio SDK Manager 安装 |
| 构建产物（`build/`、`.gradle/`、`.kotlin/`） | 可重新生成 | `./gradlew assembleDebug` |
| AI 工作记忆与规划文档（`.workbuddy/`） | 开发过程记录，非项目源码 | — |
| 个人 Token / 抓包记录 | 敏感信息 | — |

## 许可与归属

| 项 | 情况 |
| --- | --- |
| **本仓库新增与修改部分** | [MIT](LICENSE)，版权归 iCurrer |
| **上游** [`wzs0512/qiekj-android`](https://github.com/wzs0512/qiekj-android) | ⚠️ **未附任何许可文件**（默认保留所有权利）。本仓库对其中继承的代码**不主张再许可** |
| 最初来源 [`3ryng1um/qiekj`](https://github.com/3ryng1um/qiekj) | 上游 README 声明的流程来源 |
| 设备厂商及其运营方 | 本项目与其**无任何关联**，未获授权、未获背书；相关商标、接口与密钥归各自权利人所有 |
| 第三方依赖 | 见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。除 ML Kit 外均为 Apache-2.0，无 copyleft 传染性许可 |

> **请留意**：MIT 许可仅覆盖作者自己新增与修改的部分。上游代码没有明确许可，
> 如需在商业场景中使用本仓库，请自行评估授权基础。

## 注意事项

- Token 会随手机号重新登录变化，这是平台正常行为；App 会保存最新登录得到的 Token。
- 若积分任务在 Python 脚本里正常、App 中失败：优先查看执行页日志里的 HTTP 状态码、
  响应解析错误，或是否提示「未配置签名密钥」。
- 接口返回结构变化时，可能需要同步更新 `data/Models.kt`、`data/DeviceApi.kt` 或 `data/PointsTaskRunner.kt`。
- `unlockDevice`、积分任务均为**真实产生效果**的操作，接入 AI 助手时请留意确认环节。
