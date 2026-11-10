# 차량 앱 뼈대 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 화면 구현 전에, 폴더 경계·핵심 규칙·가짜 서버·시연 컨트롤을 갖춘 빌드 가능한 차량 앱 뼈대를 만든다.

**Architecture:** 단일 `:app` 모듈. 최상위 패키지(`app`·`core`·`data`·`platform`·`feature`·`demo`·`ui`)를 미래 모듈 경계로 쓰고, 금지 import는 Gradle `checkBoundaries` 작업이 빌드마다 검사한다. 저장소·플랫폼은 인터페이스로 두고 구현은 하위 패키지(`data/fake`, `platform/*/fake` 등)에 두며, 선택은 `app/AppContainer`에서만 한다.

**Tech Stack:** Kotlin 2.2.20, AGP 8.13.1, Gradle 8.14.3, Jetpack Compose (BOM 2025.10.01), Coroutines 1.10.2, JUnit 4.13.2, Pleos NaviHelper 3.0.5·Vehicle 2.3.2

## Global Constraints

- applicationId·namespace: `com.pickmedt.car`
- minSdk 28 (Pleos 필수), compileSdk 36, targetSdk 35. 실행 대상은 Pleos Connect 에뮬레이터 API 34
- Pleos Maven: `https://nexus-playground.pleos.ai/repository/maven-releases/`
- JDK: 로컬 JDK 21로 실행, 바이트코드 대상 17
- 화면 순서·문구 기준: 2차 기획안, Figma `prototype` 페이지(V00~V08)
- 제조 타이밍 식(2차 기획 §5): 예상 수령 = 입구 도착 예정 + 입구 이후 시간, 시작 추천 = 예상 수령 − 준비 시간. 제조 시작 후에는 추천을 고정
- ETA 정책(2차 기획 §5): 30초마다 전송, 90초 초과 시 오래됨, 추천 60초 이상 변경 시 강조
- 주행 맥락(2차 기획 §4): 주차 확인일 때만 상세 허용, 속도 0만으로 주차로 보지 않음
- git 명령은 사용자가 직접 실행한다. 각 Task 끝의 「커밋」은 경로별 `git add`와 메시지를 전달하는 단계다

## File Structure

```
pick-me-fe/
├─ settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew
├─ ARCHITECTURE.md                       폴더 구조와 경계 규칙
├─ README.md                             공모전 제출용 (앱 소개·실행 방법·환경·APK 위치)
├─ apk/                                  제출용 release APK
├─ docs/plans/                           구현 계획
└─ app/
   ├─ build.gradle.kts                   checkBoundaries 작업 포함
   └─ src/
      ├─ main/AndroidManifest.xml        차량 전용, 주행 최적화(DO) 선언
      ├─ main/res/values/                strings, colors, themes
      ├─ debug/                          평문 HTTP 허용 (10.0.2.2, localhost)
      ├─ main/kotlin/com/pickmedt/car/
      │  ├─ app/        PickmeApplication, MainActivity, AppContainer, AppRoot, EtaReporter
      │  ├─ core/
      │  │  ├─ model/   Money, Order, OrderStage, PickupTiming, QuickOrder, EtaReading, Drive
      │  │  ├─ rule/    DriveContextRules, EtaRules
      │  │  └─ time/    Clock
      │  ├─ data/
      │  │  ├─ order/   OrderRepository, Quote
      │  │  └─ fake/    FakeServer, FakeOrderRepository, SampleData
      │  ├─ platform/
      │  │  ├─ drive/   DriveSignalSource, fake/FakeDriveSignalSource
      │  │  └─ navi/    EtaSource, scripted/ScriptedEtaSource
      │  ├─ demo/       DemoController, DemoPanel
      │  └─ ui/theme/   PickmeTheme
      └─ test/kotlin/com/pickmedt/car/
         ├─ testing/    MutableClock, TestOrders
         ├─ core/rule/  DriveContextRulesTest, EtaRulesTest
         ├─ data/fake/  FakeServerTest
         ├─ platform/   ScriptedEtaSourceTest, FakeDriveSignalSourceTest
         └─ app/        EtaReporterTest
```

`feature/`(V00~V08 화면)와 `data/remote/`(실서버), `platform/*/car`·`platform/*/pleos`(실차)는 다음 계획에서 만든다.

---

### Task 1: Gradle 뼈대, 경계 검사, 빈 화면 빌드

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `.gitignore`, `local.properties`(git 제외)
- Create: `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/{strings,colors,themes}.xml`
- Create: `app/src/debug/AndroidManifest.xml`, `app/src/debug/res/xml/network_security_config.xml`
- Create: `app/src/main/kotlin/com/pickmedt/car/app/{PickmeApplication,MainActivity}.kt`
- Create: `ARCHITECTURE.md`, `README.md`, `apk/.gitkeep`

**Interfaces:**
- Produces: `PickmeApplication`(Task 6에서 확장), `MainActivity`, Gradle 작업 `checkBoundaries`(`preBuild`에 연결)

- [ ] **Step 1: 설정 파일 작성**

`settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Pleos Connect SDK
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
    }
}

rootProject.name = "pick-me-fe"
include(":app")
```

`build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

`gradle.properties`

```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`gradle/libs.versions.toml`

```toml
[versions]
agp = "8.13.1"
kotlin = "2.2.20"
coreKtx = "1.17.0"
activityCompose = "1.11.0"
lifecycle = "2.9.4"
composeBom = "2025.10.01"
coroutines = "1.10.2"
junit = "4.13.2"
pleosNaviHelper = "3.0.5"
pleosVehicle = "2.3.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit = { module = "junit:junit", version.ref = "junit" }
pleos-navihelper = { module = "ai.pleos.playground:NaviHelper", version.ref = "pleosNaviHelper" }
pleos-vehicle = { module = "ai.pleos.playground:Vehicle", version.ref = "pleosVehicle" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

`.gitignore`

```gitignore
.gradle/
build/
/local.properties
.idea/
*.iml
.DS_Store
/captures
.kotlin/
# release 서명 정보
keystore.properties
*.jks
*.keystore
```

`local.properties` (git 제외)

```properties
sdk.dir=/Users/ijaeseo/Library/Android/sdk
```

- [ ] **Step 2: Gradle 래퍼 생성**

Run: `~/.gradle/wrapper/dists/gradle-8.14.3-bin/cv11ve7ro1n3o1j4so8xd9n66/gradle-8.14.3/bin/gradle wrapper --gradle-version 8.14.3 --distribution-type bin`
Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/` 생성

- [ ] **Step 3: 앱 모듈 빌드 파일 작성 (경계 검사 포함)**

`app/build.gradle.kts`

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pickmedt.car"
    compileSdk = 36
    // AAOS Car API (기어·속도·주행 규제)
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.pickmedt.car"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.vehicle)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

/**
 * 패키지 경계 검사. 금지된 import가 있으면 빌드를 실패시킨다.
 * 규칙의 이유와 표는 ARCHITECTURE.md 2장.
 */
