# 短縮 URL サービス
## モジュール構成

- `server`
  - Ktor サーバー
  - API / リダイレクト / 管理画面配信
  - GraalVM Native Image(JVM動作確認なし)
- `admin`
  - Compose Multiplatform (Wasm)

## 前提条件
- GraalVM Java 24

## 通常ビルド

プロジェクト全体のコンパイル。管理画面のWasm含む。

```bash
./gradlew nativeCompile -PserverBuildProfile=dev|prod
```
- ネイティブバイナリ: `server/build/native/nativeCompile/shorturl-*`

## 開発時の起動

```bash
./gradlew :server:nativeRun -PserverBuildProfile=dev|prod
```

環境変数は`AppConfig.kt`を参照
