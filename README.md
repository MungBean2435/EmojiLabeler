# EmojiLabeler 表情标注

> 把手机里的表情包图片快速标注成对应 emoji，标注产物直接对接 PyTorch 训练管线。
> 本项目是「表情包 → emoji 分类模型」训练流程的**数据标注工具**。

## ✨ 功能

- 📥 **导入**：SAF 选择文件夹，递归扫描 jpg / jpeg / png / webp / gif / bmp
  - 复制模式（默认）：图片复制到 App 私有空间，稳定快速
  - 原地移动模式：省空间，SAF 持久授权
- 🏷️ **标注**：看图 → 点 emoji → 自动下一张，全流程 1 秒内完成
  - 最近使用 emoji 快捷行
  - 多标模式：一张图可标多个 emoji
  - 双击大图缩放看细节（表情包常有文字细节）
  - GIF 动画预览
  - 撤销 / 上一张 / 跳过 / 下一张
- ⚙️ **自定义 emoji 集合**：从 200+ 候选里勾选或直接粘贴，标注页只显示选中的
- 📤 **导出**：按 emoji 建子文件夹（多标签复制进多个文件夹）+ `labels.json` + `stats.json`，直接对接训练
- 🔁 **断点续标**：标注状态实时存本地 JSON，杀进程 / 重启不丢
- 🔎 **复查改错**：缩略图列表，点击改标 / 清除标签
- 🧹 **清空数据**：导入错文件夹后可一键清空重新导入
- 🎨 **可自定义**：图片预览背景色（透明 / 黑 / 白 / 自定义 hex）

## 📦 安装

下载 [Releases](../../releases) 里的 `app-debug.apk`，传到手机安装（需允许「安装未知来源应用」）。

## 🚀 使用流程

1. **导入**：首页 → 导入新文件夹 → 选择表情包文件夹（可含子文件夹）
2. **标注**：看图 → 点下方 emoji → 自动下一张
3. **自定义 emoji**：首页 → 设置 → 添加 / 删除需要的 emoji
4. **导出**：标完后首页 → 导出标注结果
   ```
   export/
     😂/xxx_001.jpg
     😭/xxx_002.jpg
     ...
     labels.json   # {"0":"😂","1":"😭",...}
     stats.json    # 每类数量统计
   ```
5. 把 `export/` 拷到电脑 = 训练用 `dataset_raw/`，对接 PyTorch 训练管线

## 🛠️ 构建

要求：JDK 17 + Android SDK（platform 34）+ 网络

```bash
export ANDROID_HOME=/path/to/android-sdk
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> CI：`.github/workflows/build-apk.yml` 已配置，推送到 GitHub 自动出 debug APK 工件。

## 🧱 技术栈

| 模块 | 方案 |
|---|---|
| 语言 / UI | Kotlin + XML Views |
| 最低版本 | minSdk 26（Android 8.0+），targetSdk 34 |
| 文件访问 | SAF（ACTION_OPEN_DOCUMENT_TREE + takePersistableUriPermission） |
| 状态存储 | 本地 JSON（`state.json`），无数据库依赖 |
| 图片加载 | Coil 2.6.0（缩略图、GIF 动画） |
| 构建 | Gradle Kotlin DSL，AGP 8.5.2，单模块 app |

## 📄 License

[MIT](LICENSE)
