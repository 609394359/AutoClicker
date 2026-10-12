# 安卓手机点击器（Auto Clicker）开发计划

> ✅ 已确认：
> - 技术栈：**Kotlin + Jetpack Compose**
> - 功能范围：**MVP（单点连点 + 悬浮控制 + 间隔设置 + 循环）**
> - 最低支持：**Android 12+（API 31）**
> - 交付物：**源码 + Release APK + 详细说明文档**

---

## 一、项目概述

**项目名称**：安卓自动点击器 MVP
**目标**：在 Android 12+ 设备上，提供"设定坐标 → 自动循环点击"的核心能力，操作通过悬浮窗控制。

**功能边界（MVP）**：
- ✅ 设定一个点击坐标（X、Y）
- ✅ 设定点击间隔（毫秒）
- ✅ 设定点击次数（无限 或 指定次数）
- ✅ 悬浮窗一键开始 / 停止
- ✅ 应用内主页：保存 / 加载配置
- ❌ 不包含：录制、多点脚本、滑动、长按（后续迭代）

---

## 二、技术选型（已确认）

| 维度 | 选型 |
|------|------|
| 语言 | Kotlin 1.9+ |
| UI | Jetpack Compose（Material3） |
| 最低 SDK | API 31（Android 12） |
| 目标 SDK | API 34（Android 14） |
| 核心机制 | AccessibilityService + AccessibilityNodeInfo |
| 悬浮窗 | WindowManager + 类型 `TYPE_APPLICATION_OVERLAY` |
| 持久化 | SharedPreferences（MVP 暂不需要 Room） |
| 协程 | Kotlin Coroutines |
| 构建 | Gradle 8.x + AGP 8.x |
| IDE | Android Studio Hedgehog (2023.1.1) 及以上 |

