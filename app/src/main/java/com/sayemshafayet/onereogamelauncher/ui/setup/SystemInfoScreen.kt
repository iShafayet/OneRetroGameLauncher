package com.sayemshafayet.onereogamelauncher.ui.setup

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sayemshafayet.onereogamelauncher.BuildConfig
import com.sayemshafayet.onereogamelauncher.ui.play.isCompactWidePlayLayout
import com.sayemshafayet.onereogamelauncher.ui.play.isWidePlayLayout
import com.sayemshafayet.onereogamelauncher.ui.util.configurationLandscapeAspectRatio
import com.sayemshafayet.onereogamelauncher.ui.util.physicalDisplaySizePx
import com.sayemshafayet.onereogamelauncher.ui.util.physicalLandscapeAspectRatio
import java.util.Locale

private data class SystemInfoSection(
    val title: String,
    val rows: List<Pair<String, String>>,
)

@Composable
fun SystemInfoScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val sections = remember(
        configuration.orientation,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        configuration.densityDpi,
        configuration.fontScale,
        configuration.smallestScreenWidthDp,
        density.density,
        density.fontScale,
    ) {
        collectSystemInfo(context, configuration)
    }
    val clipboardText = remember(sections) { formatSystemInfoPlainText(sections) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "Device, display, and layout metrics useful for debugging widescreen breakpoints.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ORGL System Info", clipboardText))
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Copy all to clipboard")
        }

        sections.forEach { section ->
            Spacer(Modifier.height(16.dp))
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            section.rows.forEach { (label, value) ->
                SystemInfoRow(label = label, value = value)
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.58f),
        )
    }
}

private fun collectSystemInfo(
    context: Context,
    configuration: Configuration,
): List<SystemInfoSection> {
    val metrics = context.resources.displayMetrics
    val physicalPx = physicalDisplaySizePx(context)
    val physicalRatio = physicalLandscapeAspectRatio(context)
        ?: configurationLandscapeAspectRatio(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
        )
    val windowRatio = configuration.screenWidthDp.toFloat() /
        configuration.screenHeightDp.coerceAtLeast(1).toFloat()
    val windowLandscapeRatio = configurationLandscapeAspectRatio(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
    val orientationLabel = when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        else -> "undefined"
    }
    val wideLibrary = isLibraryWideLandscape(
        orientation = configuration.orientation,
        landscapeAspectRatio = physicalRatio,
    )
    val widePlay = isWidePlayLayout(
        orientation = configuration.orientation,
        landscapeAspectRatio = physicalRatio,
    )
    val compactWidePlay = isCompactWidePlayLayout(configuration.screenHeightDp)

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowBounds = windowManager.currentWindowMetrics.bounds
    val display = context.display
    val mode = display?.mode

    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
    val runtime = Runtime.getRuntime()

    return listOf(
        SystemInfoSection(
            title = "App",
            rows = listOf(
                "Version" to BuildConfig.VERSION_NAME,
                "Version code" to BuildConfig.VERSION_CODE.toString(),
                "Application ID" to BuildConfig.APPLICATION_ID,
                "Build type" to BuildConfig.BUILD_TYPE,
                "Flavor" to BuildConfig.FLAVOR,
                "Debuggable" to BuildConfig.DEBUG.toString(),
            ),
        ),
        SystemInfoSection(
            title = "Device",
            rows = listOf(
                "Manufacturer" to Build.MANUFACTURER,
                "Brand" to Build.BRAND,
                "Model" to Build.MODEL,
                "Device" to Build.DEVICE,
                "Product" to Build.PRODUCT,
                "Hardware" to Build.HARDWARE,
                "Board" to Build.BOARD,
                "Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(", "),
            ),
        ),
        SystemInfoSection(
            title = "Android",
            rows = listOf(
                "Release" to Build.VERSION.RELEASE,
                "SDK" to Build.VERSION.SDK_INT.toString(),
                "Codename" to Build.VERSION.CODENAME,
                "Security patch" to Build.VERSION.SECURITY_PATCH,
                "Incremental" to Build.VERSION.INCREMENTAL,
                "Fingerprint" to Build.FINGERPRINT,
                "Locale" to Locale.getDefault().toLanguageTag(),
            ),
        ),
        SystemInfoSection(
            title = "Display (physical panel)",
            rows = buildList {
                add(
                    "Mode size" to (physicalPx?.let { (w, h) -> "${w}×${h} px" } ?: "n/a"),
                )
                add(
                    "Landscape aspect" to String.format(
                        Locale.US,
                        "%.4f (%.2f:9)",
                        physicalRatio,
                        physicalRatio * 9f,
                    ),
                )
                add(
                    "Refresh rate" to (
                        mode?.refreshRate?.let { String.format(Locale.US, "%.2f Hz", it) } ?: "n/a"
                        ),
                )
                add("Display ID" to (display?.displayId?.toString() ?: "n/a"))
                add("Hdr" to (display?.isHdr?.toString() ?: "n/a"))
            },
        ),
        SystemInfoSection(
            title = "Window (app after system bars)",
            rows = listOf(
                "Configuration size" to
                    "${configuration.screenWidthDp}×${configuration.screenHeightDp} dp",
                "Window metrics" to "${windowBounds.width()}×${windowBounds.height()} px",
                "Width/height ratio" to String.format(Locale.US, "%.4f", windowRatio),
                "Landscape aspect (dp)" to String.format(Locale.US, "%.4f", windowLandscapeRatio),
                "Smallest width" to "${configuration.smallestScreenWidthDp} dp",
                "Orientation" to orientationLabel,
                "Font scale" to String.format(Locale.US, "%.3f", configuration.fontScale),
                "Screen layout" to screenLayoutLabel(configuration.screenLayout),
                "UI mode" to uiModeLabel(configuration.uiMode),
            ),
        ),
        SystemInfoSection(
            title = "Density",
            rows = listOf(
                "Density" to String.format(Locale.US, "%.3f", metrics.density),
                "Density DPI" to metrics.densityDpi.toString(),
                "Scaled density" to String.format(
                    Locale.US,
                    "%.3f",
                    metrics.density * configuration.fontScale,
                ),
                "xdpi" to String.format(Locale.US, "%.2f", metrics.xdpi),
                "ydpi" to String.format(Locale.US, "%.2f", metrics.ydpi),
                "Bucket" to densityBucketLabel(metrics.densityDpi),
            ),
        ),
        SystemInfoSection(
            title = "ORGL layout breakpoints",
            rows = listOf(
                "Library wide (>16:9 physical)" to wideLibrary.toString(),
                "Play wide (≥16:9 physical)" to widePlay.toString(),
                "Play compact wide (height <500dp)" to compactWidePlay.toString(),
                "16:9 reference" to String.format(Locale.US, "%.4f", 16f / 9f),
            ),
        ),
        SystemInfoSection(
            title = "Memory",
            rows = listOf(
                "Device total" to formatBytes(memInfo.totalMem),
                "Device available" to formatBytes(memInfo.availMem),
                "Low memory" to memInfo.lowMemory.toString(),
                "Runtime max" to formatBytes(runtime.maxMemory()),
                "Runtime total" to formatBytes(runtime.totalMemory()),
                "Runtime free" to formatBytes(runtime.freeMemory()),
            ),
        ),
    )
}

