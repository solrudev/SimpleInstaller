package io.github.solrudev.simpleinstaller

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.solrudev.simpleinstaller.activityresult.DeletePackageContract
import io.github.solrudev.simpleinstaller.activityresult.UninstallPackageContract
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.pendingIntentUpdateCurrentFlags
import io.github.solrudev.simpleinstaller.utils.showNotification
import kotlinx.coroutines.*
import kotlin.coroutines.resume

private const val PACKAGE_NAME_KEY = "PACKAGE_UNINSTALLER_PACKAGE_NAME"
private const val REQUEST_CODE = 4127

/**
 * A singleton which provides Android packages uninstall functionality.
 */
object PackageUninstaller {

	/**
	 * Property which reflects if there's already a uninstall session going on.
	 * If returns true, attempting to start a new uninstall session will result in [IllegalStateException].
	 */
	@JvmStatic
	var hasActiveSession = false
		private set

	/**
	 * Starts a uninstall session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm uninstallation and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 * @param [packageName] Name of the package to be uninstalled.
	 * @return Success of uninstallation.
	 */
	@JvmSynthetic
	suspend fun uninstallPackage(packageName: String) = suspendCancellableCoroutine<Boolean> { continuation ->
		continuation.invokeOnCancellation { onCancellation() }
		if (hasActiveSession) {
			continuation.cancel(IllegalStateException("Can't uninstall while another uninstall session is active."))
		}
		hasActiveSession = true
		uninstallerContinuation = continuation
		val activityIntent = Intent(
			SimpleInstaller.applicationContext,
			UninstallLauncherActivity::class.java
		).apply {
			putExtra(PACKAGE_NAME_KEY, packageName)
		}
		val fullScreenIntent = PendingIntent.getActivity(
			SimpleInstaller.applicationContext,
			REQUEST_CODE,
			activityIntent,
			pendingIntentUpdateCurrentFlags
		)
		showNotification(
			fullScreenIntent,
			++notificationId,
			R.string.ssi_prompt_uninstall_title,
			R.string.ssi_prompt_uninstall_message
		)
	}

	/**
	 * See [uninstallPackage].
	 *
	 * @param [packageName] Name of the package to be uninstalled.
	 * @param [callback] A callback object implementing [PackageUninstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun uninstallPackage(packageName: String, callback: PackageUninstallerCallback) {
		uninstallerScope.launch {
			try {
				val result = uninstallPackage(packageName)
				withContext(Dispatchers.Main) { callback.onFinished(result) }
			} catch (t: Throwable) {
				if (t is CancellationException) {
					withContext(Dispatchers.Main + NonCancellable) { callback.onCanceled() }
					return@launch
				}
				withContext(Dispatchers.Main + NonCancellable) { callback.onException(t) }
			} finally {
				coroutineContext[Job]?.cancel()
			}
		}
	}

	/**
	 * Cancels current coroutine and uninstall session.
	 */
	@JvmStatic
	fun cancel() {
		if (::uninstallerContinuation.isInitialized && uninstallerContinuation.isActive) {
			uninstallerContinuation.cancel()
		}
	}

	private val uninstallPackageContract =
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) DeletePackageContract() else UninstallPackageContract()

	private val uninstallerScope = CoroutineScope(Dispatchers.Default)
	private var notificationId = 34187
	private lateinit var uninstallerContinuation: CancellableContinuation<Boolean>

	private fun onCancellation() = try {
		notificationManager.cancel(notificationId)
	} catch (_: ApplicationContextNotSetException) {
	} finally {
		hasActiveSession = false
	}

	internal class UninstallLauncherActivity : AppCompatActivity() {

		private val uninstallLauncher = registerForActivityResult(uninstallPackageContract) {
			onCancellation()
			uninstallerContinuation.resume(it)
			finish()
		}

		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			turnScreenOnWhenLocked()
			if (savedInstanceState != null) return
			val packageName = intent.extras?.getString(PACKAGE_NAME_KEY)
			uninstallLauncher.launch(packageName)
		}

		override fun onDestroy() {
			super.onDestroy()
			clearTurnScreenOnSettings()
		}
	}
}