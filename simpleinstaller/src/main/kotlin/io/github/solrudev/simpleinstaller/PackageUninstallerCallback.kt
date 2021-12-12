package io.github.solrudev.simpleinstaller

interface PackageUninstallerCallback {
	fun onFinished(success: Boolean)
	fun onException(e: Throwable)
	fun onCanceled()
}