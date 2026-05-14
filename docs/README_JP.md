<div align="center"><img src="https://raw.githubusercontent.com/187J3X1-114514/superresolution/refs/heads/multi-version/common/src/main/resources/assets/super_resolution/logo.png" width="256"/></div>
<div align="center"><img src="https://img.shields.io/github/forks/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/stars/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/license/187J3X1-114514/superresolution"/>
<img src="https://img.shields.io/github/issues/187J3X1-114514/superresolution"/></div>

<div align="center">
<h1>Super Resolution</h1>
<a href="README_EN.md">English</a> / <a href="README_JP.md">日本語</a> / <a href="README_ZH.md">简体中文</a>
</div>

----

Minecraft に超解像アルゴリズムを組み込み、パフォーマンス/画質を向上させます

# 対応アルゴリズム

* FSR1
* FSR2
* SGSR2
* SGSR1
* DLSS
* XeSS
* FSR3

# その他の機能

* シェーダーパック内の超解像対応、[ドキュメント](https://github.com/187J3X1-114514/superresolution/wiki/Shaderpack-Interface-documentation)

# 互換性

* Sodium 正常に動作
* Iris 正常に動作
* Distant Horizons 正常に動作
* Embeddium 正常に動作
* Voxy 正常に動作
* OptiFine テストなし

# 要件

## システム要件

* Windows 10/11 x64
* Linux x64
* Arm64 macOS 対応予定

### Android デバイスについて

現在 Android デバイスでの実行はサポートされていませんが、Android 用のネイティブライブラリが提供されています（正常に読み込まれません）

そのほかに、Android デバイスの各 OpenGL トランスレーションレイヤーの計算シェーダー、DSA、SPIR-V シェーダーバイナリなどの機能は部分的に正常に動作しませんが、SuperResolution はこれらの機能を使用しない場合は正常に動作します

## グラフィックカード要件

### 推奨

* OpenGL バージョン 4.3 以上対応
* OpenGL 拡張 `GL_ARB_direct_state_access` `GL_ARB_gl_spirv` `GL_ARB_clear_texture` 対応
* Vulkan バージョン >= 1.2 対応

### 最小

* OpenGL バージョン 4.1 以上対応

# 問題がある?

* バグを発見した
* ゲームがクラッシュした
* 他のゲームバージョンをサポートしてほしい _注: 1.18 以上のみ、ローダーは Forge、Fabric、NeoForge のみ、移植難度に応じて対応します_

[ここ](https://github.com/187J3X1-114514/superresolution/issues)で Issue を開いてください

# ビルド

まず C++ 依存ライブラリをコンパイルし、`native:buildNative` タスクを実行します
> 注: Windows プラットフォームは MinGW と Cmake が必要です。その他の要件は[ここ](native/README.md)を参照してください
>
ターミナルを開いて実行すると、build_jars が MOD ファイルになります

```shell
git clone https://github.com/187J3X1-114514/superresolution
cd superresolution
./gradlew buildAllVersions
```

---

# ライセンス

* MOD 本体は GPL-3.0 を使用
* ネイティブライブラリは MIT を使用
* このソフトウェアには NVIDIA Corporation が提供するソースコードが含まれています

## スター履歴

[![Stargazers over time](https://starchart.cc/187J3X1-114514/superresolution.svg?variant=adaptive)](https://starchart.cc/187J3X1-114514/superresolution)


