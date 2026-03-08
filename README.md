# 短縮 URL サービス
## モジュール構成

- `server`
  - Ktor サーバー
  - API / リダイレクト / 管理画面配信 
  - GraalVM Native Image ビルドに対応
- `admin`
  - Compose Multiplatform (Wasm) 製の管理画面です。

## 前提条件
- Java 21
- `server` モジュールは Gradle Toolchain で `Java 21 + GraalVM` を要求します。

## 通常ビルド

プロジェクト全体のコンパイルとテスト:

```bash
./gradlew build
```

管理画面を本番用アセットとしてビルドする場合:

```bash
./gradlew :admin:wasmJsBrowserProductionWebpack
```

生成物:

- JVM サーバー成果物: `server/build/libs/`
- 管理画面成果物: `admin/build/dist/wasmJs/productionExecutable/`

補足:

- `server` は `ADMIN_DIST` 環境変数、または既定の `admin/build/dist/wasmJs/productionExecutable/` を見て `/admin` を配信します。
- API とリダイレクト機能だけを確認したい場合は、管理画面をビルドしなくてもサーバーは起動できます。

## GraalVM Native Image ビルド

前提:

- GraalVM JDK 21
- Native Image が利用可能な状態であること
- `native-image --version` が通ること

管理画面も同梱して使う場合は、先に Wasm 側をビルドします。

```bash
./gradlew :admin:wasmJsBrowserProductionWebpack
./gradlew :server:nativeCompile
```

生成物:

- ネイティブバイナリ: `server/build/native/nativeCompile/`


## 開発時の起動

JVM サーバーを Gradle から起動:

```bash
./gradlew :server:run
```

環境変数は`AppConfig.kt`を参照
