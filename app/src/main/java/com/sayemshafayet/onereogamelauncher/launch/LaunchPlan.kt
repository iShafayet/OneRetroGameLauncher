package com.sayemshafayet.onereogamelauncher.launch

data class LaunchPlan(
    val emulatorKey: String,
    val packageName: String?,
    val activityClass: String?,
    val core: String?,
    val romPath: String,
    val customConfig: String?,
    val isRetroArch: Boolean,
)