val checkBoundaries by tasks.registering {
    group = "verification"
    description = "패키지 경계 규칙(ARCHITECTURE.md 2장)을 검사한다."
    val sourceRoot = layout.projectDirectory.dir("src/main/kotlin/com/pickmedt/car")
    inputs.dir(sourceRoot)
    doLast {
        val p = "com\\.pickmedt\\.car"
        val sdk = "(ai\\.pleos\\.|android\\.car\\.)"
        val net = "(retrofit2\\.|okhttp3\\.|io\\.socket\\.)"
        // 구현 하위 패키지: data/fake, data/remote, platform/<영역>/<구현>
        val impl = "$p\\.(data\\.(fake|remote)|platform\\.[a-z]+\\.[a-z]+)\\."
        fun pkg(vararg names: String) = "$p\\.(${names.joinToString("|")})\\."
        val rules = mapOf(
            "core" to listOf("(android|androidx)\\.", sdk, net, pkg("data", "platform", "feature", "ui", "demo", "app")),
            "data" to listOf(sdk, pkg("platform", "feature", "ui", "demo", "app")),
            "platform" to listOf(net, pkg("data", "feature", "ui", "demo", "app")),
            "feature" to listOf(sdk, net, impl, pkg("demo", "app")),
            "ui" to listOf(sdk, net, pkg("data", "platform", "feature", "demo", "app")),
            "demo" to listOf(sdk, net, pkg("feature", "app"), "$p\\.data\\.remote\\."),
        )
        val root = sourceRoot.asFile
        val violations = mutableListOf<String>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val rel = file.relativeTo(root).invariantSeparatorsPath
            val forbidden = rules[rel.substringBefore('/')].orEmpty().toMutableList()
            // 네트워크 라이브러리는 data/remote에서만 쓴다
            if (rel.startsWith("data/") && !rel.startsWith("data/remote/")) forbidden += net
            file.readLines().forEachIndexed { index, line ->
                val imported = Regex("^import\\s+([\\w.]+)").find(line.trim())?.groupValues?.get(1) ?: return@forEachIndexed
                if (forbidden.any { Regex("^$it").containsMatchIn("$imported.") }) {
                    violations += "$rel:${index + 1}: ${line.trim()}"
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("패키지 경계 위반 (ARCHITECTURE.md 2장):\n" + violations.joinToString("\n"))
        }
        println("패키지 경계 검사 통과")
    }
}

tasks.named("preBuild") { dependsOn(checkBoundaries) }
```

- [ ] **Step 4: 매니페스트와 리소스 작성**

`app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 차량 전용 앱 -->
    <uses-feature
        android:name="android.hardware.type.automotive"
        android:required="true" />

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".app.PickmeApplication"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Pickme">

        <!-- 분할 창 크기 변경 시 Activity를 다시 만들지 않고 Compose가 레이아웃을 다시 그린다 -->
        <activity
            android:name=".app.MainActivity"
            android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- 주행 중에도 진행 화면을 보여 주기 위한 주행 최적화(DO) 선언. 동작은 게이트 G4에서 확인 -->
            <meta-data
                android:name="distractionOptimized"
                android:value="true" />
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">픽미</string>
</resources>
```

`app/src/main/res/values/colors.xml`

```xml
<resources>
    <!-- Figma V01 캡처 기준 배경색. 토큰 정리는 화면 구현 계획에서 한다 -->
    <color name="pickme_background">#FF1C1D21</color>
</resources>
```

`app/src/main/res/values/themes.xml`

```xml
<resources>
    <style name="Theme.Pickme" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/pickme_background</item>
    </style>
</resources>
```

`app/src/debug/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 로컬 서버(평문 HTTP) 접속은 debug 빌드에서만 허용 (pick-me-be docs/LOCAL_DEV.md 2장) -->
    <application android:networkSecurityConfig="@xml/network_security_config" />
</manifest>
```

`app/src/debug/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
        <domain includeSubdomains="false">localhost</domain>
    </domain-config>
</network-security-config>
```

- [ ] **Step 5: 최소 Application·Activity 작성**

`app/src/main/kotlin/com/pickmedt/car/app/PickmeApplication.kt`

```kotlin
package com.pickmedt.car.app

import android.app.Application

class PickmeApplication : Application()
```

`app/src/main/kotlin/com/pickmedt/car/app/MainActivity.kt`

```kotlin
package com.pickmedt.car.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("픽미") }
    }
}
```

- [ ] **Step 6: 빌드해서 의존성 해석과 경계 검사 통과 확인**

Run: `./gradlew :app:assembleDebug`
Expected: `패키지 경계 검사 통과`, `BUILD SUCCESSFUL`. Pleos SDK 두 개가 해석되어야 한다.

- [ ] **Step 7: 경계 검사가 위반을 잡는지 확인**

`app/src/main/kotlin/com/pickmedt/car/feature/BoundaryProbe.kt`를 잠시 만든다.

```kotlin
package com.pickmedt.car.feature

import ai.pleos.playground.navi.helper.NaviHelper
```

Run: `./gradlew :app:checkBoundaries`
Expected: FAIL, `feature/BoundaryProbe.kt:3: import ai.pleos.playground.navi.helper.NaviHelper`
확인 후 파일을 지우고 다시 실행해 통과를 확인한다.

- [ ] **Step 8: ARCHITECTURE.md, README.md, apk/.gitkeep 작성**

ARCHITECTURE.md는 이 계획의 File Structure, 경계 규칙 표, 구현 선택 위치(AppContainer), 기능 패키지와 Figma 화면 코드 대응을 담는다. README.md는 공모전 요구 항목(앱 이름·한 줄 소개, 실행 방법, 테스트 환경·필요 설정, APK 위치)의 현재 값을 담는다.

- [ ] **Step 9: 커밋 (사용자 실행)**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties gradlew gradlew.bat .gitignore app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res app/src/debug app/src/main/kotlin/com/pickmedt/car/app/PickmeApplication.kt app/src/main/kotlin/com/pickmedt/car/app/MainActivity.kt ARCHITECTURE.md README.md apk/.gitkeep docs/plans/2026-11-10-app-skeleton.md
```

---

### Task 2: 핵심 모델과 규칙 (core)

**Files:**
- Create: `app/src/main/kotlin/com/pickmedt/car/core/time/Clock.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/core/model/{Money,Order,QuickOrder,EtaReading,Drive}.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/core/rule/{DriveContextRules,EtaRules}.kt`
- Test: `app/src/test/kotlin/com/pickmedt/car/testing/{MutableClock,TestOrders}.kt`
- Test: `app/src/test/kotlin/com/pickmedt/car/core/rule/{DriveContextRulesTest,EtaRulesTest}.kt`

**Interfaces:**
- Produces:
  - `fun interface Clock { fun now(): Instant }`, `Clock.System`
  - `@JvmInline value class Money(val won: Int)` + `plus`, `times(Int)`, `format(): String`
  - `enum class OrderStage { AWAITING_STORE, WAITING_TO_PREPARE, PREPARING, READY, PICKED_UP, CANCEL_REQUESTED, CANCELLED }` + `isActive`
  - `data class StoreSummary(id, name)`, `data class OrderLine(menuItemId, name, option: String?, unitPrice: Money, qty: Int)` + `price`
  - `data class PickupTiming(entranceEtaAt, expectedPickupAt, recommendedStartAt: Instant, delaySec: Int, etaReceivedAt: Instant)`
  - `enum class CancelReason { STORE_REJECTED, CANCEL_APPROVED, OTHER }`
  - `data class Order(id, displayNo: String?, stage, store, lines, plateNumber, timing: PickupTiming?, cancelReason: CancelReason?, cancelRejected: Boolean = false)` + `total`
  - `data class QuickOrder(id, store, lines)` + `total`
  - `data class EtaReading(etaSec: Int, measuredAt: Instant, origin: EtaOrigin)`, `enum class EtaOrigin { NAVI, SCRIPTED }`
  - `enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE }`, `data class DriveSignals(gear: Gear?, speedKmh: Float?, uxRestricted: Boolean?)` + `Unknown`
  - `enum class DriveContext { PARKED, DRIVING, STOPPED, UNKNOWN }` + `allowsDetail`
  - `fun resolveDriveContext(signals: DriveSignals): DriveContext`
  - `object EtaPolicy { ReportInterval, StaleAfter, EmphasizeShift }`, `enum class EtaFreshness { NONE, FRESH, STALE }`
  - `fun etaFreshness(receivedAt: Instant?, now: Instant): EtaFreshness`
  - `fun isRecommendationShiftEmphasized(previous: Instant?, next: Instant?): Boolean`
  - 테스트: `MutableClock(current)` + `advance(Duration)`, `kst("08:02")`, `testOrder(stage)`

- [ ] **Step 1: 모델과 Clock 작성**

`core/time/Clock.kt`

```kotlin
package com.pickmedt.car.core.time

import java.time.Instant

/** 현재 시각. 테스트·시연에서 시간을 고정하거나 앞당길 수 있게 주입한다. */
fun interface Clock {
    fun now(): Instant

    companion object {
        val System: Clock = Clock { Instant.now() }
    }
}
```

`core/model/Money.kt`

```kotlin
package com.pickmedt.car.core.model

import java.util.Locale

/** 원 단위 금액 */
@JvmInline
value class Money(val won: Int) {
    operator fun plus(other: Money) = Money(won + other.won)
    operator fun times(qty: Int) = Money(won * qty)

    /** 예: "4,500원" */
    fun format(): String = String.format(Locale.KOREA, "%,d원", won)
}
```

`core/model/Order.kt`

