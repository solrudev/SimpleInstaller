@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.solrudev.simpleinstaller

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.solrudev.simpleinstaller.activityresult.ActionInstallPackageContract
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.*
import io.github.solrudev.simpleinstaller.utils.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

private const val APK_URI_KEY = "PACKAGE_INSTALLER_APK_URI"
private const val TEMP_FILE_NAME = "temp.apk"
private const val REQUEST_CODE = 6541
private const val ACTION_INSTALLATION_STATUS = "solrudev.simpleinstaller.INSTALLATION_STATUS"

object PackageInstaller {

	/**
	 * Property which reflects if there's already an install session going on.
	 * If returns true, attempting to start a new install session will result in [IllegalStateException].
	 */
	var hasActiveSession = false
		private set

	private val _progress = MutableSharedFlow<ProgressData>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	/**
	 * A [SharedFlow] of [ProgressData] which represents installation progress.
	 */
	val progress: SharedFlow<ProgressData> = _progress.asSharedFlow()

	/**
	 * Starts an install session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation and suspends until it finishes.
	 *
	 * Split packages are not supported on Android versions lower than Lollipop (5.0).
	 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 * @param [apkFiles] [Uri]s of split APK files. Must be file: or content: URIs.
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	suspend fun installSplitPackage(vararg apkFiles: Uri): InstallResult {
		if (!usePackageInstallerApi) {
			throw SplitPackagesNotSupportedException()
		}
		return installPackages(*apkFiles)
	}

	/**
	 * Starts an install session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 * @param [apkFile] [Uri] of APK file. Must be a file: or content: URI.
	 * @return [InstallResult]
	 */
	suspend fun installPackage(apkFile: Uri) = installPackages(apkFile)

	@JvmStatic
	private var NOTIFICATION_ID = 18475

	private lateinit var capturedContinuation: CancellableContinuation<InstallResult>
	private val contract = ActionInstallPackageContract()
	private var activityFirstCreated = true
	private val tempApk by lazy { File(SimpleInstaller.applicationContext.externalCacheDir, TEMP_FILE_NAME) }

	private val installFinishedCallback = MutableSharedFlow<Unit>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	private val localBroadcastManager
		get() = LocalBroadcastManager.getInstance(SimpleInstaller.applicationContext)

	private val packageInstaller
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		get() = SimpleInstaller.applicationContext.packageManager.packageInstaller

	private val usePackageInstallerApi
		@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

