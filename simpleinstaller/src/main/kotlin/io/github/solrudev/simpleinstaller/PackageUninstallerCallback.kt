package io.github.solrudev.simpleinstaller

/**
 * A callback interface for [PackageUninstaller]'s usage from Java.
 */
interface PackageUninstallerCallback {

	/**
	 * Called when uninstallation finished, either with success or failure.
	 *
	 * @param [success] Success of uninstallation.
	 */
	fun onFinished(success: Boolean)

	/**
	 * Called when an exception is thrown.
	 *
	 * @param [exception] A [Throwable] which has been thrown.
	 */
	fun onException(exception: Throwable)

	/**
	 * Called when uninstallation was canceled normally.
	 */
	fun onCanceled()
}