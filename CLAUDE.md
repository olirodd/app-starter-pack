# app-starter-pack

Generic Kotlin Multiplatform library providing reusable infrastructure for any iOS/Android project. Contains zero product-specific knowledge.

## Purpose

Consumed by `the-collectorium` (and any future KMP projects) as a versioned Gradle dependency. Provides the building blocks so domain-layer projects don't have to set up networking, persistence, or analytics from scratch.

## Modules

| Module | What it provides |
|--------|-----------------|
| `:networking` | `HttpClient` interface, `KtorHttpClient` implementation, `RequestDefinition`, `NetworkError` |
| `:persistence` | `DriverFactory` expect/actual — creates the correct SQLDelight `SqlDriver` per platform |
| `:analytics` | `AnalyticsClient`, `ErrorReporter`, `Tracker` (fan-out to multiple providers) |
| `:util` | `DateSerializer` — ISO-8601 serialization via kotlinx.datetime |

## Targets

Every module targets: `androidLibrary`, `iosArm64`, `iosSimulatorArm64`, `jvm` (JVM is included to enable fast unit test runs without a simulator).

## Key design decisions

- **`HttpClientConfig.tokenProvider: (() -> String?)?`** — auth token injection via lambda. The HTTP client never imports auth state; callers wire the lambda at construction time.
- **`KtorHttpClient` is `internal`** — consumers use the `HttpClient` interface and the `createHttpClient(config)` expect/actual factory function.
- **`NetworkError` seals over HTTP-level errors only** — domain layers map these to user-facing errors.
- **A request body is JSON or multipart, never both.** `RequestDefinition.bodyJson` sends `application/json`; `RequestDefinition.multipartBody` (`MultipartBody`: `TextPart`s plus `FilePart`s with bytes, filename and content type) sends `multipart/form-data` for uploads. `KtorHttpClient` refuses a request that sets both with an `IllegalArgumentException`, raised before the request is sent and never wrapped as a `NetworkError`, because it is a programmer error. Don't set a `Content-Type` header for multipart: the content carries its own, boundary included.
- **`Tracker` is not a singleton** — it is DI-injected and takes `List<AnalyticsClient>` + `List<ErrorReporter>` for fan-out. Firebase implementations live in app layers, not here.

## Running tests

```bash
# Fast JVM tests for a single module
./gradlew :networking:jvmTest

# All modules
./gradlew jvmTest
```

## Java version

Requires Java 21. Configured via `org.gradle.java.home` in `gradle.properties` and `jvmToolchain(21)` in each module's build file.

## Module structure

Each module follows the same layout:

```
<module>/
  build.gradle.kts
  src/
    commonMain/kotlin/io/appstarterpack/<module>/   ← interfaces, shared logic
    androidMain/kotlin/io/appstarterpack/<module>/  ← Android-specific actuals
    iosMain/kotlin/io/appstarterpack/<module>/      ← iOS-specific actuals
    jvmMain/kotlin/io/appstarterpack/<module>/      ← JVM actuals (for test runs)
    commonTest/kotlin/io/appstarterpack/<module>/   ← all tests (run on JVM)
```

## Status

- [x] `:networking` — complete and tested
- [ ] `:persistence` — scaffold only
- [ ] `:analytics` — scaffold only
- [ ] `:util` — scaffold only
