import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
