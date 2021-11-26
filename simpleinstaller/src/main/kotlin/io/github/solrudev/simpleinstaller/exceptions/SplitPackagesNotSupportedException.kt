package io.github.solrudev.simpleinstaller.exceptions

import android.os.Build

class SplitPackagesNotSupportedException : Exception() {
	override val message: String =
		"Split packages are not supported on current Android API level: ${Build.VERSION.SDK_INT}."
}