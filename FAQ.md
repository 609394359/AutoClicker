# 常见问题（FAQ）

## Q1：点了"开始"按钮，没有任何反应？
逐项检查：
1. 是否已开启 **无障碍服务**？（设置 → 辅助功能 → 已下载的服务 → 自动点击器）
2. **悬浮窗权限**是否已授予？未授予时仅后台运行，无可见反馈。
3. **X、Y 坐标**是否填写了正整数？空 / 0 会拒绝启动。
4. **点击次数**填了 0 才是无限循环，其它正整数是限定次数。
5. 部分品牌会在「最近任务」中冻结后台服务，把应用加入「电池白名单」。

## Q2：每次点击的位置有偏差（偏上 / 偏左）？
- 一般是高 DPI 设备上坐标被等比缩放。本应用以"屏幕宽 / 高百分比"换算，若你手动填的是像素，请保留和原始设备相近的参考尺寸。
- 部分 App 是基于 View 树绘制的，响应在无障碍事件里可能存在 50~100ms 抖动。

## Q3：坐标怎么准确获取？
详见 `README.md` 第 3 节："系统设置 → 开发者选项 → 指针位置"。这是 Android 自带工具，误差为 0。

## Q4：为什么在一些 App（如银行类）中完全无效？
部分 App 调用了 `FLAG_SECURE` 标记，禁止截图与无障碍读取——这是 Android 系统级保护，无法绕过，**这不是本应用的 bug**。

## Q5：可以录制多点 / 滑动吗？
MVP 不支持。当前只支持"一个固定坐标，单击循环"。录制回放在 V2 中提供。

## Q6：服务被系统回收怎么办？
- 关闭「电池优化」
- 进入「最近任务」下拉 → 锁定本应用
- 在「电池 → 后台高耗电」中允许

我们已在 Service 中以 `FOREGROUND_SERVICE` 保持前台通知，大幅降低被回收概率，但极端省电策略下仍可能掉。

## Q7：Android 14 上能否使用？
可以。本应用使用 `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` 类型，targetSdk = 34。
如出现"前台服务启动失败"日志，请确认系统设置 / 制造商策略未拦截。

## Q8：APK 安装时被拦截？
开启「未知来源安装」/「外部来源应用」即可。
- MIUI / HyperOS：设置 → 隐私保护 → 特殊权限 → 安装未知应用
- 鸿蒙：设置 → 安全 → 更多安全设置 → 安装未知应用
- 三星：设置 → 应用 → 特殊访问 → 安装未知应用

## Q9：怎样修改"抖动"幅度？
在 `model/ClickConfig.kt` 中将 `+/-15%` 改成你需要的值（建议在 5%~30% 之间）。

## Q10：能导出 / 分享脚本吗？
MVP 不支持。下个版本会加入 JSON 导入 / 导出。

## Q11：能后台启动 / 跟随某个 App 自动开始？
未来计划：监听"前台 App"切换，匹配包名后自动启动。本期未实现。

## Q12：编译报错 "SDK location not found"
在项目根目录创建 `local.properties`，写入：
```
sdk.dir=C\:\\Users\\<你的用户>\\AppData\\Local\\Android\\Sdk
```
macOS / Linux 用户将路径改为：
```
sdk.dir=/Users/<你的用户>/Library/Android/sdk
```

## Q13：报错 "Manifest does not start with the correct package"
通常是因为 IDE 写入了无效的 namespace。确保 `app/build.gradle.kts` 中 `namespace = "com.autoclicker.app"`，
并且 `AndroidManifest.xml` 顶层 `<manifest>` 没有 `package="..."` 属性。

## Q14：Compose Preview 出现红屏
- 检查 `composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }` 是否与 Kotlin 1.9.22 匹配
- Android Studio Hedgehog+ 通常会自动填充；若报错可执行 *File → Invalidate Caches* 后重试

## Q15：是否会上传数据 / 后门？
- 不会。本应用网络权限未声明（**Manifest 中无 `INTERNET` 权限**），无从联网。
- 所有坐标、间隔、点击次数全部保存在本地 SharedPreferences。
- 你可以查看完整源码 (`com.autoclicker.app.*`) 自行确认。

## Q16：构建报 "Incompatible Gradle JVM version"？
本项目使用 **Gradle 9.2.0** + **AGP 8.12.0**。Gradle 9.2 当前**只支持 Java 17-24**，因此用 Java 25 的 IDE 必须配合 JDK 21/24 运行 Gradle。