private fun formatSystemInfoPlainText(sections: List<SystemInfoSection>): String =
    buildString {
        appendLine("ORGL System Info")
        appendLine("================")
        sections.forEach { section ->
            appendLine()
            appendLine("[${section.title}]")
            section.rows.forEach { (label, value) ->
                appendLine("$label: $value")
            }
        }
    }.trimEnd()

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

private fun densityBucketLabel(dpi: Int): String = when {
    dpi <= DisplayMetrics.DENSITY_LOW -> "ldpi"
    dpi <= DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
    dpi <= DisplayMetrics.DENSITY_TV -> "tvdpi"
    dpi <= DisplayMetrics.DENSITY_HIGH -> "hdpi"
    dpi <= DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
    dpi <= DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
    else -> "xxxhdpi+"
}

private fun screenLayoutLabel(screenLayout: Int): String {
    val size = when (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
        Configuration.SCREENLAYOUT_SIZE_SMALL -> "small"
        Configuration.SCREENLAYOUT_SIZE_NORMAL -> "normal"
        Configuration.SCREENLAYOUT_SIZE_LARGE -> "large"
        Configuration.SCREENLAYOUT_SIZE_XLARGE -> "xlarge"
        else -> "undefined"
    }
    val long = when (screenLayout and Configuration.SCREENLAYOUT_LONG_MASK) {
        Configuration.SCREENLAYOUT_LONG_YES -> "long"
        Configuration.SCREENLAYOUT_LONG_NO -> "not-long"
        else -> "long-undefined"
    }
    return "$size, $long"
}

private fun uiModeLabel(uiMode: Int): String {
    val type = when (uiMode and Configuration.UI_MODE_TYPE_MASK) {
        Configuration.UI_MODE_TYPE_NORMAL -> "normal"
        Configuration.UI_MODE_TYPE_DESK -> "desk"
        Configuration.UI_MODE_TYPE_CAR -> "car"
        Configuration.UI_MODE_TYPE_TELEVISION -> "television"
        Configuration.UI_MODE_TYPE_APPLIANCE -> "appliance"
        Configuration.UI_MODE_TYPE_WATCH -> "watch"
        Configuration.UI_MODE_TYPE_VR_HEADSET -> "vr"
        else -> "other"
    }
    val night = when (uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> "night"
        Configuration.UI_MODE_NIGHT_NO -> "day"
        else -> "night-undefined"
    }
    return "$type, $night"
}
