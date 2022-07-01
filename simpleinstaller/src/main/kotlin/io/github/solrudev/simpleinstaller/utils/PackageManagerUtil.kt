package io.github.solrudev.simpleinstaller.utils

import android.content.pm.PackageManager
import io.github.solrudev.simpleinstaller.SimpleInstaller.packageManager

@JvmSynthetic
internal fun isPackageInstalled(packageName: String) = try {
	packageManager.getPackageInfo(
		packageName,
		PackageManager.GET_ACTIVITIES
	)
	true
} catch (_: PackageManager.NameNotFoundException) {
	false
}

@JvmSynthetic
internal fun getApplicationLabel(packageName: String) = try {
	packageManager
		.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
		.loadLabel(packageManager)
} catch (_: PackageManager.NameNotFoundException) {
	null
}