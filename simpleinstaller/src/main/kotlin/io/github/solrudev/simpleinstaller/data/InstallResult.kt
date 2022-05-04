package io.github.solrudev.simpleinstaller.data

import android.content.pm.PackageInstaller
import io.github.solrudev.simpleinstaller.data.InstallResult.Failure
import io.github.solrudev.simpleinstaller.data.InstallResult.Success

/**
 * Represents result of installation.
 *
 * May be [Success] or [Failure].
 */
sealed class InstallResult {

	/**
	 * Install succeeded.
	 */
	object Success : InstallResult()

	/**
	 * Install failed.
	 *
	 * May contain cause of failure in [cause] property.
	 * @property cause Cause of installation failure. Always null on Android versions lower than Lollipop (5.0).
	 */
	data class Failure(val cause: InstallFailureCause? = null) : InstallResult()

	companion object {

		/**
		 * Converts Android's [PackageInstaller] status code to [InstallResult] object.
		 */
		fun fromStatusCode(
			statusCode: Int,
			message: String? = null,
			otherPackageName: String? = null,
			storagePath: String? = null
		) = when (statusCode) {
			PackageInstaller.STATUS_SUCCESS -> Success
			PackageInstaller.STATUS_FAILURE -> Failure(InstallFailureCause.Generic(message))
			PackageInstaller.STATUS_FAILURE_ABORTED -> Failure(InstallFailureCause.Aborted(message))
			PackageInstaller.STATUS_FAILURE_BLOCKED -> Failure(InstallFailureCause.Blocked(message, otherPackageName))
			PackageInstaller.STATUS_FAILURE_CONFLICT -> Failure(InstallFailureCause.Conflict(message, otherPackageName))
			PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> Failure(InstallFailureCause.Incompatible(message))
			PackageInstaller.STATUS_FAILURE_INVALID -> Failure(InstallFailureCause.Invalid(message))
			PackageInstaller.STATUS_FAILURE_STORAGE -> Failure(InstallFailureCause.Storage(message, storagePath))
			else -> Failure()
		}
	}
}