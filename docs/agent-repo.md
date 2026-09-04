# shorturl 固有ルール

## 構成
- `server`: Ktor。リダイレクト・管理API・管理画面配信
- `admin`: Compose Multiplatform (Wasm) 管理画面
- `build-logic`: `createUser` 等

## ビルド
- 全体: `./gradlew build`
- admin確認: `./gradlew :admin:wasmJsBrowserDevelopmentExecutableDistribution`
- サーバ: `./gradlew :server:run`
- Native: `./gradlew :server:nativeCompile`
- ユーザー作成: `./gradlew :server:createUser -Pusername=<name> -PpasswordHash='<bcrypt-hash>'`

## 前提
- serverは Java 24 + GraalVM Toolchain
- 管理画面既定配信: `admin/build/dist/wasmJs/developExecutable/`（`ADMIN_DIST` 未変更時）
