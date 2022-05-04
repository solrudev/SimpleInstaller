package io.github.solrudev.simpleinstaller

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.impl.PackageInstallerImpl
import kotlinx.coroutines.flow.SharedFlow

/**
 * Provides Android packages install functionality.
 */
interface PackageInstaller {

	/**
	 * Property which reflects if there's already an install session going on.
	 * If returns true, attempting to start a new install session will result in [IllegalStateException].
	 */
	val hasActiveSession: Boolean

	/**
	 * A [SharedFlow] of [ProgressData] which represents installation progress.
	 */
	val progress: SharedFlow<ProgressData>

	/**
	 * Starts an install session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation and suspends until it finishes.
	 *
	 * Split packages are not supported on Android versions lower than Lollipop (5.0).
	 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 *
	 * @param apkFiles Any source of split APK files implemented by [ApkSource].
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	suspend fun installSplitPackage(vararg apkFiles: ApkSource): InstallResult

	/**
	 * Starts an install session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 *
	 * @param apkFile Any source of APK file implemented by [ApkSource].
	 * @return [InstallResult]
	 */
	@JvmSynthetic
	suspend fun installPackage(apkFile: ApkSource): InstallResult

	/**
	 * Starts an install session and displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation.
	 *
	 * Split packages are not supported on Android versions lower than Lollipop (5.0).
	 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
	 *
	 * @param apkFiles Any source of split APK files implemented by [ApkSource].
	 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	fun installSplitPackage(vararg apkFiles: ApkSource, callback: Callback)

	/**
	 * Starts an install session and displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation.
	 *
	 * @param apkFile Any source of APK file implemented by [ApkSource].
	 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
	 * Its methods are called on main thread.
	 */
	fun installPackage(apkFile: ApkSource, callback: Callback)

	/**
	 * Cancels current coroutine and abandons current install session.
	 */
	fun cancel()

	/**
	 * Default singleton instance of [PackageInstaller].
	 */
	companion object : PackageInstaller by PackageInstallerImpl {

		/**
		 * Retrieves the default singleton instance of [PackageInstaller].
		 */
		@JvmStatic
		fun getInstance(): PackageInstaller = PackageInstallerImpl
	}

	/**
	 * A callback interface for [PackageInstaller]'s usage from Java.
	 */
	interface Callback {

		/**
		 * Called when installation finished with success.
		 */
		fun onSuccess()

		/**
		 * Called when installation finished with failure.
		 * @param cause Cause of failure. Always null on Android versions lower than Lollipop (5.0).
		 */
		fun onFailure(cause: InstallFailureCause?)

		/**
		 * Called when an exception is thrown.
		 * @param exception A [Throwable] which has been thrown.
		 */
		fun onException(exception: Throwable)

		/**
		 * Called when installation was canceled normally.
		 */
		fun onCanceled()

		/**
		 * Called when installation progress has been updated.
		 * @param progress [ProgressData] object representing installation progress.
		 */
		fun onProgressChanged(progress: ProgressData)
	}
}