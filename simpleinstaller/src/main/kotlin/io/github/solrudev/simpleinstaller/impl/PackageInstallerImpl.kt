@file:Suppress("BlockingMethodInNonBlockingContext")

package io.github.solrudev.simpleinstaller.impl

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import io.github.solrudev.simpleinstaller.PackageInstaller
import io.github.solrudev.simpleinstaller.R
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.SimpleInstaller.installerPackageName
import io.github.solrudev.simpleinstaller.SimpleInstaller.packageManager
import io.github.solrudev.simpleinstaller.activityresult.ActionInstallPackageContract
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.data.InstallFailureCause
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.utils.makeIndeterminate
import io.github.solrudev.simpleinstaller.data.utils.tryEmit
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException
import io.github.solrudev.simpleinstaller.exceptions.SplitPackagesNotSupportedException
import io.github.solrudev.simpleinstaller.utils.*
import io.github.solrudev.simpleinstaller.utils.extensions.clearTurnScreenOnSettings
import io.github.solrudev.simpleinstaller.utils.extensions.turnScreenOnWhenLocked
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.coroutines.resume
import android.content.pm.PackageInstaller as AndroidPackageInstaller

private const val APK_URI_KEY = "PACKAGE_INSTALLER_APK_URI"
private const val REQUEST_CODE = 6541

/**
 * A concrete implementation of [PackageInstaller].
 */
internal object PackageInstallerImpl : PackageInstaller {

	override var hasActiveSession = false
		private set

	private val _progress = MutableSharedFlow<ProgressData>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	override val progress = _progress.asSharedFlow()

	override suspend fun installSplitPackage(vararg apkFiles: ApkSource): InstallResult {
		if (!usePackageInstallerApi) {
			throw SplitPackagesNotSupportedException()
		}
		return installPackages(apkFiles)
	}

	override suspend fun installPackage(apkFile: ApkSource) = installPackages(arrayOf(apkFile))

	override fun installSplitPackage(vararg apkFiles: ApkSource, callback: PackageInstaller.Callback) {
		if (!usePackageInstallerApi) {
			throw SplitPackagesNotSupportedException()
		}
		installPackages(apkFiles, callback)
	}

	override fun installPackage(apkFile: ApkSource, callback: PackageInstaller.Callback) =
		installPackages(arrayOf(apkFile), callback)

	override fun cancel() {
		if (::installerContinuation.isInitialized && installerContinuation.isActive) {
			installerContinuation.cancel()
		}
	}

	private val actionInstallationStatus by lazy { "${installerPackageName}.INSTALLATION_STATUS" }
	private val actionInstallPackageContract = ActionInstallPackageContract()
	private val installerScope = CoroutineScope(Dispatchers.Default)
	private val isInstallFinished = MutableStateFlow(false)
	private var notificationId = 18475
	private var currentApkSources: Array<out ApkSource> = emptyArray()
	private var activityFirstCreated = true
	private lateinit var installerContinuation: CancellableContinuation<InstallResult>

	private val packageInstaller
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		get() = packageManager.packageInstaller

	private val usePackageInstallerApi
		@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
		get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

