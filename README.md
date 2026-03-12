# 短縮 URL サービス
## モジュール構成

- `server`
  - Ktor サーバー
  - API / リダイレクト / 管理画面配信 
  - GraalVM Native Image ビルドに対応
- `admin`
  - Compose Multiplatform (Wasm) 製の管理画面です。

## 前提条件
- Java 24
- `server` モジュールは Gradle Toolchain で `Java 24 + GraalVM` を要求します。

## 通常ビルド

プロジェクト全体のコンパイルとテスト:

```bash
./gradlew build
```

`server` は admin 管理画面をビルド時に内包します。`-PserverBuildProfile=dev|prod` で同梱する管理画面アセットと成果物名を切り替えます。

JVM の自己完結 JAR を作る場合:

```bash
./gradlew :server:shadowJar -PserverBuildProfile=prod
```

生成物:

- JVM サーバー成果物: `server/build/libs/server-prod-all.jar`
- 通常 JAR: `server/build/libs/server-prod.jar`

補足:

- `dev` を指定すると `admin` の development webpack 出力を、`prod` を指定すると production webpack 出力を内包します。
- `./gradlew :server:run` の既定プロファイルは `dev` です。
- 実行時に `ADMIN_DIST=/path/to/admin/dist` を指定すると、そのディレクトリを `/admin` と `/fonts` の配信元として埋め込みリソースより優先します。
- `ADMIN_DIST` を付けて `:server:run` / `:server:nativeRun` を実行した場合、Gradle は admin の埋め込み生成を省くため、開発時の待ち時間を減らせます。

## GraalVM Native Image ビルド

前提:

- GraalVM JDK 24
- Native Image が利用可能な状態であること
- `native-image --version` が通ること

```bash
./gradlew :server:nativeCompile -PserverBuildProfile=prod
```

生成物:

- ネイティブバイナリ: `server/build/native/nativeCompile/shorturl-prod(.exe)`

補足:

- `nativeCompile` 実行時に対応する admin アセットも自動ビルドされ、native image のリソースへ埋め込まれます。


## 開発時の起動

JVM サーバーを Gradle から起動:

```bash
./gradlew :server:run
```

ネイティブ実行ファイルをビルドしてそのまま起動する場合:

```bash
./gradlew :server:nativeRun -PserverBuildProfile=dev
./gradlew :server:nativeRun -PserverBuildProfile=prod
```

外部の admin ビルド結果を優先して使う場合:

```bash
ADMIN_DIST=./admin/build/dist/wasmJs/developmentExecutable ./gradlew :server:run
ADMIN_DIST=./admin/build/dist/wasmJs/developmentExecutable ./gradlew :server:nativeRun -PserverBuildProfile=dev
```

本番相当の JVM JAR を作る場合:

```bash
./gradlew :server:shadowJar -PserverBuildProfile=prod
```

開発用 JAR / native binary を作る場合:

```bash
./gradlew :server:shadowJar -PserverBuildProfile=dev
./gradlew :server:nativeCompile -PserverBuildProfile=dev
```

環境変数は`AppConfig.kt`を参照