	private val installationEventsReceiver = object : BroadcastReceiver() {
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		override fun onReceive(context: Context, intent: Intent) {
			if (intent.action != ACTION_INSTALLATION_STATUS) {
				return
			}
			val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
			if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
				val confirmationIntent = intent
					.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
					?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				if (confirmationIntent != null) {
					context.startActivity(confirmationIntent)
				} else {
					capturedContinuation.cancel(IllegalArgumentException("confirmationIntent was null."))
				}
				return
			}
			val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
			val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
			val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
			localBroadcastManager.unregisterReceiver(this)
			finishInstallation(InstallResult.getFromStatusCode(status, message, otherPackageName, storagePath))
		}
	}

	private val packageInstallerSessionObserver by lazy {
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : PackageInstaller.SessionCallback() {
			override fun onCreated(sessionId: Int) {}
			override fun onBadgingChanged(sessionId: Int) {}
			override fun onActiveChanged(sessionId: Int, active: Boolean) {}

			override fun onProgressChanged(sessionId: Int, progress: Float) {
				_progress.tryEmit((progress * 100).toInt(), 100)
			}

			override fun onFinished(sessionId: Int, success: Boolean) {
				packageInstaller.unregisterSessionCallback(this)
			}
		}
	}

	private suspend fun installPackages(vararg apkFiles: Uri): InstallResult {
		if (apkFiles.isEmpty()) {
			return InstallResult.Failure(InstallFailureCause.Generic("No APKs provided."))
		}
		val capturedCoroutineContext = coroutineContext
		return suspendCancellableCoroutine { continuation ->
			var session: PackageInstaller.Session? = null
			continuation.invokeOnCancellation {
				if (usePackageInstallerApi) {
					abandonSession(session)
				}
				onCancellation()
			}
			capturedContinuation = continuation
			if (hasActiveSession) {
				continuation.cancel(IllegalStateException("Can't install while another install session is active."))
			}
			hasActiveSession = true
			CoroutineScope(capturedCoroutineContext + Dispatchers.IO).launch {
				try {
					if (!usePackageInstallerApi) {
						if (apkFiles.size > 1) {
							throw SplitPackagesNotSupportedException()
						}
						val apkUri = when (apkFiles.first().scheme) {
							ContentResolver.SCHEME_CONTENT -> createTempCopy(apkFiles.first())
							ContentResolver.SCHEME_FILE -> apkFiles.first()
							else -> throw UnsupportedUriSchemeException(apkFiles.first())
						}
						_progress.makeIndeterminate()
						displayNotification(apkUri)
						return@launch
					}
					apkFiles.forEach {
						if (!isUriSupported(it)) {
							throw UnsupportedUriSchemeException(it)
						}
					}
					localBroadcastManager.registerReceiver(
						installationEventsReceiver,
						IntentFilter(ACTION_INSTALLATION_STATUS)
					)
					val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)
					}
					val sessionId = packageInstaller.createSession(sessionParams)
					session = packageInstaller.openSession(sessionId)
					session!!.use {
						withContext(Dispatchers.Main) {
							packageInstaller.registerSessionCallback(packageInstallerSessionObserver)
						}
						it.copyApksFrom(*apkFiles)
						val intent = Intent(
							SimpleInstaller.applicationContext,
							InstallationEventsReceiver::class.java
						).apply {
							action = ACTION_INSTALLATION_STATUS
						}
						val pendingIntent = PendingIntent.getBroadcast(
							SimpleInstaller.applicationContext,
							REQUEST_CODE,
							intent,
							pendingIntentUpdateCurrentFlags
						)
						val statusReceiver = pendingIntent.intentSender
						displayNotification()
						waitUntilActivityLaunched()
						it.commit(statusReceiver)
					}
				} catch (e: Throwable) {
					continuation.cancel(e)
				}
			}
		}
	}

	private suspend inline fun createTempCopy(apkFile: Uri): Uri {
		tempApk.delete()
		tempApk.createNewFile()
		val inputStream = SimpleInstaller.applicationContext.contentResolver.openInputStream(apkFile)
		val outputStream = FileOutputStream(tempApk)
		copy(
			requireNotNull(inputStream) { "APK InputStream was null." },
			outputStream,
			apkFile.length,
			_progress
		)
		return Uri.fromFile(tempApk)
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private suspend inline fun PackageInstaller.Session.copyApksFrom(vararg apkFiles: Uri) = coroutineScope {
		val progressFlow = MutableSharedFlow<ProgressData>(
			replay = 0,
			extraBufferCapacity = 1,
			onBufferOverflow = BufferOverflow.DROP_OLDEST
		)
		val progressJob = launch {
			progressFlow.collect { setStagingProgress(it.progress.toFloat() / it.max) }
		}
		val totalSize = apkFiles.map { it.length }.sum()
		var transferredBytes = 0L
		for ((index, apkFile) in apkFiles.withIndex()) {
			val apkStream = SimpleInstaller.applicationContext.contentResolver.openInputStream(apkFile)
			val apkSize = apkFile.length
			val sessionStream = openWrite("temp$index.apk", 0, apkSize)
			copy(
				requireNotNull(apkStream) { "APK $index InputStream was null." },
				sessionStream,
				totalSize,
				progressFlow,
				transferredBytes
			)
			transferredBytes += apkSize
		}
		progressJob.cancel()
	}

	private fun isUriSupported(uri: Uri) =
		uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == ContentResolver.SCHEME_CONTENT

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun abandonSession(session: PackageInstaller.Session?) = try {
		session?.abandon()
	} catch (_: Throwable) {
	}

	private fun onCancellation() = try {
		if (usePackageInstallerApi) {
			packageInstaller.unregisterSessionCallback(packageInstallerSessionObserver)
			localBroadcastManager.unregisterReceiver(installationEventsReceiver)
		} else if (tempApk.exists()) {
			tempApk.delete()
		}
		notificationManager.cancel(NOTIFICATION_ID)
	} catch (_: ApplicationContextNotSetException) {
	} finally {
		CoroutineScope(capturedContinuation.context + NonCancellable).launch {
			_progress.reset()
			installFinishedCallback.emit(Unit)
			activityFirstCreated = true
			hasActiveSession = false
		}
	}

	private fun finishInstallation(result: InstallResult) {
		onCancellation()
		capturedContinuation.resume(result)
	}

	private fun displayNotification(apkUri: Uri? = null) {
		val activityIntent = Intent(
			SimpleInstaller.applicationContext,
			InstallLauncherActivity::class.java
		).apply {
			apkUri?.let { putExtra(APK_URI_KEY, it) }
		}
		val fullScreenIntent = PendingIntent.getActivity(
			SimpleInstaller.applicationContext,
			REQUEST_CODE,
			activityIntent,
			pendingIntentCancelCurrentFlags
		)
		showNotification(
			fullScreenIntent,
			++NOTIFICATION_ID,
			R.string.ssi_prompt_install_title,
			R.string.ssi_prompt_install_message
		)
	}

	private var activityLaunchedContinuation: CancellableContinuation<Unit>? = null

	private suspend inline fun waitUntilActivityLaunched() = suspendCancellableCoroutine<Unit> {
		it.invokeOnCancellation { onCancellation() }
		activityLaunchedContinuation = it
	}

	class InstallLauncherActivity : AppCompatActivity() {

		private var onRestartCalled = false

		private val installLauncher = registerForActivityResult(contract) {
			val result = if (it) InstallResult.Success else InstallResult.Failure()
			finishInstallation(result)
		}

		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			turnScreenOnWhenLocked()
			if (activityFirstCreated && !isRestarted(savedInstanceState)) {
				activityFirstCreated = false
				activityLaunchedContinuation?.resume(Unit)
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				onRestartCalled = false
				finish()
				return
			}
			lifecycleScope.launchWhenResumed {
				installFinishedCallback.collect { finish() }
			}
			if (isRestarted(savedInstanceState)) {
				onRestartCalled = false
				return
			}
			val apkUri = intent.extras?.getParcelable<Uri>(APK_URI_KEY)
			installLauncher.launch(apkUri)
		}

		override fun onRestart() {
			super.onRestart()
			onRestartCalled = true
		}

		override fun onSaveInstanceState(outState: Bundle) {
			super.onSaveInstanceState(outState)
			outState.putBoolean(KEY_RECREATED, true)
		}

		override fun onDestroy() {
			super.onDestroy()
			activityFirstCreated = false
			clearTurnScreenOnSettings()
		}

		private fun isRestarted(savedInstanceState: Bundle?) =
			savedInstanceState?.getBoolean(KEY_RECREATED) == true || onRestartCalled

		companion object {
			private const val KEY_RECREATED = "RECREATED"
		}
	}
}