```kotlin
package com.pickmedt.car.core.model

import java.time.Instant

/** 2차 기획 §3의 주문 단계. 서버 상태 이름과의 변환은 data 계층에서 한다. */
enum class OrderStage {
    /** 매장 확인 중 */
    AWAITING_STORE,
    /** 제조 대기 (직원 수락) */
    WAITING_TO_PREPARE,
    /** 제조 중 */
    PREPARING,
    /** 준비 완료 */
    READY,
    /** 수령 완료 */
    PICKED_UP,
    /** 취소 확인 중 */
    CANCEL_REQUESTED,
    /** 취소 완료 (매장 거절 포함) */
    CANCELLED;

    val isActive: Boolean get() = this != PICKED_UP && this != CANCELLED
}

data class StoreSummary(val id: String, val name: String)

data class OrderLine(
    val menuItemId: String,
    val name: String,
    /** 예: "Regular" */
    val option: String?,
    val unitPrice: Money,
    val qty: Int,
) {
    val price: Money get() = unitPrice * qty
}

/** 제조 타이밍 (2차 기획 §5). 서버가 계산한 값을 담는다. */
data class PickupTiming(
    val entranceEtaAt: Instant,
    val expectedPickupAt: Instant,
    /** 제조 시작 후에는 고정된다 */
    val recommendedStartAt: Instant,
    /** 제조 시작 시점보다 수령 예상이 늦어진 시간(초). 제조 전에는 0 */
    val delaySec: Int,
    val etaReceivedAt: Instant,
)

enum class CancelReason { STORE_REJECTED, CANCEL_APPROVED, OTHER }

data class Order(
    val id: String,
    /** 예: "P-021" */
    val displayNo: String?,
    val stage: OrderStage,
    val store: StoreSummary,
    val lines: List<OrderLine>,
    val plateNumber: String,
    val timing: PickupTiming?,
    val cancelReason: CancelReason?,
    /** 직전 취소 요청을 매장이 거절했는지 (Figma V06-E) */
    val cancelRejected: Boolean = false,
) {
    val total: Money get() = lines.fold(Money(0)) { sum, line -> sum + line.price }
}
```

`core/model/QuickOrder.kt`

```kotlin
package com.pickmedt.car.core.model

/** 홈에 보여 주는 등록 매장·메뉴 (2차 기획 §3 자주 주문 메뉴) */
data class QuickOrder(
    val id: String,
    val store: StoreSummary,
    val lines: List<OrderLine>,
) {
    val total: Money get() = lines.fold(Money(0)) { sum, line -> sum + line.price }
}
```

`core/model/EtaReading.kt`

```kotlin
package com.pickmedt.car.core.model

import java.time.Instant

/** 매장 입구까지 남은 시간 측정값 */
data class EtaReading(
    val etaSec: Int,
    val measuredAt: Instant,
    val origin: EtaOrigin,
)

enum class EtaOrigin {
    /** NaviHelper 실측 */
    NAVI,
    /** 시연용 모의 값 */
    SCRIPTED,
}
```

`core/model/Drive.kt`

```kotlin
package com.pickmedt.car.core.model

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE }

/** 차량에서 읽은 원신호. 아직 못 읽었거나 미지원·권한 없음이면 null */
data class DriveSignals(
    val gear: Gear?,
    val speedKmh: Float?,
    /** 플랫폼 주행 규제(CarUxRestrictions)가 걸려 있는지 */
    val uxRestricted: Boolean?,
) {
    companion object {
        val Unknown = DriveSignals(gear = null, speedKmh = null, uxRestricted = null)
    }
}

/** 2차 기획 §4 차량 맥락 */
enum class DriveContext {
    /** 주차 확인 */
    PARKED,
    /** 주행 */
    DRIVING,
    /** 일시 정차 */
    STOPPED,
    /** 상태 불명 */
    UNKNOWN;

    /** 상세 조작(예약·설정·주문 상세)은 주차 확인일 때만 */
    val allowsDetail: Boolean get() = this == PARKED
}
```

- [ ] **Step 2: 테스트 도구 작성**

`test/.../testing/MutableClock.kt`

```kotlin
package com.pickmedt.car.testing

import com.pickmedt.car.core.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class MutableClock(var current: Instant) : Clock {
    override fun now(): Instant = current

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }
}

/** 시연 기준일 한국 시각. 예: kst("08:02") */
fun kst(time: String): Instant =
    LocalDate.of(2026, 11, 14).atTime(LocalTime.parse(time)).atZone(ZoneId.of("Asia/Seoul")).toInstant()
```

`test/.../testing/TestOrders.kt`

```kotlin
package com.pickmedt.car.testing

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.StoreSummary

fun testOrder(stage: OrderStage, id: String = "order-1") = Order(
    id = id,
    displayNo = "P-021",
    stage = stage,
    store = StoreSummary("store-1", "양재 DT점"),
    lines = listOf(OrderLine("menu-1", "아이스 아메리카노", "Regular", Money(4_500), 1)),
    plateNumber = "12가3456",
    timing = null,
    cancelReason = null,
)
```

- [ ] **Step 3: 규칙 테스트 작성 (실패 확인용)**

`test/.../core/rule/DriveContextRulesTest.kt`

```kotlin
package com.pickmedt.car.core.rule

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveContextRulesTest {
    @Test fun `기어를 모르면 상태 불명`() =
        assertEquals(DriveContext.UNKNOWN, resolveDriveContext(DriveSignals.Unknown))

    @Test fun `P단이고 주행 규제가 없으면 주차 확인`() =
        assertEquals(DriveContext.PARKED, resolveDriveContext(DriveSignals(Gear.PARK, 0f, false)))

    @Test fun `P단이고 주행 규제를 모르면 기어를 따라 주차 확인`() =
        assertEquals(DriveContext.PARKED, resolveDriveContext(DriveSignals(Gear.PARK, null, null)))

    @Test fun `P단이어도 플랫폼이 주행 규제를 걸면 주차로 보지 않는다`() =
        assertEquals(DriveContext.STOPPED, resolveDriveContext(DriveSignals(Gear.PARK, 0f, true)))

    @Test fun `D단 속도 0은 일시 정차`() =
        assertEquals(DriveContext.STOPPED, resolveDriveContext(DriveSignals(Gear.DRIVE, 0f, true)))

    @Test fun `D단 속도를 모르면 일시 정차`() =
        assertEquals(DriveContext.STOPPED, resolveDriveContext(DriveSignals(Gear.DRIVE, null, null)))

    @Test fun `D단 속도가 있으면 주행`() =
        assertEquals(DriveContext.DRIVING, resolveDriveContext(DriveSignals(Gear.DRIVE, 30f, true)))

    @Test fun `R단 속도가 있으면 주행`() =
        assertEquals(DriveContext.DRIVING, resolveDriveContext(DriveSignals(Gear.REVERSE, 5f, true)))

    @Test fun `상세 조작은 주차 확인일 때만 허용`() {
        assertTrue(DriveContext.PARKED.allowsDetail)
        assertFalse(DriveContext.DRIVING.allowsDetail)
        assertFalse(DriveContext.STOPPED.allowsDetail)
        assertFalse(DriveContext.UNKNOWN.allowsDetail)
    }
}
```

`test/.../core/rule/EtaRulesTest.kt`

```kotlin
package com.pickmedt.car.core.rule

import com.pickmedt.car.testing.kst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EtaRulesTest {
    private val now = kst("08:05")

    @Test fun `수신 기록이 없으면 NONE`() =
        assertEquals(EtaFreshness.NONE, etaFreshness(null, now))

    @Test fun `90초까지는 최신`() =
        assertEquals(EtaFreshness.FRESH, etaFreshness(now.minusSeconds(90), now))

    @Test fun `90초를 넘으면 오래됨`() =
        assertEquals(EtaFreshness.STALE, etaFreshness(now.minusSeconds(91), now))

    @Test fun `추천이 59초 바뀌면 강조하지 않는다`() =
        assertFalse(isRecommendationShiftEmphasized(kst("08:07"), kst("08:07").plusSeconds(59)))

    @Test fun `추천이 60초 이상 늦어지면 강조`() =
        assertTrue(isRecommendationShiftEmphasized(kst("08:07"), kst("08:08")))

    @Test fun `추천이 60초 이상 당겨져도 강조`() =
        assertTrue(isRecommendationShiftEmphasized(kst("08:07"), kst("08:06")))

    @Test fun `이전 추천이 없으면 강조하지 않는다`() =
        assertFalse(isRecommendationShiftEmphasized(null, kst("08:07")))
}
```

- [ ] **Step 4: 테스트 실행해 실패 확인**

Run: `./gradlew :app:testDebugUnitTest`
Expected: FAIL, `Unresolved reference: resolveDriveContext`, `etaFreshness` 등 컴파일 오류

- [ ] **Step 5: 규칙 구현**

`core/rule/DriveContextRules.kt`

