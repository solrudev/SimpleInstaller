package io.github.solrudev.simpleinstaller

import io.github.solrudev.simpleinstaller.impl.PackageUninstallerImpl

/**
 * Provides Android packages uninstall functionality.
 */
interface PackageUninstaller {

	/**
	 * Property which reflects if there's already a uninstall session going on.
	 * If returns true, attempting to start a new uninstall session will result in [IllegalStateException].
	 */
	val hasActiveSession: Boolean

	/**
	 * Starts a uninstall session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm uninstallation and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 *
	 * @param packageName Name of the package to be uninstalled.
	 * @return Success of uninstallation.
	 */
	@JvmSynthetic
	suspend fun uninstallPackage(packageName: String): Boolean

	/**
	 * Starts a uninstall session and displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm uninstallation.
	 *
	 * @param packageName Name of the package to be uninstalled.
	 * @param callback A callback object implementing [PackageUninstaller.Callback] interface.
	 * Its methods are called on main thread.
	 */
	fun uninstallPackage(packageName: String, callback: Callback)

	/**
	 * Cancels current coroutine and uninstall session.
	 */
	fun cancel()

	/**
	 * Default singleton instance of [PackageUninstaller].
	 */
	companion object : PackageUninstaller by PackageUninstallerImpl.instance {

		/**
		 * Retrieves the default singleton instance of [PackageUninstaller].
		 */
		@JvmStatic
		fun getInstance(): PackageUninstaller = PackageUninstallerImpl.instance
	}

	/**
	 * A callback interface for [PackageUninstaller]'s usage from Java.
	 */
	interface Callback {

		/**
		 * Called when uninstallation finished, either with success or failure.
		 * @param success Success of uninstallation.
		 */
		fun onFinished(success: Boolean)

		/**
		 * Called when an exception is thrown.
		 * @param exception A [Throwable] which has been thrown.
		 */
		fun onException(exception: Throwable)

		/**
		 * Called when uninstallation was canceled normally.
		 */
		fun onCanceled()
	}
}