	private val packageInstallerSessionObserver by lazy {
		@RequiresApi(Build.VERSION_CODES.LOLLIPOP) object : AndroidPackageInstaller.SessionCallback() {
			override fun onCreated(sessionId: Int) {}
			override fun onBadgingChanged(sessionId: Int) {}
			override fun onActiveChanged(sessionId: Int, active: Boolean) {}
			override fun onFinished(sessionId: Int, success: Boolean) {}

			override fun onProgressChanged(sessionId: Int, progress: Float) {
				_progress.tryEmit((progress * 100).toInt(), 100)
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmSynthetic
	internal fun onPackageInstallerStatusChanged(context: Context, intent: Intent) {
		if (intent.action != actionInstallationStatus) {
			return
		}
		val sessionId = intent.getIntExtra(AndroidPackageInstaller.EXTRA_SESSION_ID, -1)
		val status = intent.getIntExtra(AndroidPackageInstaller.EXTRA_STATUS, -1)
		if (status == AndroidPackageInstaller.STATUS_PENDING_USER_ACTION) {
			val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
			if (confirmationIntent != null) {
				val wrapperIntent = Intent(context, InstallLauncherActivity::class.java)
					.putExtra(Intent.EXTRA_INTENT, confirmationIntent)
					.putExtra(AndroidPackageInstaller.EXTRA_SESSION_ID, sessionId)
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(wrapperIntent)
			} else {
				installerContinuation.cancel(IllegalArgumentException("confirmationIntent was null."))
			}
			return
		}
		val message = intent.getStringExtra(AndroidPackageInstaller.EXTRA_STATUS_MESSAGE)
		val otherPackageName = intent.getStringExtra(AndroidPackageInstaller.EXTRA_OTHER_PACKAGE_NAME)
		val storagePath = intent.getStringExtra(AndroidPackageInstaller.EXTRA_STORAGE_PATH)
		finishInstallation(InstallResult.fromStatusCode(status, message, otherPackageName, storagePath))
	}

	private fun installPackages(apkFiles: Array<out ApkSource>, callback: PackageInstaller.Callback) {
		installerScope.launch {
			try {
				launch(Dispatchers.Main) {
					progress.collect { callback.onProgressChanged(it) }
				}
				when (val result = installPackages(apkFiles)) {
					is InstallResult.Success -> withContext(Dispatchers.Main) { callback.onSuccess() }
					is InstallResult.Failure -> withContext(Dispatchers.Main) { callback.onFailure(result.cause) }
				}
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
						val appLabel = getApplicationLabel(apkFile.file)
						displayNotification(appLabel, apkUri = apkUri)
						return@main
					}
					val sessionParams =
						AndroidPackageInstaller.SessionParams(AndroidPackageInstaller.SessionParams.MODE_FULL_INSTALL)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER)
					}
					sessionId = packageInstaller.createSession(sessionParams)
					packageInstaller.openSession(sessionId).use { session ->
						withContext(Dispatchers.Main) {
							packageInstaller.registerSessionCallback(packageInstallerSessionObserver)
						}
						session.copyApksFrom(apkFiles)
						val sessionInfo = packageInstaller.getSessionInfo(sessionId)
						val appLabel = sessionInfo?.buildSessionDir(sessionParams)?.let { sessionDir ->
							val file = File(sessionDir, session.names.first())
							getApplicationLabel(file)
						}
						displayNotification(appLabel, sessionId = sessionId)
					}
				} catch (t: Throwable) {
					continuation.cancel(t)
				}
			}
		}
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private suspend inline fun AndroidPackageInstaller.Session.copyApksFrom(apkFiles: Array<out ApkSource>) {
		val totalLength = apkFiles.sumOf { it.length }
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
			) { progressData ->
				setStagingProgress(progressData.progress.toFloat() / progressData.max.coerceAtLeast(1))
			}
			transferredBytes += apkLength
		}
	}

	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	private fun abandonSession(sessionId: Int) = try {
		packageInstaller.abandonSession(sessionId)
	} catch (_: Throwable) {
	}

	private fun onCancellation() = try {
		if (usePackageInstallerApi) {
			packageInstaller.unregisterSessionCallback(packageInstallerSessionObserver)
		}
		currentApkSources.forEach { it.clearTempFiles() }
		notificationManager.cancel(notificationId)
	} catch (_: ApplicationContextNotSetException) {
	} finally {
		CoroutineScope(installerContinuation.context + NonCancellable).launch {
			isInstallFinished.value = true
		}
		currentApkSources = emptyArray()
		activityFirstCreated = true
		hasActiveSession = false
	}

	private fun finishInstallation(result: InstallResult) {
		onCancellation()
		if (installerContinuation.isActive) {
			installerContinuation.resume(result)
		}
	}

	private fun displayNotification(appLabel: CharSequence?, sessionId: Int? = null, apkUri: Uri? = null) {
		val activityIntent = Intent(
			applicationContext,
			InstallLauncherActivity::class.java
		).apply {
			if (usePackageInstallerApi && sessionId != null) {
				putExtra(AndroidPackageInstaller.EXTRA_SESSION_ID, sessionId)
			}
			apkUri?.let { putExtra(APK_URI_KEY, it) }
		}
		val fullScreenIntent = PendingIntent.getActivity(
			applicationContext,
			REQUEST_CODE,
			activityIntent,
			pendingIntentCancelCurrentFlags
		)
		val message = if (appLabel != null) {
			applicationContext.getString(R.string.ssi_prompt_install_message_with_label, appLabel)
		} else {
			applicationContext.getString(R.string.ssi_prompt_install_message)
		}
		showNotification(
			fullScreenIntent,
			++notificationId,
			R.string.ssi_prompt_install_title,
			message
		)
	}

	private fun getApplicationLabel(apkFile: File) = apkFile.run {
		val packageInfo = packageManager.getPackageArchiveInfo(absolutePath, PackageManager.GET_META_DATA)
		packageInfo
			?.applicationInfo
			?.loadLabel(packageManager)
			?.takeIf { it != packageInfo.packageName }
	}

	internal class InstallLauncherActivity : AppCompatActivity() {

		private val actionInstallPackageLauncher = registerForActivityResult(actionInstallPackageContract) {
			val result = if (it) InstallResult.Success else InstallResult.Failure()
			finishInstallation(result)
		}

		@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
		private val confirmationIntentLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
				intent.extras?.getInt(AndroidPackageInstaller.EXTRA_SESSION_ID)?.let main@{ sessionId ->
					packageInstaller.getSessionInfo(sessionId)?.let { sessionInfo ->
						// Hacky workaround: progress not going higher than 0.8 means install failed.
						// This is needed to resume the coroutine with failure on reasons which are not
						// handled in onPackageInstallerStatusChanged. For example, "There was a problem
						// parsing the package" error falls under that.
						if (sessionInfo.progress < 0.81) {
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
			isInstallFinished
				.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
				.filter { it }
				.onEach {
					isInstallFinished.value = false
					finish()
				}
				.launchIn(lifecycleScope)
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
				applicationContext,
				InstallationEventsReceiver::class.java
			).apply {
				action = actionInstallationStatus
			}
			val receiverPendingIntent = PendingIntent.getBroadcast(
				applicationContext,
				REQUEST_CODE,
				receiverIntent,
				pendingIntentUpdateCurrentFlags
			)
			val statusReceiver = receiverPendingIntent.intentSender
			val sessionId = intent.extras?.getInt(AndroidPackageInstaller.EXTRA_SESSION_ID)
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