```kotlin
package com.pickmedt.car.core.rule

import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear

/**
 * 2차 기획 §4: 주차 확인일 때만 상세를 허용한다.
 * - 기어를 모르면 상태 불명
 * - P단이어도 플랫폼 주행 규제가 걸리면 규제를 따른다
 * - 속도 0만으로는 주차로 보지 않는다 (일시 정차)
 */
fun resolveDriveContext(signals: DriveSignals): DriveContext {
    val gear = signals.gear ?: return DriveContext.UNKNOWN
    if (gear == Gear.PARK && signals.uxRestricted != true) return DriveContext.PARKED
    return if ((signals.speedKmh ?: 0f) > 0f) DriveContext.DRIVING else DriveContext.STOPPED
}
```

`core/rule/EtaRules.kt`

```kotlin
package com.pickmedt.car.core.rule

import java.time.Duration
import java.time.Instant

/** 2차 기획 §5 ETA 정책 */
object EtaPolicy {
    /** 차량 앱이 ETA를 보내는 주기 */
    val ReportInterval: Duration = Duration.ofSeconds(30)

    /** 이 시간을 넘기면 오래됨 */
    val StaleAfter: Duration = Duration.ofSeconds(90)

    /** 추천 시각이 이만큼 이상 바뀌면 강조 */
    val EmphasizeShift: Duration = Duration.ofSeconds(60)
}

enum class EtaFreshness { NONE, FRESH, STALE }

fun etaFreshness(receivedAt: Instant?, now: Instant): EtaFreshness = when {
    receivedAt == null -> EtaFreshness.NONE
    Duration.between(receivedAt, now) > EtaPolicy.StaleAfter -> EtaFreshness.STALE
    else -> EtaFreshness.FRESH
}

fun isRecommendationShiftEmphasized(previous: Instant?, next: Instant?): Boolean {
    if (previous == null || next == null) return false
    return Duration.between(previous, next).abs() >= EtaPolicy.EmphasizeShift
}
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (DriveContextRulesTest 9개, EtaRulesTest 7개)

- [ ] **Step 7: 커밋 (사용자 실행)**

```bash
git add app/src/main/kotlin/com/pickmedt/car/core app/src/test/kotlin/com/pickmedt/car/testing app/src/test/kotlin/com/pickmedt/car/core
```

---

### Task 3: 주문 저장소와 가짜 서버 (data)

**Files:**
- Create: `app/src/main/kotlin/com/pickmedt/car/data/order/{OrderRepository,Quote}.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/data/fake/{SampleData,FakeServer,FakeOrderRepository}.kt`
- Test: `app/src/test/kotlin/com/pickmedt/car/data/fake/FakeServerTest.kt`

**Interfaces:**
- Consumes: Task 2의 `Order`, `OrderStage`, `QuickOrder`, `PickupTiming`, `CancelReason`, `EtaReading`, `Money`, `Clock`
- Produces:
  - `interface OrderRepository { val currentOrder: StateFlow<Order?>; suspend fun quickOrders(): List<QuickOrder>; suspend fun quote(quickOrderId: String): Quote; suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order; suspend fun reportEta(orderId: String, reading: EtaReading); suspend fun requestCancel(orderId: String); suspend fun refresh(); fun clearFinished() }`
  - `data class Quote(quickOrderId, store, lines, total: Money, issue: QuoteIssue?)`, `enum class QuoteIssue { SOLD_OUT, PRICE_CHANGED, STORE_CLOSED }`
  - `class FakeServer(clock, afterEntrance = 2분, prepTime = 3분, firstDisplayNo = 21, plateNumber)`: 차량 쪽 `quote`, `reserve`, `reportEta(orderId, etaSec)`, `requestCancel`; 매장 쪽 `accept`, `reject`, `startPreparing`, `markReady`, `pickUp`, `approveCancel`, `rejectCancel`; `reset`; `order: StateFlow<Order?>`; `quickOrders`
  - `class FakeOrderRepository(server: FakeServer) : OrderRepository`
  - `object SampleData { PLATE_NUMBER, yangjae, usualIcedAmericano }`

- [ ] **Step 1: 인터페이스 작성**

`data/order/OrderRepository.kt`

```kotlin
package com.pickmedt.car.data.order

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.QuickOrder
import kotlinx.coroutines.flow.StateFlow

/**
 * 주문 저장소. 화면은 이 인터페이스만 쓰고, 구현(가짜·실서버)은 AppContainer가 고른다.
 * 상태의 기준은 서버다 (2차 기획 §3: 재연결 시 로컬 화면이 아닌 서버 상태를 조회).
 */
interface OrderRepository {
    /** 진행 중이거나 방금 끝난(수령·취소 완료) 주문. 없으면 null */
    val currentOrder: StateFlow<Order?>

    /** 홈에 보여 줄 등록 매장·메뉴 */
    suspend fun quickOrders(): List<QuickOrder>

    /** 예약 전 가격·품절 재확인 (2차 기획 §7) */
    suspend fun quote(quickOrderId: String): Quote

    /** 예약. 같은 키로 다시 부르면 같은 주문을 돌려준다 (2차 기획 §5) */
    suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order

    suspend fun reportEta(orderId: String, reading: EtaReading)

    /** 취소 요청. 완료 여부는 매장 승인 후 currentOrder로 확인한다 */
    suspend fun requestCancel(orderId: String)

    /** 재연결·앱 복귀 시 서버 상태로 다시 맞춘다 */
    suspend fun refresh()

    /** 끝난 주문 화면을 닫고 홈으로 돌아갈 때 */
    fun clearFinished()
}
```

`data/order/Quote.kt`

```kotlin
package com.pickmedt.car.data.order

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.StoreSummary

data class Quote(
    val quickOrderId: String,
    val store: StoreSummary,
    val lines: List<OrderLine>,
    val total: Money,
    /** 예약 전에 다시 확인해야 하는 조건. 없으면 null */
    val issue: QuoteIssue?,
)

/** Figma V02-SO·V02-PC·V02-CL */
enum class QuoteIssue { SOLD_OUT, PRICE_CHANGED, STORE_CLOSED }
```

`data/fake/SampleData.kt`

```kotlin
package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.Money
import com.pickmedt.car.core.model.OrderLine
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.model.StoreSummary

/** 2차 기획 §9 시연 주문: 양재 DT점·아이스 아메리카노. ID는 서버 시드와 같다 (pick-me-be src/seed/seed.ts:8,18) */
object SampleData {
    const val PLATE_NUMBER = "12가3456"

    val yangjae = StoreSummary(id = "00000000-0000-4000-8000-000000000101", name = "양재 DT점")

