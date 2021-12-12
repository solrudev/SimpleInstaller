package io.github.solrudev.simpleinstaller

import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.ProgressData

/**
 * A callback interface for [PackageInstaller]'s usage from Java.
 */
interface PackageInstallerCallback {

	/**
	 * Called when installation finished with success.
	 */
	fun onSuccess()

	/**
	 * Called when installation finished with failure.
	 *
	 * @param [cause] Cause of failure. Always null on Android versions lower than Lollipop (5.0).
	 */
	fun onFailure(cause: InstallFailureCause?)

	/**
	 * Called when an exception is thrown.
	 *
	 * @param [exception] A [Throwable] which has been thrown.
	 */
	fun onException(exception: Throwable)

	/**
	 * Called when installation was canceled normally.
	 */
	fun onCanceled()

	/**
	 * Called when installation progress has been updated.
	 *
	 * @param [progress] [ProgressData] object representing installation progress.
	 */
	fun onProgressChanged(progress: ProgressData)
}