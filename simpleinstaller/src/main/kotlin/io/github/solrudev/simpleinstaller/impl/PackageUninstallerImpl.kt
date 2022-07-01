package io.github.solrudev.simpleinstaller.impl

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.solrudev.simpleinstaller.PackageUninstaller
import io.github.solrudev.simpleinstaller.R
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.activityresult.DeletePackageContract
import io.github.solrudev.simpleinstaller.activityresult.UninstallPackageContract
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import io.github.solrudev.simpleinstaller.utils.getApplicationLabel
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.pendingIntentUpdateCurrentFlags
import io.github.solrudev.simpleinstaller.utils.showNotification
import kotlinx.coroutines.*
import kotlin.coroutines.resume

private const val PACKAGE_NAME_KEY = "PACKAGE_UNINSTALLER_PACKAGE_NAME"
private const val REQUEST_CODE = 4127

/**
 * A concrete implementation of [PackageUninstaller].
 */
internal object PackageUninstallerImpl : PackageUninstaller {

	override var hasActiveSession = false
		private set

	override suspend fun uninstallPackage(packageName: String) = suspendCancellableCoroutine { continuation ->
		continuation.invokeOnCancellation { onCancellation() }
		if (hasActiveSession) {
			continuation.cancel(IllegalStateException("Can't uninstall while another uninstall session is active."))
		}
		hasActiveSession = true
		uninstallerContinuation = continuation
		val activityIntent = Intent(
			applicationContext,
			UninstallLauncherActivity::class.java
		).apply {
			putExtra(PACKAGE_NAME_KEY, packageName)
		}
		val fullScreenIntent = PendingIntent.getActivity(
			applicationContext,
			REQUEST_CODE,
			activityIntent,
			pendingIntentUpdateCurrentFlags
		)
		val appLabel = getApplicationLabel(packageName)
		val message = if (appLabel != null) {
			applicationContext.getString(R.string.ssi_prompt_uninstall_message_with_label, appLabel)
		} else {
			applicationContext.getString(R.string.ssi_prompt_uninstall_message)
		}
		showNotification(
			fullScreenIntent,
			++notificationId,
			R.string.ssi_prompt_uninstall_title,
			message
		)
	}

	override fun uninstallPackage(packageName: String, callback: PackageUninstaller.Callback) {
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

	override fun cancel() {
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
			if (uninstallerContinuation.isActive) {
				uninstallerContinuation.resume(it)
			}
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