    val usualIcedAmericano = QuickOrder(
        id = "fake-quick-usual",
        store = yangjae,
        lines = listOf(
            OrderLine(
                menuItemId = "00000000-0000-4000-8000-000000000206",
                name = "아이스 아메리카노",
                option = "Regular",
                unitPrice = Money(4_500),
                qty = 1,
            ),
        ),
    )
}
```

- [ ] **Step 2: 가짜 서버 테스트 작성**

`test/.../data/fake/FakeServerTest.kt`

```kotlin
package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class FakeServerTest {
    private val clock = MutableClock(kst("08:02"))
    private val server = FakeServer(clock)
    private val usual = SampleData.usualIcedAmericano.id

    private fun reserved() = server.reserve(usual, "key-1")

    private fun stage() = server.order.value?.stage

    @Test fun `예약하면 매장 확인 중이고 주문번호는 P-021`() {
        val order = reserved()
        assertEquals(OrderStage.AWAITING_STORE, order.stage)
        assertEquals("P-021", order.displayNo)
        assertEquals("4,500원", order.total.format())
    }

    @Test fun `같은 요청 키로 다시 예약하면 같은 주문`() {
        val first = reserved()
        val second = server.reserve(usual, "key-1")
        assertEquals(first.id, second.id)
    }

    @Test(expected = IllegalStateException::class)
    fun `진행 중 주문이 있으면 다른 키로 예약할 수 없다`() {
        reserved()
        server.reserve(usual, "key-2")
    }

    @Test fun `직원이 수락하면 제조 대기`() {
        reserved()
        server.accept()
        assertEquals(OrderStage.WAITING_TO_PREPARE, stage())
    }

    @Test fun `2차 기획 예시 - 08시02분 ETA 6분이면 입구 08시08분 수령 08시10분 추천 08시07분`() {
        val order = reserved()
        server.accept()
        server.reportEta(order.id, etaSec = 360)
        val timing = server.order.value!!.timing!!
        assertEquals(kst("08:08"), timing.entranceEtaAt)
        assertEquals(kst("08:10"), timing.expectedPickupAt)
        assertEquals(kst("08:07"), timing.recommendedStartAt)
        assertEquals(0, timing.delaySec)
        assertEquals(kst("08:02"), timing.etaReceivedAt)
    }

    @Test fun `제조 전 지연 - ETA 10분이면 추천이 08시11분으로 늦춰진다`() {
        val order = reserved()
        server.accept()
        server.reportEta(order.id, etaSec = 360)
        server.reportEta(order.id, etaSec = 600)
        assertEquals(kst("08:11"), server.order.value!!.timing!!.recommendedStartAt)
    }

    @Test fun `제조 후 지연 - 추천은 고정되고 늦어진 시간만 공유한다`() {
        val order = reserved()
        server.accept()
        server.reportEta(order.id, etaSec = 360)
        server.startPreparing()
        clock.advance(Duration.ofMinutes(3))
        server.reportEta(order.id, etaSec = 300)
        val timing = server.order.value!!.timing!!
        assertEquals(kst("08:07"), timing.recommendedStartAt)
        assertEquals(kst("08:12"), timing.expectedPickupAt)
        assertEquals(120, timing.delaySec)
    }

    @Test(expected = IllegalStateException::class)
    fun `수락 없이 제조를 시작할 수 없다`() {
        reserved()
        server.startPreparing()
    }

    @Test fun `준비 완료 후 수령하면 수령 완료`() {
        reserved()
        server.accept()
        server.startPreparing()
        server.markReady()
        server.pickUp()
        assertEquals(OrderStage.PICKED_UP, stage())
    }

    @Test fun `매장이 거절하면 취소 완료이고 사유는 매장 거절`() {
        reserved()
        server.reject()
        assertEquals(OrderStage.CANCELLED, stage())
        assertEquals(CancelReason.STORE_REJECTED, server.order.value!!.cancelReason)
    }

    @Test fun `취소 요청 후 승인되면 취소 완료`() {
        val order = reserved()
        server.accept()
        server.requestCancel(order.id)
        assertEquals(OrderStage.CANCEL_REQUESTED, stage())
        server.approveCancel()
        assertEquals(OrderStage.CANCELLED, stage())
        assertEquals(CancelReason.CANCEL_APPROVED, server.order.value!!.cancelReason)
    }

    @Test fun `취소 요청이 거절되면 요청 전 단계로 돌아간다`() {
        val order = reserved()
        server.accept()
        server.startPreparing()
        server.requestCancel(order.id)
        server.rejectCancel()
        assertEquals(OrderStage.PREPARING, stage())
        assertTrue(server.order.value!!.cancelRejected)
    }

    @Test fun `수령 후에는 새 주문을 받을 수 있고 번호가 이어진다`() {
        val first = reserved()
        server.accept(); server.startPreparing(); server.markReady(); server.pickUp()
        val second = server.reserve(usual, "key-2")
        assertNotEquals(first.id, second.id)
        assertEquals("P-022", second.displayNo)
    }

    @Test fun `초기화하면 주문이 없다`() {
        reserved()
        server.reset()
        assertNull(server.order.value)
    }
}
```

- [ ] **Step 3: 테스트 실행해 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*FakeServerTest*'`
Expected: FAIL, `Unresolved reference: FakeServer`

- [ ] **Step 4: 가짜 서버 구현**

`data/fake/FakeServer.kt`

```kotlin
package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.CancelReason
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.OrderStage.AWAITING_STORE
import com.pickmedt.car.core.model.OrderStage.CANCELLED
import com.pickmedt.car.core.model.OrderStage.CANCEL_REQUESTED
import com.pickmedt.car.core.model.OrderStage.PICKED_UP
import com.pickmedt.car.core.model.OrderStage.PREPARING
import com.pickmedt.car.core.model.OrderStage.READY
import com.pickmedt.car.core.model.OrderStage.WAITING_TO_PREPARE
import com.pickmedt.car.core.model.PickupTiming
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.order.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.util.Locale

/**
 * 서버 없이 2차 기획 흐름을 재현하는 가짜 서버.
 * 차량 쪽 호출(예약·ETA·취소 요청)과 매장 쪽 조작(수락·제조·준비·수령·취소 판단)을 함께 제공한다.
 * 제조 타이밍은 2차 기획 §5 식을 따른다. 기본값 2분·3분은 같은 절의 시연용 가정이다.
 */
class FakeServer(
    private val clock: Clock,
    private val afterEntrance: Duration = Duration.ofMinutes(2),
    private val prepTime: Duration = Duration.ofMinutes(3),
    firstDisplayNo: Int = 21,
    private val plateNumber: String = SampleData.PLATE_NUMBER,
) {
    private val state = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = state.asStateFlow()

    val quickOrders: List<QuickOrder> = listOf(SampleData.usualIcedAmericano)

    private val orderIdByKey = mutableMapOf<String, String>()
    private var nextDisplayNo = firstDisplayNo
    private var stageBeforeCancel: OrderStage? = null

    /** 제조를 시작한 시점의 수령 예상. 이후 지연 계산의 기준 */
    private var pickupAtPrepStart: Instant? = null

    // ── 차량 쪽 ─────────────────────────────

    fun quote(quickOrderId: String): Quote {
        val quick = quickOrder(quickOrderId)
        return Quote(quick.id, quick.store, quick.lines, quick.total, issue = null)
    }

    fun reserve(quickOrderId: String, idempotencyKey: String): Order {
        val existingId = orderIdByKey[idempotencyKey]
        state.value?.let { if (it.id == existingId) return it }
        check(state.value?.stage?.isActive != true) { "진행 중인 주문이 있습니다" }

        val quick = quickOrder(quickOrderId)
        val no = nextDisplayNo++
        val order = Order(
            id = "fake-order-$no",
            displayNo = String.format(Locale.ROOT, "P-%03d", no),
            stage = AWAITING_STORE,
            store = quick.store,
            lines = quick.lines,
            plateNumber = plateNumber,
            timing = null,
            cancelReason = null,
        )
        orderIdByKey[idempotencyKey] = order.id
        stageBeforeCancel = null
        pickupAtPrepStart = null
        state.value = order
        return order
    }

    fun reportEta(orderId: String, etaSec: Int) {
        val order = current(orderId)
        state.value = order.copy(timing = timingFor(order, etaSec))
    }

    fun requestCancel(orderId: String) {
        val order = current(orderId)
        check(order.stage.isActive && order.stage != CANCEL_REQUESTED) { "취소를 요청할 수 없는 상태입니다: ${order.stage}" }
        stageBeforeCancel = order.stage
        state.value = order.copy(stage = CANCEL_REQUESTED, cancelRejected = false)
    }

    // ── 매장 쪽 (시연 컨트롤에서 호출) ─────────────

    fun accept() = move(from = AWAITING_STORE, to = WAITING_TO_PREPARE)

    fun reject() {
        val order = expect(AWAITING_STORE)
        state.value = order.copy(stage = CANCELLED, cancelReason = CancelReason.STORE_REJECTED)
    }

    fun startPreparing() {
        val order = expect(WAITING_TO_PREPARE)
        pickupAtPrepStart = order.timing?.expectedPickupAt
        state.value = order.copy(stage = PREPARING)
    }

    fun markReady() = move(from = PREPARING, to = READY)

    fun pickUp() = move(from = READY, to = PICKED_UP)

    fun approveCancel() {
        val order = expect(CANCEL_REQUESTED)
        state.value = order.copy(stage = CANCELLED, cancelReason = CancelReason.CANCEL_APPROVED)
    }

    fun rejectCancel() {
        val order = expect(CANCEL_REQUESTED)
        state.value = order.copy(stage = checkNotNull(stageBeforeCancel), cancelRejected = true)
        stageBeforeCancel = null
    }

    /** 시연 처음 상태로 */
    fun reset() {
        state.value = null
        stageBeforeCancel = null
        pickupAtPrepStart = null
    }

    // ── 내부 ────────────────────────────────

    private fun timingFor(order: Order, etaSec: Int): PickupTiming {
        val now = clock.now()
        val entrance = now.plusSeconds(etaSec.toLong())
        val pickup = entrance.plus(afterEntrance)
        val startedPickup = pickupAtPrepStart
        val previous = order.timing
        // 제조를 시작했으면 추천을 고정하고 늦어진 시간만 계산한다 (2차 기획 §3, §5)
        if (startedPickup != null && previous != null) {
            val delaySec = maxOf(0L, Duration.between(startedPickup, pickup).seconds).toInt()
            return PickupTiming(entrance, pickup, previous.recommendedStartAt, delaySec, now)
        }
        return PickupTiming(entrance, pickup, pickup.minus(prepTime), delaySec = 0, etaReceivedAt = now)
    }

    private fun move(from: OrderStage, to: OrderStage) {
        val order = expect(from)
        state.value = order.copy(stage = to)
    }

    private fun expect(stage: OrderStage): Order {
        val order = checkNotNull(state.value) { "주문이 없습니다" }
        check(order.stage == stage) { "${order.stage} 상태에서는 할 수 없습니다 (필요: $stage)" }
        return order
    }

    private fun current(orderId: String): Order {
        val order = checkNotNull(state.value) { "주문이 없습니다" }
        check(order.id == orderId) { "다른 주문입니다: $orderId" }
        return order
    }

    private fun quickOrder(id: String): QuickOrder =
        quickOrders.firstOrNull { it.id == id } ?: throw IllegalArgumentException("등록 메뉴가 없습니다: $id")
}
```

