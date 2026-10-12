# 发布签名（Release Signing）说明

本项目 `app/build.gradle.kts` 默认使用 **debug 签名**构建 Release APK，这样能"开箱即用"，但
不适合正式对外发布（Play Store 等接受任何签名，但企业内部 / 商店分发建议使用独立密钥）。

## 一键生成 release 密钥

```bash
# 在项目根目录执行
keytool -genkey -v \
  -keystore keystore/release.keystore \
  -alias autoclicker \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Auto Clicker, OU=Dev, O=YourOrg, L=City, ST=Province, C=CN"
```

## 配置凭据

在 `keystore/release.properties` 写入（**此文件不要提交到 Git**）：

```
RELEASE_STORE_FILE=release.keystore
RELEASE_STORE_PASSWORD=YOUR_STORE_PASSWORD
RELEASE_KEY_ALIAS=autoclicker
RELEASE_KEY_PASSWORD=YOUR_KEY_PASSWORD
```

> `.gitignore` 已包含 `keystore/release.keystore` 与 `keystore/release.properties`，避免误提交。

## 启用 release signing

打开 `app/build.gradle.kts`，搜索 `signingConfigs.create("release")`，将该块取消注释，
并将 `buildTypes.release.signingConfig` 指向 `signingConfigs.getByName("release")`。

之后执行：
```bash
./gradlew assembleRelease
```

`app-release.apk` 即用你的密钥签名。
