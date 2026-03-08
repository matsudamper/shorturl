# Repository Notes

## Structure

- `server`: Ktor サーバー本体。リダイレクト、管理 API、管理画面配信を担当する。
- `admin`: Compose Multiplatform (Wasm) の管理画面。
- `build-logic`: Gradle のカスタムビルドロジック。`createUser` タスクを提供する。

## Build Commands

- まず全体の検証は `./gradlew build` を使う。
- `/admin` を確認する変更では `./gradlew :admin:wasmJsBrowserDevelopWebpack` も実行する。
- JVM サーバー起動は `./gradlew :server:run`。
- GraalVM Native Image は `./gradlew :server:nativeCompile`。

## Assumptions

- `server` は Gradle Toolchain で `Java 24 + GraalVM` を要求している。
- オフラインや制限付き環境では、Gradle の自動 Toolchain 解決に頼らず GraalVM JDK 24 を事前に用意する前提で考える。
- 管理画面の既定配信先は `admin/build/dist/wasmJs/developExecutable/`。`ADMIN_DIST` を変えない限り、このパスを前提に動作確認する。

## Outputs

- JVM 生成物は `server/build/libs/`。
- 管理画面の本番成果物は `admin/build/dist/wasmJs/developExecutable/`。
- ネイティブバイナリは `server/build/native/nativeCompile/`。

## Useful Task

- 管理ユーザー作成: `./gradlew :server:createUser -Pusername=<name> -Ppassword=<password>`