`data/fake/FakeOrderRepository.kt`

```kotlin
package com.pickmedt.car.data.fake

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import kotlinx.coroutines.flow.StateFlow

/** 가짜 서버를 쓰는 주문 저장소. 실서버 연동 전 화면 개발과 시연용 */
class FakeOrderRepository(private val server: FakeServer) : OrderRepository {
    override val currentOrder: StateFlow<Order?> = server.order

    override suspend fun quickOrders(): List<QuickOrder> = server.quickOrders

    override suspend fun quote(quickOrderId: String): Quote = server.quote(quickOrderId)

    override suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order =
        server.reserve(quickOrderId, idempotencyKey)

    override suspend fun reportEta(orderId: String, reading: EtaReading) = server.reportEta(orderId, reading.etaSec)

    override suspend fun requestCancel(orderId: String) = server.requestCancel(orderId)

    override suspend fun refresh() = Unit

    override fun clearFinished() {
        if (server.order.value?.stage?.isActive == false) server.reset()
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*FakeServerTest*'`
Expected: PASS (14개)

- [ ] **Step 6: 커밋 (사용자 실행)**

```bash
git add app/src/main/kotlin/com/pickmedt/car/data app/src/test/kotlin/com/pickmedt/car/data
```

---

### Task 4: 차량 플랫폼 인터페이스와 모의 구현 (platform)

**Files:**
- Create: `app/src/main/kotlin/com/pickmedt/car/platform/drive/DriveSignalSource.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/platform/drive/fake/FakeDriveSignalSource.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/platform/navi/EtaSource.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/platform/navi/scripted/ScriptedEtaSource.kt`
- Test: `app/src/test/kotlin/com/pickmedt/car/platform/{ScriptedEtaSourceTest,FakeDriveSignalSourceTest}.kt`

**Interfaces:**
- Consumes: `DriveSignals`, `Gear`, `EtaReading`, `EtaOrigin`, `Clock`
- Produces:
  - `interface DriveSignalSource { val signals: StateFlow<DriveSignals> }`
  - `class FakeDriveSignalSource(initial = P단·0km/h·규제 없음) : DriveSignalSource` + `shift(gear: Gear)`, `set(signals)`
  - `interface EtaSource { suspend fun read(): EtaReading? }`
  - `class ScriptedEtaSource(clock) : EtaSource` + `set(etaSec)`, `add(deltaSec)`, `clear()`

- [ ] **Step 1: 테스트 작성**

`test/.../platform/ScriptedEtaSourceTest.kt`

```kotlin
package com.pickmedt.car.platform

import com.pickmedt.car.core.model.EtaOrigin
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration

class ScriptedEtaSourceTest {
    private val clock = MutableClock(kst("08:02"))
    private val source = ScriptedEtaSource(clock)

    @Test fun `설정 전에는 읽을 값이 없다`() = runTest {
        assertNull(source.read())
    }

    @Test fun `설정한 값이 실제 시간만큼 줄어든다`() = runTest {
        source.set(360)
        assertEquals(360, source.read()!!.etaSec)
        clock.advance(Duration.ofSeconds(60))
        val reading = source.read()!!
        assertEquals(300, reading.etaSec)
        assertEquals(kst("08:03"), reading.measuredAt)
        assertEquals(EtaOrigin.SCRIPTED, reading.origin)
    }

    @Test fun `지연을 더하면 남은 시간에 더해진다`() = runTest {
        source.set(360)
        clock.advance(Duration.ofSeconds(60))
        source.add(240)
        assertEquals(540, source.read()!!.etaSec)
    }

    @Test fun `0 아래로 내려가지 않는다`() = runTest {
        source.set(30)
        clock.advance(Duration.ofSeconds(60))
        assertEquals(0, source.read()!!.etaSec)
    }

    @Test fun `지우면 다시 읽을 값이 없다`() = runTest {
        source.set(360)
        source.clear()
        assertNull(source.read())
    }
}
```

`test/.../platform/FakeDriveSignalSourceTest.kt`

```kotlin
package com.pickmedt.car.platform

import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeDriveSignalSourceTest {
    @Test fun `처음에는 P단 정지`() =
        assertEquals(DriveSignals(Gear.PARK, 0f, false), FakeDriveSignalSource().signals.value)

    @Test fun `D단으로 바꾸면 주행 속도와 주행 규제가 함께 바뀐다`() {
        val source = FakeDriveSignalSource()
        source.shift(Gear.DRIVE)
        assertEquals(DriveSignals(Gear.DRIVE, 30f, true), source.signals.value)
    }

    @Test fun `P단으로 돌아오면 정지하고 규제가 풀린다`() {
        val source = FakeDriveSignalSource()
        source.shift(Gear.DRIVE)
        source.shift(Gear.PARK)
        assertEquals(DriveSignals(Gear.PARK, 0f, false), source.signals.value)
    }
}
```

- [ ] **Step 2: 테스트 실행해 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*platform*'`
Expected: FAIL, `Unresolved reference: ScriptedEtaSource`

- [ ] **Step 3: 인터페이스와 모의 구현 작성**

`platform/drive/DriveSignalSource.kt`

```kotlin
package com.pickmedt.car.platform.drive

import com.pickmedt.car.core.model.DriveSignals
import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 주행 신호(기어·속도·주행 규제).
 * 실차 구현은 car/ 하위에 AAOS Car API로 만든다 (게이트 G2·G4 확인 후).
 */
interface DriveSignalSource {
    val signals: StateFlow<DriveSignals>
}
```

`platform/drive/fake/FakeDriveSignalSource.kt`

```kotlin
package com.pickmedt.car.platform.drive.fake

import com.pickmedt.car.core.model.DriveSignals
import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.platform.drive.DriveSignalSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 에뮬레이터 없이 개발·시연할 때 쓰는 주행 신호. 시연 컨트롤에서 값을 바꾼다. */
class FakeDriveSignalSource(
    initial: DriveSignals = DriveSignals(Gear.PARK, speedKmh = 0f, uxRestricted = false),
) : DriveSignalSource {
    private val state = MutableStateFlow(initial)
    override val signals: StateFlow<DriveSignals> = state.asStateFlow()

    fun set(signals: DriveSignals) {
        state.value = signals
    }

    /** 기어를 바꾸면 실차처럼 속도와 주행 규제도 함께 바뀐다 */
    fun shift(gear: Gear) {
        val parked = gear == Gear.PARK
        state.value = DriveSignals(gear, speedKmh = if (parked) 0f else DRIVING_SPEED_KMH, uxRestricted = !parked)
    }

    private companion object {
        const val DRIVING_SPEED_KMH = 30f
    }
}
```

`platform/navi/EtaSource.kt`

```kotlin
package com.pickmedt.car.platform.navi

import com.pickmedt.car.core.model.EtaReading

/**
 * 매장 입구까지 남은 시간.
 * 실차 구현은 pleos/ 하위에 NaviHelper로 만든다 (게이트 G3 확인 후).
 */
interface EtaSource {
    /** 지금 남은 시간을 읽는다. 읽을 수 없으면 null */
    suspend fun read(): EtaReading?
}
```

`platform/navi/scripted/ScriptedEtaSource.kt`

