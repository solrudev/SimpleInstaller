package io.github.solrudev.simpleinstaller.utils

import android.content.pm.PackageManager
import io.github.solrudev.simpleinstaller.SimpleInstaller

@JvmSynthetic
internal fun isPackageInstalled(packageName: String?): Boolean {
	if (packageName == null) return false
	return try {
		SimpleInstaller.applicationContext.packageManager.getPackageInfo(
			packageName,
			PackageManager.GET_ACTIVITIES
		)
		true
	} catch (_: PackageManager.NameNotFoundException) {
		false
	}
}