**关键权限清单**：
- `BIND_ACCESSIBILITY_SERVICE`
- `SYSTEM_ALERT_WINDOW`（悬浮窗）
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`（前台保活，Android 14 必填）
- `POST_NOTIFICATIONS`（Android 13+ 通知）

---

## 三、目录结构（计划）

```
anzhuodianji/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/autoclicker/app/
│   │   │   ├── MainActivity.kt                 # 主页（Compose）
│   │   │   ├── ui/
│   │   │   │   ├── HomeScreen.kt               # 主界面（设置坐标、间隔、次数）
│   │   │   │   ├── PermissionGuideScreen.kt    # 权限引导页
│   │   │   │   └── theme/                      # Compose 主题
│   │   │   ├── service/
│   │   │   │   └── AutoClickService.kt         # 无障碍核心服务
│   │   │   ├── floating/
│   │   │   │   ├── FloatingController.kt       # 悬浮窗控制器
│   │   │   │   └── FloatingLayout.kt           # 悬浮窗 Compose 视图
│   │   │   ├── model/
│   │   │   │   └── ClickConfig.kt              # 配置数据类
│   │   │   └── util/
│   │   │       ├── PrefsManager.kt            # SharedPreferences 封装
│   │   │       └── CoordinateHelper.kt        # 坐标换算
│   │   └── res/
│   │       ├── values/strings.xml
│   │       ├── values/themes.xml
│   │       └── xml/accessibility_service_config.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/wrapper/
├── README.md                                    # 使用手册
├── ACCESSIBILITY_GUIDE.md                       # 无障碍授权教程
├── FAQ.md                                       # 常见问题
└── LICENSE
```

---

## 四、任务拆解（开发阶段）

### 阶段 A：项目骨架（Task 1-3）

| ID | 任务 | 验收标准 |
|----|------|----------|
| A1 | 创建 Gradle 项目，配置 `build.gradle.kts`（Compose、Coroutines 依赖） | `./gradlew tasks` 可正常执行 |
| A2 | 配置 `AndroidManifest.xml`（权限、Service 声明、Activity、Application） | 清单文件无 lint 错误 |
| A3 | 创建 `accessibility_service_config.xml` 与 `themes.xml` | XML 校验通过 |

### 阶段 B：配置模型与持久化（Task 4-5）

| ID | 任务 | 验收标准 |
|----|------|----------|
| B1 | 实现 `ClickConfig`（x、y、intervalMs、repeatCount、enabled） | data class + 校验 |
| B2 | 实现 `PrefsManager`（保存 / 读取配置） | 进程重启后能恢复上次的设置 |

### 阶段 C：无障碍服务（Task 6-8）

| ID | 任务 | 验收标准 |
|----|------|----------|
| C1 | 实现 `AutoClickService.onServiceConnected`：加载 `accessibility_service_config` | 服务能被系统识别 |
| C2 | 实现 `ClickExecutor.performClick(x, y)`：通过 `AccessibilityNodeInfo` + `GestureDescription` 派发手势 | 真机中能成功触发一次点击 |
| C3 | 实现循环调度逻辑（协程 + 间隔定时器 + 次数控制 + 暂停 / 停止） | 设置 100ms 间隔点击 10 次，实际为 10 次 |

### 阶段 D：UI 层（任务 9-11）

| ID | 任务 | 验收标准 |
|----|------|----------|
| D1 | `HomeScreen`（Compose）：输入坐标、间隔、次数、保存按钮、状态显示 | 截图见主页 |
| D2 | `PermissionGuideScreen`：检测权限状态并引导用户开启无障碍 / 悬浮窗 | 点击按钮跳转系统设置 |
| D3 | `MainActivity`：组合主页 + 引导页，处理权限回调 | 启动后正确路由 |

### 阶段 E：悬浮窗（任务 12-14）

| ID | 任务 | 验收标准 |
|----|------|----------|
| E1 | `FloatingController.show()`：`WindowManager.addView` 添加 Compose 悬浮窗 | 应用退到后台后悬浮窗仍可见 |
| E2 | 悬浮窗按钮：开始 / 停止 | 点击后服务开始 / 停止点击 |
| E3 | 悬浮窗可拖动、可隐藏 | 长按拖动、双击收起 |

### 阶段 F：装配与调试（任务 15-17）

| ID | 任务 | 验收标准 |
|----|------|----------|
| F1 | `MainActivity` 中：保存配置 → 启动服务 → 显示悬浮窗 全流程贯通 | Demo 录屏通过 |
| F2 | 适配刘海屏 / 三星 OneUI / 鸿蒙（坐标换算） | 三类设备实测通过 |
| F3 | 异常处理：未授权时禁止启动、未开启电池优化时提示 | 不崩溃 |

### 阶段 G：打包交付（任务 18-20）

| ID | 任务 | 验收标准 |
|----|------|----------|
| G1 | 生成 debug keystore，配置签名（debug 签名即可分发） | `app-release.apk` 生成成功 |
| G2 | `README.md`：项目介绍、编译说明、`./gradlew assembleRelease` 命令 | 文档完整 |
| G3 | `ACCESSIBILITY_GUIDE.md`：不同品牌手机的无障碍开启步骤（含华为、小米、OPPO、vivo、三星、Pixel） | 含截图位说明 |
| G4 | `FAQ.md`：覆盖"为什么没有反应"、"坐标偏差"、"被安全软件拦截"等 | 至少 10 条 |
| G5 | `./gradlew assembleRelease` 输出最终 APK 到 `app/build/outputs/apk/release/` | 文件存在 |

---

## 五、风险与应对

| 风险 | 应对 |
|------|------|
| Android 14 前台服务类型限制 | 使用 `mediaPlayback` 兜底，文档说明可在设置中关闭 |
| 厂商无障碍策略不同 | `ACCESSIBILITY_GUIDE.md` 提供逐品牌教程 |
| 坐标偏差（高 DPI 设备） | 使用相对坐标（屏幕百分比）而非绝对像素 |
| 服务被系统回收 | MVP 范围内允许此情况，FAQ 说明"建议保持应用前台运行" |
| APK 安装拦截 | README 注明需开启"未知来源安装" |

---

## 六、交付清单

- ✅ 完整源码（可直接用 Android Studio 打开）
- ✅ Release APK：`app/build/outputs/apk/release/app-release.apk`
- ✅ `README.md`：编译 / 安装 / 运行指南
- ✅ `ACCESSIBILITY_GUIDE.md`：各品牌无障碍授权步骤
- ✅ `FAQ.md`：常见问题排查
- ✅ `LICENSE`（Apache 2.0）

---

## 七、最终确认

上述计划是否可以进入开发阶段？
- ✅ 全部 OK → 回复"开始开发"或"确认"
- 🔧 需要调整 → 指出修改点

> 注：开始开发前，我会在每次大阶段完成后用简短反馈让您掌握进度；MVP 整体预估工作量约 1 个完整开发周期。
