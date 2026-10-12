# 自动点击器（Auto Clicker）— MVP

一款运行在 Android 12+ 设备上的极简自动点击工具，支持：

- 自定义点击坐标（X、Y）
- 自定义点击间隔（毫秒）
- 无限循环 / 指定次数
- 悬浮窗一键控制（开始 / 停止 / 关闭）
- 随机抖动（±15%）防止被检测

> 本仓库为 **MVP** 版本，专注于"设坐标 → 自动循环点击"一条主链路的可用性。
> 后续迭代规划：多点脚本、录制回放、滑动与长按、定时触发、脚本导入导出。

---

## 1. 环境要求

| 工具 | 版本 |
|------|------|
| Android Studio | **Ladybug (2024.2.1) 及以上** |
| Android Gradle Plugin | 8.12.0 |
| Gradle | **9.2.0**（wrapper 自动下载） |
| Kotlin | **2.3.21**（KGP 2.3.20+ 才支持 Gradle 9.2） |
| Compose 编译器 | 随 Kotlin 插件自动解析（`org.jetbrains.kotlin.plugin.compose`） |
| compileSdk | 34 |
| minSdk | 31（Android 12） |
| JDK（构建时） | **17 / 21 / 24**（Gradle 9.2 仅支持到 Java 24） |
| JDK（你机器上可以装） | 25 没问题，Gradle 不直接用它跑 |

> ⚠️ **"Incompatible Gradle JVM version" 反复出现？Java 25 用户必读**
>
> Gradle 9.2.0 目前**只支持到 Java 24**。如果你的 IDE / 系统把 Gradle 锁在 JDK 25，就会一直报上面那个错。
>
> **有三种办法可以无视 IDE 配置强制 Gradle 用 JDK 21/24：**
>
> 1. **推荐（项目级强制）**：在 `gradle.properties` 里取消注释并填写本地 JDK 21/24 路径：
>    ```properties
>    org.gradle.java.home=C\\:/Program Files/Java/jdk-21
>    ```
>    *这会让 Gradle 启动时无视 IDE 的 JDK 选择，直接用你写的 JDK。*
>
> 2. **环境变量（仅命令行）**：
>    ```bash
>    $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
>    .\gradlew.bat assembleRelease
>    ```
>
> 3. **IDE 设置**：在 `Settings → Build, Execution, Deployment → Build Tools → Gradle`
>    把 *Gradle JDK* 改为 21 / 24。
>
> 不需要重新装 JDK 21，从下面任一下载即可：
> - **Adoptium Temurin 21**：<https://adoptium.net/temurin/releases/?version=21>
> - **Microsoft OpenJDK 21**：<https://learn.microsoft.com/java/openjdk/download>
> - **Azul Zulu 21**：<https://www.azul.com/downloads/?package=jdk>
>
> 解压后像 `C:\Program Files\Java\jdk-21.0.5` 这样的目录即可。

---

## 2. 快速开始

```bash
# 1) 在 Android Studio 中打开本项目，等待首次 Gradle 同步
# 2) 真机或模拟器（Android 12+）连接电脑
# 3) 点击 Run ▶，安装调试包并启动应用
```

### 直接构建 Release APK（无需 Android Studio）

```bash
# 在项目根目录执行
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

> 首次构建若提示 `SDK location not found`，请在项目根目录创建 `local.properties`，写入：
>
> ```
> sdk.dir=C\:\\Users\\<你的用户>\\AppData\\Local\\Android\\Sdk
> ```

---

## 3. 使用步骤

1. 打开应用 → 点击「权限设置」
2. 按引导开启 **无障碍服务**（*自动点击器服务*）
3. 按引导开启 **悬浮窗权限**（*显示在其他应用上层*）
4. 返回主页，输入或调整：
   - **X / Y 坐标**
   - **点击间隔（毫秒）**：默认 200ms
   - **点击次数**：0 表示无限循环
5. 点击「开始」
6. 应用退到后台后，悬浮窗仍可见，可在那里开始 / 停止 / 关闭

### 3.1 如何获取准确的坐标？

最快的方法：
1. 进入系统 **设置 → 关于手机 → 开发者选项**
2. 打开 **指针位置**
3. 手指按住屏幕中央顶部的状态栏往下拖，会显示坐标
4. 在目标位置上点击，再回到应用输入该坐标

> 注：本 MVP 不内置"屏幕取点器"是因为高 DPI 设备上看到的坐标就是真实的屏幕像素，使用系统指针位置是最稳的。

---

## 4. 权限说明

| 权限 | 用途 | 是否必需 |
|------|------|----------|
| `BIND_ACCESSIBILITY_SERVICE` | 注入点击手势 | ✅ 必需 |
| `SYSTEM_ALERT_WINDOW` | 显示悬浮窗 | ✅ 推荐 |
| `POST_NOTIFICATIONS` | 显示"运行中"通知（Android 13+） | ⚠️ 可选 |
| `FOREGROUND_SERVICE` (+ `_SPECIAL_USE`) | 防止被系统回收 | ✅ 必需 |

---

## 5. 项目结构

```
.
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/autoclicker/app/
│       │   ├── AutoClickApplication.kt
│       │   ├── MainActivity.kt
│       │   ├── model/ClickConfig.kt
│       │   ├── service/AutoClickService.kt
│       │   ├── floating/
│       │   │   ├── FloatingController.kt
│       │   │   └── FloatingPanel.kt
│       │   ├── ui/
│       │   │   ├── HomeScreen.kt
│       │   │   ├── HomeViewModel.kt
│       │   │   ├── PermissionGuideScreen.kt
│       │   │   └── theme/
│       │   └── util/
│       │       ├── CoordinateHelper.kt
│       │       ├── PermissionUtils.kt
│       │       └── PrefsManager.kt
│       └── res/  (themes, strings, icons, xml)
├── keystore/                  # 你的发布签名（可选）
├── ACCESSIBILITY_GUIDE.md     # 各品牌无障碍开启教程
├── FAQ.md                     # 常见问题
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── README.md
```

---

## 6. 自定义发布签名（可选）

默认 `./gradlew assembleRelease` 使用 debug 签名，方便快速分发。
如需正式签名，请参考 `app/build.gradle.kts` 中的注释：生成 keystore、填写 `keystore/release.properties`、启用 `signingConfigs.release`。

---

## 7. 已知限制 / 后续迭代

- 不支持多点 / 滑动 / 长按（MVP 之外）
- 部分银行类 App 检测无障碍服务，主动拦截，详见 FAQ
- Android 14 上前台服务类型必须声明；本应用使用 `SPECIAL_USE`，部分 OEM 仍可能限制
- 屏幕取点仅以系统指针位置代替，未提供可视化十字准星

---

## 8. 免责声明

本工具仅供自动化测试、个人游戏辅助等合法用途。请遵守 App 平台的服务条款与当地法律法规，开发者不对滥用行为负责。