```kotlin
package com.pickmedt.car.platform.navi.scripted

import com.pickmedt.car.core.model.EtaOrigin
import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.platform.navi.EtaSource
import java.time.Duration
import java.time.Instant

/**
 * 시연용 모의 ETA (2차 기획 §5 '불가 시 모의 ETA').
 * 설정한 순간부터 실제 시간만큼 줄어든다.
 */
class ScriptedEtaSource(private val clock: Clock) : EtaSource {
    private var baseSec: Int? = null
    private var baseAt: Instant = Instant.EPOCH

    fun set(etaSec: Int) {
        baseSec = maxOf(0, etaSec)
        baseAt = clock.now()
    }

    /** 정체 등으로 남은 시간을 늘리거나 줄인다 */
    fun add(deltaSec: Int) = set(remainingSec() + deltaSec)

    fun clear() {
        baseSec = null
    }

    override suspend fun read(): EtaReading? {
        if (baseSec == null) return null
        return EtaReading(remainingSec(), clock.now(), EtaOrigin.SCRIPTED)
    }

    private fun remainingSec(): Int {
        val sec = baseSec ?: return 0
        val elapsed = Duration.between(baseAt, clock.now()).seconds.toInt()
        return maxOf(0, sec - elapsed)
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*platform*'`
Expected: PASS (8개)

- [ ] **Step 5: 커밋 (사용자 실행)**

```bash
git add app/src/main/kotlin/com/pickmedt/car/platform app/src/test/kotlin/com/pickmedt/car/platform
```

---

### Task 5: ETA 주기 전송 (EtaReporter)

**Files:**
- Create: `app/src/main/kotlin/com/pickmedt/car/app/EtaReporter.kt`
- Test: `app/src/test/kotlin/com/pickmedt/car/app/EtaReporterTest.kt`

**Interfaces:**
- Consumes: `OrderRepository`, `EtaSource`, `EtaPolicy.ReportInterval`
- Produces: `class EtaReporter(orders: OrderRepository, eta: EtaSource, scope: CoroutineScope, interval: Duration = EtaPolicy.ReportInterval)` + `start()`, `stop()`, `suspend fun reportNow()`

- [ ] **Step 1: 테스트 작성**

`test/.../app/EtaReporterTest.kt`

```kotlin
package com.pickmedt.car.app

import com.pickmedt.car.core.model.EtaReading
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.model.OrderStage
import com.pickmedt.car.core.model.QuickOrder
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.data.order.Quote
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import com.pickmedt.car.testing.MutableClock
import com.pickmedt.car.testing.kst
import com.pickmedt.car.testing.testOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EtaReporterTest {
    private val eta = ScriptedEtaSource(MutableClock(kst("08:02"))).apply { set(360) }
    private val orders = RecordingOrders()

    @Test fun `진행 중 주문이 없으면 보내지 않는다`() = runTest {
        EtaReporter(orders, eta, backgroundScope).start()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(0, orders.reports.size)
    }

    @Test fun `진행 중 주문이 생기면 바로 보내고 30초마다 다시 보낸다`() = runTest {
        EtaReporter(orders, eta, backgroundScope).start()
        orders.currentOrder.value = testOrder(OrderStage.WAITING_TO_PREPARE)
        runCurrent()
        assertEquals(listOf("order-1" to 360), orders.reports)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(2, orders.reports.size)
    }

    @Test fun `주문이 끝나면 보내지 않는다`() = runTest {
        EtaReporter(orders, eta, backgroundScope).start()
        orders.currentOrder.value = testOrder(OrderStage.WAITING_TO_PREPARE)
        runCurrent()
        orders.currentOrder.value = testOrder(OrderStage.PICKED_UP)
        runCurrent()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, orders.reports.size)
    }

    @Test fun `reportNow는 주기와 상관없이 바로 보낸다`() = runTest {
        val reporter = EtaReporter(orders, eta, backgroundScope)
        orders.currentOrder.value = testOrder(OrderStage.PREPARING)
        reporter.reportNow()
        assertEquals(1, orders.reports.size)
    }

    private class RecordingOrders : OrderRepository {
        override val currentOrder = MutableStateFlow<Order?>(null)
        val reports = mutableListOf<Pair<String, Int>>()

        override suspend fun reportEta(orderId: String, reading: EtaReading) {
            reports += orderId to reading.etaSec
        }

        override suspend fun quickOrders(): List<QuickOrder> = emptyList()
        override suspend fun quote(quickOrderId: String): Quote = error("사용하지 않음")
        override suspend fun reserve(quickOrderId: String, idempotencyKey: String): Order = error("사용하지 않음")
        override suspend fun requestCancel(orderId: String) = Unit
        override suspend fun refresh() = Unit
        override fun clearFinished() = Unit
    }
}
```

- [ ] **Step 2: 테스트 실행해 실패 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*EtaReporterTest*'`
Expected: FAIL, `Unresolved reference: EtaReporter`

- [ ] **Step 3: 구현**

`app/EtaReporter.kt`

```kotlin
package com.pickmedt.car.app

import com.pickmedt.car.core.rule.EtaPolicy
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.platform.navi.EtaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * 진행 중 주문이 있는 동안 ETA를 주기적으로 서버에 보낸다 (2차 기획 §5: 30초 갱신).
 * 주문이 없거나 끝나면 멈춰 백그라운드 폴링을 줄인다.
 */
class EtaReporter(
    private val orders: OrderRepository,
    private val eta: EtaSource,
    private val scope: CoroutineScope,
    private val interval: Duration = EtaPolicy.ReportInterval,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            orders.currentOrder
                .map { order -> order?.takeIf { it.stage.isActive }?.id }
                .distinctUntilChanged()
                .collectLatest { orderId ->
                    if (orderId == null) return@collectLatest
                    while (true) {
                        report(orderId)
                        delay(interval.toMillis())
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** 시연 조작 직후처럼 주기를 기다리지 않고 바로 보낼 때 */
    suspend fun reportNow() {
        val orderId = orders.currentOrder.value?.takeIf { it.stage.isActive }?.id ?: return
        report(orderId)
    }

    private suspend fun report(orderId: String) {
        val reading = eta.read() ?: return
        try {
            orders.reportEta(orderId, reading)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 다음 주기에 다시 보낸다. 끊김 표시는 서버의 마지막 수신 시각으로 판단한다 (2차 기획 §5)
        }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew :app:testDebugUnitTest --tests '*EtaReporterTest*'`
Expected: PASS (4개)

- [ ] **Step 5: 커밋 (사용자 실행)**

```bash
git add app/src/main/kotlin/com/pickmedt/car/app/EtaReporter.kt app/src/test/kotlin/com/pickmedt/car/app/EtaReporterTest.kt
```

---

### Task 6: 앱 연결과 시연 컨트롤 (app, demo, ui)

**Files:**
- Create: `app/src/main/kotlin/com/pickmedt/car/app/{AppContainer,AppRoot}.kt`
- Modify: `app/src/main/kotlin/com/pickmedt/car/app/{PickmeApplication,MainActivity}.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/demo/{DemoController,DemoPanel}.kt`
- Create: `app/src/main/kotlin/com/pickmedt/car/ui/theme/PickmeTheme.kt`

**Interfaces:**
- Consumes: Task 2~5 전부
- Produces:
  - `class AppContainer(appScope: CoroutineScope, clock: Clock = Clock.System)`: `orderRepository: OrderRepository`, `driveSignalSource: DriveSignalSource`, `etaReporter: EtaReporter`, `demo: DemoController`
  - `class DemoController(server: FakeServer, drive: FakeDriveSignalSource, eta: ScriptedEtaSource, reportEtaNow: suspend () -> Unit, scope: CoroutineScope)`: 각 조작은 `Result<Unit>` 반환
  - `@Composable fun DemoPanel(controller: DemoController, modifier: Modifier = Modifier)`
  - `@Composable fun PickmeTheme(content: @Composable () -> Unit)`
  - `@Composable fun AppRoot(container: AppContainer)`

- [ ] **Step 1: 테마 작성**

`ui/theme/PickmeTheme.kt`

```kotlin
package com.pickmedt.car.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Figma V01·V04-A 캡처 기준 색. 디자인 토큰 정리는 화면 구현 계획에서 한다 */
private val PickmeColors = darkColorScheme(
    primary = Color(0xFFFF7A3D),
    background = Color(0xFF1C1D21),
    surface = Color(0xFF26272B),
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
)

@Composable
fun PickmeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PickmeColors, content = content)
}
```

- [ ] **Step 2: 시연 컨트롤 작성**

`demo/DemoController.kt`

