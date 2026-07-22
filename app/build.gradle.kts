import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val versionPropertiesFile = rootProject.file("version.properties")

fun loadVersionProperties(): Properties {
    val props = Properties()
    if (versionPropertiesFile.exists()) {
        versionPropertiesFile.inputStream().use { props.load(it) }
    }
    return props
}

fun orglVersionName(props: Properties = loadVersionProperties()): String {
    val major = props.getProperty("VERSION_MAJOR", "0")
    val minor = props.getProperty("VERSION_MINOR", "0")
    val patch = props.getProperty("VERSION_PATCH", "0")
    val prerelease = props.getProperty("VERSION_PRERELEASE", "alpha")
    val build = props.getProperty("VERSION_BUILD", "0")
    return "$major.$minor.$patch-$prerelease+$build"
}

fun orglVersionCode(props: Properties = loadVersionProperties()): Int =
    props.getProperty("VERSION_BUILD", "0").toInt()

fun bumpOrglBuildVersion() {
    val props = loadVersionProperties()
    val build = props.getProperty("VERSION_BUILD", "0").toInt() + 1
    versionPropertiesFile.writeText(
        """
        |# ORGL semver — bump MAJOR/MINOR/PATCH manually when cutting a release.
        |# VERSION_BUILD is the monotonic Android versionCode (+build in versionName); auto-increments on debug builds.
        |VERSION_MAJOR=${props.getProperty("VERSION_MAJOR", "0")}
        |VERSION_MINOR=${props.getProperty("VERSION_MINOR", "0")}
        |VERSION_PATCH=${props.getProperty("VERSION_PATCH", "0")}
        |VERSION_PRERELEASE=${props.getProperty("VERSION_PRERELEASE", "alpha")}
        |VERSION_BUILD=$build
        |
        """.trimMargin(),
    )
}

fun storeFlavorFromAssembleTask(taskName: String): String? = when {
    taskName.contains("Fdroid", ignoreCase = true) -> "fdroid"
    taskName.contains("Play", ignoreCase = true) -> "play"
    else -> null
}

val orglVersion = orglVersionName()
val orglVersionCodeValue = orglVersionCode()

android {
    namespace = "com.sayemshafayet.onereogamelauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sayemshafayet.onereogamelauncher"
        minSdk = 30
        targetSdk = 36
        versionCode = orglVersionCodeValue
        versionName = orglVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "store"
    productFlavors {
        create("fdroid") {
            dimension = "store"
            isDefault = true
        }
        create("play") {
            dimension = "store"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.matching { it.name.matches(Regex("assemble(Fdroid|Play)Debug")) }.configureEach {
    doLast {
        val flavor = storeFlavorFromAssembleTask(name) ?: return@doLast
        val version = orglVersionName()
        val apk = layout.buildDirectory.file("outputs/apk/$flavor/debug/app-$flavor-debug.apk").get().asFile
        if (apk.exists()) {
            val destDir = rootProject.file(".local/apk")
            destDir.mkdirs()
            val dest = destDir.resolve("orgl-$flavor-debug-$version.apk")
            apk.copyTo(dest, overwrite = true)
            logger.lifecycle("Copied APK to ${dest.relativeTo(rootProject.projectDir)}")
        } else {
            logger.warn("Expected APK missing: $apk")
        }
        bumpOrglBuildVersion()
        logger.lifecycle("Next debug build version: ${orglVersionName()}")
    }
}
