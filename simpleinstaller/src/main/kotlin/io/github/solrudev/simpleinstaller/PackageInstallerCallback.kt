package io.github.solrudev.simpleinstaller

import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.ProgressData

interface PackageInstallerCallback {
	fun onSuccess()
	fun onFailure(cause: InstallFailureCause?)
	fun onException(exception: Throwable)
	fun onCanceled()
	fun onProgressChanged(progress: ProgressData)
}