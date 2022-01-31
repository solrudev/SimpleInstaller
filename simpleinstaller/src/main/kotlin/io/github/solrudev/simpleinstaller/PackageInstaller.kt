@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.solrudev.simpleinstaller

import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.github.solrudev.simpleinstaller.activityresult.ActionInstallPackageContract
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.apksource.AssetFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import io.github.solrudev.simpleinstaller.apksource.utils.toApkSourceArray
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.utils.makeIndeterminate
import io.github.solrudev.simpleinstaller.data.utils.reset
import io.github.solrudev.simpleinstaller.data.utils.tryEmit
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.*
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import java.io.File
import kotlin.coroutines.resume

private const val APK_URI_KEY = "PACKAGE_INSTALLER_APK_URI"
private const val REQUEST_CODE = 6541

/**
 * A singleton which provides Android packages install functionality.
 */
object PackageInstaller {

	/**
	 * Property which reflects if there's already an install session going on.
	 * If returns true, attempting to start a new install session will result in [IllegalStateException].
	 */
	@JvmStatic
	var hasActiveSession = false
		private set

	private val _progress = MutableSharedFlow<ProgressData>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	/**
	 * A [SharedFlow] of [ProgressData] which represents installation progress.
	 */
	val progress = _progress.asSharedFlow()

