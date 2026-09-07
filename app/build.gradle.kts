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
        |# VERSION_BUILD is the monotonic Android versionCode (+build in versionName); bump with: make bump
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
    taskName.contains("Internal", ignoreCase = true) -> "internal"
    taskName.contains("Foss", ignoreCase = true) -> "foss"
    taskName.contains("Play", ignoreCase = true) -> "play"
    else -> null
}

val orglVersion = orglVersionName()
val orglVersionCodeValue = orglVersionCode()

fun loadKeystoreProperties(fileName: String): Properties? {
    val file = rootProject.file(fileName)
    if (!file.exists()) return null
    val props = Properties().apply {
        file.inputStream().use { load(it) }
    }
    val required = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    return if (required.all { props.getProperty(it) != null }) props else null
}


val playKeystoreProperties = loadKeystoreProperties("keystore-play.properties")
val fossKeystoreProperties = loadKeystoreProperties("keystore-foss.properties")

fun hasSigningForFlavor(flavor: String): Boolean = when (flavor) {
    "foss" -> fossKeystoreProperties != null
    "play" -> playKeystoreProperties != null
    else -> false
}

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

    signingConfigs {
        playKeystoreProperties?.let { props ->
            create("playRelease") {
                storeFile = rootProject.file(props.getProperty("storeFile")!!)
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
        fossKeystoreProperties?.let { props ->
            create("fossRelease") {
                storeFile = rootProject.file(props.getProperty("storeFile")!!)
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    productFlavors {
        create("foss") {
            dimension = "store"
            isDefault = true
            applicationId = "com.sayemshafayet.orglfoss"
            if (fossKeystoreProperties != null) {
                signingConfig = signingConfigs.getByName("fossRelease")
            }
        }
        create("play") {
            dimension = "store"
            applicationId = "com.sayemshafayet.onereogamelauncher"
            if (playKeystoreProperties != null) {
                signingConfig = signingConfigs.getByName("playRelease")
            }
        }
        create("internal") {
            dimension = "store"
            // Sideload / debug only — never signed for store release.
            applicationId = "com.sayemshafayet.orglinternal"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        // Keep minified debug APKs small enough to sideload (default stores DEX uncompressed).
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Internal is debug-only — do not generate a release variant.
androidComponents {
    beforeVariants { variantBuilder ->
        if (variantBuilder.flavorName == "internal" && variantBuilder.buildType == "release") {
            variantBuilder.enable = false
        }
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

tasks.register("bumpVersion") {
    group = "versioning"
    description = "Increment VERSION_BUILD in version.properties"
    doLast {
        val before = orglVersionName()
        bumpOrglBuildVersion()
        logger.lifecycle("Bumped build version: $before → ${orglVersionName()}")
    }
}

tasks.matching { it.name.matches(Regex("assemble(Foss|Play|Internal)Debug")) }.configureEach {
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
    }
}

tasks.matching { it.name.matches(Regex("assemble(Foss|Play)Release")) }.configureEach {
    doLast {
        val flavor = storeFlavorFromAssembleTask(name) ?: return@doLast
        val version = orglVersionName()
        val apk = layout.buildDirectory.file("outputs/apk/$flavor/release/app-$flavor-release.apk").get().asFile
        if (apk.exists()) {
            val destDir = rootProject.file(".local/apk")
            destDir.mkdirs()
            val dest = destDir.resolve("orgl-$flavor-release-$version.apk")
            apk.copyTo(dest, overwrite = true)
            logger.lifecycle("Copied APK to ${dest.relativeTo(rootProject.projectDir)}")
            if (!hasSigningForFlavor(flavor)) {
                val propsFile = if (flavor == "foss") "keystore-foss.properties" else "keystore-play.properties"
                logger.warn(
                    "Release signing is not configured ($propsFile missing). " +
                        "This APK is unsigned and should not be published.",
                )
            }
        } else {
            logger.warn("Expected APK missing: $apk")
        }
    }
}

tasks.matching { it.name.matches(Regex("bundle(Foss|Play)Release")) }.configureEach {
    doLast {
        val flavor = storeFlavorFromAssembleTask(name) ?: return@doLast
        val version = orglVersionName()
        val aab = layout.buildDirectory
            .file("outputs/bundle/${flavor}Release/app-$flavor-release.aab")
            .get()
            .asFile
        if (aab.exists()) {
            val destDir = rootProject.file(".local/bundle")
            destDir.mkdirs()
            val dest = destDir.resolve("orgl-$flavor-release-$version.aab")
            aab.copyTo(dest, overwrite = true)
            logger.lifecycle("Copied AAB to ${dest.relativeTo(rootProject.projectDir)}")
            if (!hasSigningForFlavor(flavor)) {
                val propsFile = if (flavor == "foss") "keystore-foss.properties" else "keystore-play.properties"
                logger.warn(
                    "Release signing is not configured ($propsFile missing). " +
                        "This AAB is unsigned and will be rejected by Play Console.",
                )
            }
        } else {
            logger.warn("Expected AAB missing: $aab")
        }
    }
}
