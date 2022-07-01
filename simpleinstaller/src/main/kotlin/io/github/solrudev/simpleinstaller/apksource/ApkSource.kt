package io.github.solrudev.simpleinstaller.apksource

import android.net.Uri
import io.github.solrudev.simpleinstaller.PackageInstaller
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.utils.copy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.InputStream

private const val TEMP_APK_FILE_NAME = "temp.apk"

/**
 * An abstraction of an APK file source. Can be subclassed and fed to [PackageInstaller.installPackage]
 * or [PackageInstaller.installSplitPackage].
 */
abstract class ApkSource {

	/**
	 * A [MutableSharedFlow] of [ProgressData].
	 */
	private val _progress = MutableSharedFlow<ProgressData>(
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST
	)

	/**
	 * A [SharedFlow] of [ProgressData] which represents progress for possible operations on the APK file source.
	 * For example, an APK input stream must be copied to a temporary cache file to install on Android versions
	 * lower than 5.0 (Lollipop) when the APK source is not a [File] or file: [Uri].
	 *
	 * It is re-emitted to [PackageInstaller.progress] when [getUri] is called during installation.
	 */
	val progress = _progress.asSharedFlow()

	/**
	 * Length of the APK file.
	 */
	abstract val length: Long

	/**
	 * File which will be installed on API level < 21.
	 */
	open val file: File
		// By default a temp copy is created
		get() = tempApk

	/**
	 * Default temp APK file.
	 */
	private val tempApk: File
		get() = File(applicationContext.externalCacheDir, TEMP_APK_FILE_NAME)

	/**
	 * Opens an [InputStream] for the provided APK source.
	 */
	abstract fun openInputStream(): InputStream?

	/**
	 * Returns a [Uri] of the APK file. Used for installation on Android versions lower than 5.0 (Lollipop).
	 */
	abstract suspend fun getUri(): Uri

	/**
	 * Clears created temporary files.
	 */
	open fun clearTempFiles() {
		if (tempApk.exists()) tempApk.delete()
	}

	/**
	 * Emits progress for possible operations on the APK file source to private [MutableSharedFlow] of
	 * [ProgressData]. It is recommended to emit progress to it when you do any operations on the APK file
	 * in your implementation of [getUri].
	 */
	protected suspend fun progress(value: ProgressData) = _progress.emit(value)

	/**
	 * Creates a temporary cache copy of the APK file. Note that it will be rewritten if it already exists.
	 * Emits progress to [progress] SharedFlow.
	 *
	 * @return [Uri] of the copy.
	 */
	protected suspend fun createTempCopy(): Uri {
		if (tempApk.exists()) tempApk.delete()
		tempApk.createNewFile()
		val inputStream = openInputStream()
		val outputStream = tempApk.outputStream()
		copy(
			requireNotNull(inputStream) { "APK InputStream was null." },
			outputStream,
			length
		) { progressData -> progress(progressData) }
		return Uri.fromFile(tempApk)
	}
}