	/**
	 * Starts an install session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation and suspends until it finishes.
	 *
	 * Split packages are not supported on Android versions lower than Lollipop (5.0).
	 * Attempting to use this method on these versions will produce [SplitPackagesNotSupportedException].
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 * @param [apkFiles] Any source of split APK files implemented by [ApkSource].
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	suspend fun installSplitPackage(vararg apkFiles: ApkSource): InstallResult {
		if (!usePackageInstallerApi) {
			throw SplitPackagesNotSupportedException()
		}
		return installPackages(apkFiles)
	}

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] [Uri]s of split APK files. Must be file: or content: URIs.
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	suspend fun installSplitPackage(vararg apkFiles: Uri): InstallResult {
		apkFiles.forEach {
			if (!isUriSupported(it)) {
				throw UnsupportedUriSchemeException(it)
			}
		}
		return installSplitPackage(*apkFiles.toApkSourceArray())
	}

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] [AssetFileDescriptor]s of split APK files.
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	suspend fun installSplitPackage(vararg apkFiles: AssetFileDescriptor) =
		installSplitPackage(*apkFiles.toApkSourceArray())

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] [File] objects representing split APK files.
	 * @return [InstallResult]
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	suspend fun installSplitPackage(vararg apkFiles: File) = installSplitPackage(*apkFiles.toApkSourceArray())

	/**
	 * Starts an install session, displays a full-screen intent or notification (depending on firmware)
	 * with prompting user to confirm installation and suspends until it finishes.
	 *
	 * It is safe to call on main thread. Supports cancellation.
	 * @param [apkFile] Any source of APK file implemented by [ApkSource].
	 * @return [InstallResult]
	 */
	@JvmSynthetic
	suspend fun installPackage(apkFile: ApkSource) = installPackages(arrayOf(apkFile))

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] [Uri] of APK file. Must be a file: or content: URI.
	 * @return [InstallResult]
	 */
	@JvmSynthetic
	suspend fun installPackage(apkFile: Uri) = installPackage(UriApkSource(apkFile))

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] [AssetFileDescriptor] of APK file.
	 * @return [InstallResult]
	 */
	@JvmSynthetic
	suspend fun installPackage(apkFile: AssetFileDescriptor) = installPackage(AssetFileDescriptorApkSource(apkFile))

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] [File] object representing APK file.
	 * @return [InstallResult]
	 */
	@JvmSynthetic
	suspend fun installPackage(apkFile: File) = installPackage(FileApkSource(apkFile))

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] Any source of split APK files implemented by [ApkSource].
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmStatic
	fun installSplitPackage(vararg apkFiles: ApkSource, callback: PackageInstallerCallback) {
		if (!usePackageInstallerApi) {
			throw SplitPackagesNotSupportedException()
		}
		installPackages(apkFiles, callback)
	}

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] [Uri]s of split APK files. Must be file: or content: URIs.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmStatic
	fun installSplitPackage(vararg apkFiles: Uri, callback: PackageInstallerCallback) {
		apkFiles.forEach {
			if (!isUriSupported(it)) {
				throw UnsupportedUriSchemeException(it)
			}
		}
		installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)
	}

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] [AssetFileDescriptor]s of split APK files.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmStatic
	fun installSplitPackage(vararg apkFiles: AssetFileDescriptor, callback: PackageInstallerCallback) =
		installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)

	/**
	 * See [installSplitPackage].
	 *
	 * @param [apkFiles] [File] objects representing split APK files.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmStatic
	fun installSplitPackage(vararg apkFiles: File, callback: PackageInstallerCallback) =
		installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] Any source of APK file implemented by [ApkSource].
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: ApkSource, callback: PackageInstallerCallback) =
		installPackages(arrayOf(apkFile), callback)

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] [Uri] of APK file. Must be a file: or content: URI.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: Uri, callback: PackageInstallerCallback) =
		installPackage(UriApkSource(apkFile), callback)

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] [AssetFileDescriptor] of APK file.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: AssetFileDescriptor, callback: PackageInstallerCallback) =
		installPackage(AssetFileDescriptorApkSource(apkFile), callback)

	/**
	 * See [installPackage].
	 *
	 * @param [apkFile] [File] object representing APK file.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: File, callback: PackageInstallerCallback) =
		installPackage(FileApkSource(apkFile), callback)

	/**
	 * Cancels current coroutine and abandons current install session.
	 */
	@JvmStatic
	fun cancel() {
		if (::installerContinuation.isInitialized && installerContinuation.isActive) {
			installerContinuation.cancel()
		}
	}

	private val actionInstallationStatus by lazy { "${SimpleInstaller.applicationContext.packageName}.INSTALLATION_STATUS" }
	private val actionInstallPackageContract = ActionInstallPackageContract()
	private val installerScope = CoroutineScope(Dispatchers.Default)
	private var notificationId = 18475
	private var currentApkSources: Array<out ApkSource> = emptyArray()
	private var activityFirstCreated = true
	private lateinit var installerContinuation: CancellableContinuation<InstallResult>

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
			if (intent.action != actionInstallationStatus) {
				return
			}
			val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
			val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
			if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
				val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
				if (confirmationIntent != null) {
					val wrapperIntent = Intent(context, InstallLauncherActivity::class.java)
						.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
						.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					context.startActivity(wrapperIntent)
				} else {
					installerContinuation.cancel(IllegalArgumentException("confirmationIntent was null."))
				}
				return
			}
			val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
			val otherPackageName = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
			val storagePath = intent.getStringExtra(PackageInstaller.EXTRA_STORAGE_PATH)
			finishInstallation(InstallResult.getFromStatusCode(status, message, otherPackageName, storagePath))
		}
	}

	private val packageInstallerSessionObserver by lazy {
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : PackageInstaller.SessionCallback() {
			override fun onCreated(sessionId: Int) {}
			override fun onBadgingChanged(sessionId: Int) {}
			override fun onActiveChanged(sessionId: Int, active: Boolean) {}
			override fun onFinished(sessionId: Int, success: Boolean) {}

			override fun onProgressChanged(sessionId: Int, progress: Float) {
				_progress.tryEmit((progress * 100).toInt(), 100)
			}
		}
	}

	private fun installPackages(apkFiles: Array<out ApkSource>, callback: PackageInstallerCallback) {
		installerScope.launch {
			try {
				launch(Dispatchers.Main) {
					progress.collect { callback.onProgressChanged(it) }
				}
				when (val result = installPackages(apkFiles)) {
					is InstallResult.Success -> withContext(Dispatchers.Main) { callback.onSuccess() }
					is InstallResult.Failure -> withContext(Dispatchers.Main) { callback.onFailure(result.cause) }
				}
			} catch (e: Throwable) {
				if (e is CancellationException) {
					withContext(Dispatchers.Main + NonCancellable) { callback.onCanceled() }
					return@launch
				}
				withContext(Dispatchers.Main + NonCancellable) { callback.onException(e) }
			} finally {
				withContext(Dispatchers.Main + NonCancellable) { callback.onProgressChanged(ProgressData()) }
				coroutineContext[Job]?.cancel()
			}
		}
	}

	private suspend fun installPackages(apkFiles: Array<out ApkSource>): InstallResult {
		if (apkFiles.isEmpty()) {
			return InstallResult.Failure(InstallFailureCause.Generic("No APKs provided."))
		}
		return suspendCancellableCoroutine { continuation ->
			var sessionId = -1
			continuation.invokeOnCancellation {
				if (usePackageInstallerApi) {
					abandonSession(sessionId)
				}
				onCancellation()
			}
			if (hasActiveSession) {
				continuation.cancel(IllegalStateException("Can't install while another install session is active."))
			}
			hasActiveSession = true
			installerContinuation = continuation
			CoroutineScope(continuation.context + Dispatchers.IO).launch main@{
				try {
					currentApkSources = apkFiles
					if (!usePackageInstallerApi) {
						if (apkFiles.size > 1) {
							throw SplitPackagesNotSupportedException()
						}
						val apkFile = apkFiles.first()
						val progressJob = launch {
							_progress.emitAll(apkFile.progress)
						}
						val apkUri = apkFile.getUri()
						progressJob.cancel()
						_progress.makeIndeterminate()
						displayNotification(apkUri = apkUri)
						return@main
					}
					localBroadcastManager.registerReceiver(
						installationEventsReceiver,
						IntentFilter(actionInstallationStatus)
					)
					val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)
					}
					sessionId = packageInstaller.createSession(sessionParams)
					packageInstaller.openSession(sessionId).use { session ->
						withContext(Dispatchers.Main) {
							packageInstaller.registerSessionCallback(packageInstallerSessionObserver)
						}
						session.copyApksFrom(apkFiles)
						displayNotification(sessionId = sessionId)
					}
				} catch (e: Throwable) {
					continuation.cancel(e)
				}
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private suspend inline fun PackageInstaller.Session.copyApksFrom(apkFiles: Array<out ApkSource>) {
		val totalLength = apkFiles.map { it.length }.sum()
		var transferredBytes = 0L
		for ((index, apkFile) in apkFiles.withIndex()) {
			val apkStream = apkFile.openInputStream()
			val apkLength = apkFile.length
			val sessionStream = openWrite("temp$index.apk", 0, apkLength)
			copy(
				requireNotNull(apkStream) { "APK $index InputStream was null." },
				sessionStream,
				totalLength,
				transferredBytes
			) { progressData -> setStagingProgress(progressData.progress.toFloat() / progressData.max.coerceAtLeast(1)) }
			transferredBytes += apkLength
		}
	}

	private fun isUriSupported(uri: Uri) =
		uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == ContentResolver.SCHEME_CONTENT

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun abandonSession(sessionId: Int) = try {
		packageInstaller.abandonSession(sessionId)
	} catch (_: Throwable) {
	}

	private fun onCancellation() = try {
		if (usePackageInstallerApi) {
			packageInstaller.unregisterSessionCallback(packageInstallerSessionObserver)
			localBroadcastManager.unregisterReceiver(installationEventsReceiver)
		}
		currentApkSources.forEach { it.clearTempFiles() }
		currentApkSources = emptyArray()
		notificationManager.cancel(notificationId)
	} catch (_: ApplicationContextNotSetException) {
	} finally {
		CoroutineScope(installerContinuation.context + NonCancellable).launch {
			_progress.reset()
			installFinishedCallback.emit(Unit)
			activityFirstCreated = true
			hasActiveSession = false
		}
	}

	private fun finishInstallation(result: InstallResult) {
		onCancellation()
		installerContinuation.resume(result)
	}

	private fun displayNotification(sessionId: Int? = null, apkUri: Uri? = null) {
		val activityIntent = Intent(
			SimpleInstaller.applicationContext,
			InstallLauncherActivity::class.java
		).apply {
			if (usePackageInstallerApi && sessionId != null) {
				putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
			}
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
			++notificationId,
			R.string.ssi_prompt_install_title,
			R.string.ssi_prompt_install_message
		)
	}

	internal class InstallLauncherActivity : AppCompatActivity() {

		private val actionInstallPackageLauncher = registerForActivityResult(actionInstallPackageContract) {
			val result = if (it) InstallResult.Success else InstallResult.Failure()
			finishInstallation(result)
		}

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		private val confirmationIntentLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
				intent.extras?.getInt(PackageInstaller.EXTRA_SESSION_ID)?.let main@{ sessionId ->
					packageInstaller.getSessionInfo(sessionId)?.let { sessionInfo ->
						// Hacky workaround: progress not going higher than 0.8 means install failed.
						// This is needed to resume the coroutine with failure on reasons which are not
						// handled in installationEventsReceiver. For example, "There was a problem
						// parsing the package" error falls under that.
						if (sessionInfo.progress < 0.81 && installerContinuation.isActive) {
							abandonSession(sessionId)
							finishInstallation(InstallResult.Failure())
							return@main
						}
					}
					// If session doesn't exist anymore.
					finish()
				}
			}

		override fun onCreate(savedInstanceState: Bundle?) {
			super.onCreate(savedInstanceState)
			turnScreenOnWhenLocked()
			lifecycleScope.launch {
				installFinishedCallback
					.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
					.collect { finish() }
			}
			if (savedInstanceState != null) return
			if (activityFirstCreated) {
				activityFirstCreated = false
				if (usePackageInstallerApi) {
					commitSession()
				}
			}
			if (usePackageInstallerApi) {
				intent.extras?.getParcelable<Intent>(Intent.EXTRA_INTENT)?.let {
					confirmationIntentLauncher.launch(it)
				}
			} else {
				intent.extras?.getParcelable<Uri>(APK_URI_KEY)?.let {
					actionInstallPackageLauncher.launch(it)
				}
			}
		}

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		private fun commitSession() {
			val receiverIntent = Intent(
				SimpleInstaller.applicationContext,
				InstallationEventsReceiver::class.java
			).apply {
				action = actionInstallationStatus
			}
			val receiverPendingIntent = PendingIntent.getBroadcast(
				SimpleInstaller.applicationContext,
				REQUEST_CODE,
				receiverIntent,
				pendingIntentUpdateCurrentFlags
			)
			val statusReceiver = receiverPendingIntent.intentSender
			val sessionId = intent.extras?.getInt(PackageInstaller.EXTRA_SESSION_ID)
			packageInstaller.openSession(sessionId!!).commit(statusReceiver)
		}

		override fun onNewIntent(intent: Intent?) {
			super.onNewIntent(intent)
			startActivity(intent)
			finish()
		}

		override fun onDestroy() {
			super.onDestroy()
			clearTurnScreenOnSettings()
		}
	}
}