| 情况 | 解决方案 |
|------|----------|
| 我的 IDE 是 JDK 25，但我不想改 IDE | 在 `gradle.properties` 中取消注释：`org.gradle.java.home=C\\:/Program Files/Java/jdk-21`（冒号前双反斜杠） |
| 只用命令行 | 执行前设置 `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` |
| 想永久改 IDE | `Settings → Build, Execution, Deployment → Build Tools → Gradle`，把 *Gradle JDK* 改为 21/24 |
| 没装 JDK 21/24 | 下载 Temurin 21：<https://adoptium.net/temurin/releases/?version=21>，解压到 `C:\Program Files\Java\jdk-21` |
| 旧版 IDE (Iguana/Hedgehog) 不识别 Gradle 9 | 升级到 Android Studio **Ladybug (2024.2.1) 及以上** |
| IDE 卡在旧 wrapper | 删除 `.gradle/` 与 `.idea/`，重新打开触发 wrapper 下载 Gradle 9.2 |

> Gradle 9.2 首次同步会下载约 160MB 的发行包。

## Q17：升级后报 "Compose Compiler / AGP / Kotlin 版本不匹配"
Gradle 9.2 必须搭配 AGP 8.12+，本项目已锁定 AGP 8.12.0。如 IDE 仍提示旧版本，执行 *File → Invalidate Caches → Invalidate and Restart*，然后 *Build → Clean Project*。

## Q18：QA - 我能不能直接用 Java 25 构建？
目前**不能**。Gradle 9.2 上限 Java 24。Java 25 完整官方支持需等待 Gradle 9.4+（按官方 roadmap 推测 2026 中后期）。在那之前，配合 JDK 21/24 是行业标准做法。AGP 编译目标保持 `JavaVersion.VERSION_17`，运行时无影响。

## Q19：构建报 "contains AndroidX dependencies, but the `android.useAndroidX` property is not enabled"？
本项目所有依赖（Compose、Lifecycle、Activity 等）都是 AndroidX 库，必须启用 AndroidX。已在 `gradle.properties` 中配置：

```properties
android.useAndroidX=true
```

如果你是从旧项目（支持库时代）迁移过来的，同时还需要 Jetifier 做依赖转换：

```properties
android.enableJetifier=true
```

本项目的依赖全部为原生 AndroidX，**不需要** Jetifier。

## Q20：构建末尾提示 "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10"？
这是一条**非阻断性警告**，构建本身仍然成功。本项目已移除所有已知的 Gradle 9 弃用项：

| 已清理的弃用项 | 原因 | 替代 |
|----------------|------|------|
| `org.gradle.configureondemand=true` | Gradle 9 废弃，Gradle 10 移除 | 已删除（默认行为不变） |
| `android.defaults.buildfeatures.buildconfig=false` | AGP 9 移除的全局属性 | 已在 `app/build.gradle.kts` 模块级显式声明 |
| `kotlinOptions { ... }` 代码块 | Gradle 10 移除 | 已改为顶层 `kotlin { compilerOptions { ... } }` |

如果警告仍出现，说明来源是某个第三方插件（如 AGP、Kotlin 插件自身）内部 API，需要等其上游更新，不影响当前使用。可用下面命令精确定位：

```bash
.\gradlew.bat assembleRelease --warning-mode all
```

## Q21：compileDebugKotlin 报 "Compilation error" 但 `e:` 后面是空的？
这是 **Kotlin 编译器崩溃**（不是源码错误）。根因：Kotlin Gradle Plugin 与 Gradle 版本不兼容。本项目曾用 Kotlin 1.9.22 + Gradle 9.2 —— KGP 1.9.x 最高只支持 Gradle 8.x，跑在 9.x 上编译器直接崩溃，产生空错误消息。

官方兼容矩阵（KGP ↔ Gradle）：

| Kotlin / KGP 版本 | 支持的 Gradle 范围 |
|---|---|
| 1.9.22 | 6.8.3 – 8.1（旧） |
| 2.1.x | 7.6.3 – 8.12.1 |
| 2.2.x | 7.6.3 – 8.14 |
| **2.3.20 – 2.3.21** | 7.6.3 – **9.3.0** |
| 2.4.x | 7.6.3 – 9.5+ |

本项目现使用 **Kotlin 2.3.21 + Gradle 9.2.0 + AGP 8.12.0**，完全兼容。

注意：升到 Kotlin 2.x 后 Compose 编译器改为 Gradle 插件形式（`org.jetbrains.kotlin.plugin.compose`），`composeOptions { kotlinCompilerExtensionVersion = ... }` 配置已废弃并删除。