```kotlin
package com.pickmedt.car.demo

import com.pickmedt.car.core.model.Gear
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 시연 컨트롤 (Figma 프로토타입 하단 '시연 컨트롤' 영역).
 * 가짜 서버의 매장 조작, 모의 ETA, 모의 주행 신호를 바꾼다. 실제 주문·차량과 연동되지 않는다.
 */
class DemoController(
    private val server: FakeServer,
    private val drive: FakeDriveSignalSource,
    private val eta: ScriptedEtaSource,
    private val reportEtaNow: suspend () -> Unit,
    private val scope: CoroutineScope,
) {
    fun accept() = runCatching { server.accept() }
    fun reject() = runCatching { server.reject() }
    fun startPreparing() = runCatching { server.startPreparing() }
    fun markReady() = runCatching { server.markReady() }
    fun pickUp() = runCatching { server.pickUp() }
    fun approveCancel() = runCatching { server.approveCancel() }
    fun rejectCancel() = runCatching { server.rejectCancel() }

    fun shift(gear: Gear) = runCatching { drive.shift(gear) }

    fun setEta(etaSec: Int) = runCatching {
        eta.set(etaSec)
        scope.launch { reportEtaNow() }
        Unit
    }

    fun delayEta(deltaSec: Int) = runCatching {
        eta.add(deltaSec)
        scope.launch { reportEtaNow() }
        Unit
    }

    fun reset() = runCatching {
        server.reset()
        eta.clear()
        drive.shift(Gear.PARK)
    }
}
```

`demo/DemoPanel.kt`

```kotlin
package com.pickmedt.car.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pickmedt.car.core.model.Gear

/** 시연 컨트롤 화면. 앱 영역과 분리해 아래에 붙인다 (Figma 프로토타입 840×1184 앱 영역 + 하단 컨트롤) */
@Composable
fun DemoPanel(controller: DemoController, modifier: Modifier = Modifier) {
    var message by remember { mutableStateOf("시연 컨트롤 · 실제 주문/차량 연동 없음") }
    val actions: List<Pair<String, () -> Result<Unit>>> = listOf(
        "매장 수락" to controller::accept,
        "매장 거절" to controller::reject,
        "제조 시작" to controller::startPreparing,
        "준비 완료" to controller::markReady,
        "수령 완료" to controller::pickUp,
        "취소 승인" to controller::approveCancel,
        "취소 거절" to controller::rejectCancel,
        "ETA 6분" to { controller.setEta(360) },
        "지연 +4분" to { controller.delayEta(240) },
        "P단" to { controller.shift(Gear.PARK) },
        "D단" to { controller.shift(Gear.DRIVE) },
        "초기화" to controller::reset,
    )
    Column(
        modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1D17))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, color = Color(0xFFE6E6E6))
        actions.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, action) ->
                    Button(
                        onClick = { message = action().fold({ "$label 완료" }, { "$label 실패: ${it.message}" }) },
                        modifier = Modifier.weight(1f),
                    ) { Text(label) }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
```

- [ ] **Step 3: 앱 연결 작성**

`app/AppContainer.kt`

```kotlin
package com.pickmedt.car.app

import com.pickmedt.car.core.time.Clock
import com.pickmedt.car.data.fake.FakeOrderRepository
import com.pickmedt.car.data.fake.FakeServer
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.demo.DemoController
import com.pickmedt.car.platform.drive.DriveSignalSource
import com.pickmedt.car.platform.drive.fake.FakeDriveSignalSource
import com.pickmedt.car.platform.navi.EtaSource
import com.pickmedt.car.platform.navi.scripted.ScriptedEtaSource
import kotlinx.coroutines.CoroutineScope

/**
 * 수동 의존성 주입. 가짜/실서버, 모의/실차 구현은 여기서만 고른다.
 * 지금은 서버 연동(11/13)과 에뮬레이터 게이트 전이라 모두 가짜·모의 구현이다.
 */
class AppContainer(appScope: CoroutineScope, clock: Clock = Clock.System) {
    private val fakeServer = FakeServer(clock)
    private val fakeDrive = FakeDriveSignalSource()
    private val scriptedEta = ScriptedEtaSource(clock)

    val orderRepository: OrderRepository = FakeOrderRepository(fakeServer)
    val driveSignalSource: DriveSignalSource = fakeDrive
    private val etaSource: EtaSource = scriptedEta

    val etaReporter = EtaReporter(orderRepository, etaSource, appScope)

    val demo = DemoController(
        server = fakeServer,
        drive = fakeDrive,
        eta = scriptedEta,
        reportEtaNow = etaReporter::reportNow,
        scope = appScope,
    )
}
```

`app/PickmeApplication.kt` (교체)

```kotlin
package com.pickmedt.car.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PickmeApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(appScope)
        container.etaReporter.start()
    }
}
```

`app/AppRoot.kt`

```kotlin
package com.pickmedt.car.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pickmedt.car.core.model.DriveContext
import com.pickmedt.car.core.model.Order
import com.pickmedt.car.core.rule.resolveDriveContext
import com.pickmedt.car.data.order.OrderRepository
import com.pickmedt.car.demo.DemoPanel
import com.pickmedt.car.ui.theme.PickmeTheme
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 최상위 화면. 앱 영역 + 시연 컨트롤.
 * 앱 영역은 뼈대 확인용 상태 표시이고, 화면 구현 계획에서 feature/ 화면으로 바꾼다.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val order by container.orderRepository.currentOrder.collectAsStateWithLifecycle()
    val signals by container.driveSignalSource.signals.collectAsStateWithLifecycle()
    PickmeTheme {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp)) {
                SkeletonStatus(order, resolveDriveContext(signals), container.orderRepository)
            }
            DemoPanel(container.demo)
        }
    }
}

@Composable
private fun SkeletonStatus(order: Order?, drive: DriveContext, orders: OrderRepository) {
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("주행 맥락: $drive", color = MaterialTheme.colorScheme.onBackground)
        if (order == null) {
            Text("진행 중 주문 없음", color = MaterialTheme.colorScheme.onBackground)
            Button(
                enabled = drive.allowsDetail,
                onClick = {
                    scope.launch {
                        val quick = orders.quickOrders().first()
                        orders.reserve(quick.id, UUID.randomUUID().toString())
                    }
                },
            ) { Text("이 메뉴로 예약") }
        } else {
            Text("${order.displayNo} · ${order.stage}", color = MaterialTheme.colorScheme.onBackground)
            Text("${order.store.name} · ${order.total.format()}", color = MaterialTheme.colorScheme.onBackground)
            order.timing?.let {
                Text("입구 ${it.entranceEtaAt} · 추천 ${it.recommendedStartAt} · 지연 ${it.delaySec}초", color = MaterialTheme.colorScheme.onBackground)
            }
            if (!order.stage.isActive) {
                Button(onClick = { orders.clearFinished() }) { Text("닫기") }
            }
        }
    }
}
```

`app/MainActivity.kt` (교체)

```kotlin
package com.pickmedt.car.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PickmeApplication).container
        setContent { AppRoot(container) }
    }
}
```

- [ ] **Step 4: 전체 빌드와 테스트**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: `패키지 경계 검사 통과`, `BUILD SUCCESSFUL`, 테스트 42개 통과

- [ ] **Step 5: 실행 확인 (에뮬레이터 준비 후)**

Pleos 에뮬레이터가 있으면: `./gradlew :app:installDebug` 후 실행해 「이 메뉴로 예약 → 매장 수락 → ETA 6분 → 지연 +4분 → 제조 시작 → 지연 +4분 → 준비 완료 → 수령 완료 → 닫기」를 시연 컨트롤로 진행하고, D단에서 「이 메뉴로 예약」 버튼이 비활성인지 확인한다. 에뮬레이터가 없으면 이 단계는 게이트 G1 이후로 미룬다.

- [ ] **Step 6: 커밋 (사용자 실행)**

```bash
git add app/src/main/kotlin/com/pickmedt/car/app app/src/main/kotlin/com/pickmedt/car/demo app/src/main/kotlin/com/pickmedt/car/ui
```

---

## 다음 계획

1. 화면 구현: Figma V00~V08을 `feature/launch·home·reserve·progress·order·settings`로 구현, 디자인 토큰 정리
2. 실서버 연동(11/13): `data/remote`에 Retrofit·Socket.IO, 백엔드가 정한 상태·필드 이름으로 변환
3. 실차 연동(게이트 G1~G4 이후): `platform/drive/car`(Car API·CarUxRestrictions), `platform/navi/pleos`(NaviHelper)
