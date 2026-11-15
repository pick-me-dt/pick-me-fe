import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 서버 JSON (data/remote)
    alias(libs.plugins.kotlin.serialization)
    // JVM 스크린샷 테스트 (./gradlew :app:recordRoborazziDebug)
    alias(libs.plugins.roborazzi)
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
        // 실서버 주소. 비우면 앱 안의 가짜 서버를 쓴다.
        // 예: ./gradlew :app:installDebug -Ppickme.serverUrl=http://10.0.2.2:3000
        val serverUrl = providers.gradleProperty("pickme.serverUrl").orElse("").get()
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
    }

    // release 서명 정보는 저장소 루트의 keystore.properties에 둔다 (git 제외)
    // 키: storeFile(저장소 루트 기준 경로)·storePassword·keyAlias·keyPassword
    // 파일이 없으면 release APK는 서명 없이 나온다 (app-release-unsigned.apk)
    val releaseKeystore = rootProject.file("keystore.properties")
    signingConfigs {
        if (releaseKeystore.exists()) {
            val props = Properties().apply { releaseKeystore.inputStream().use { load(it) } }
            create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric 스크린샷 테스트가 글꼴·드로어블 리소스를 읽도록
        unitTests.isIncludeAndroidResources = true
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
    // 실서버 연동 (data/remote에서만 import)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.socket.io.client) {
        // Android에 org.json이 있다
        exclude(group = "org.json", module = "json")
    }

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.okhttp.mockwebserver)
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
        // 구현 하위 패키지: data/fake, data/local, data/remote, platform/<영역>/<구현>
        val impl = "$p\\.(data\\.(fake|local|remote)|platform\\.[a-z]+\\.[a-z]+)\\."
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
