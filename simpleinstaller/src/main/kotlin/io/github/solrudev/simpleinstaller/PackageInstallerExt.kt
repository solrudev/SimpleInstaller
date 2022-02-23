package io.github.solrudev.simpleinstaller

import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.PackageInstaller.installPackage
import io.github.solrudev.simpleinstaller.PackageInstaller.installSplitPackage
import io.github.solrudev.simpleinstaller.apksource.AssetFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import io.github.solrudev.simpleinstaller.apksource.utils.toApkSourceArray
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.extensions.isSupported
import java.io.File

/**
 * See [installSplitPackage].
 *
 * @param [apkFiles] [Uri]s of split APK files. Must be file: or content: URIs.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
suspend fun PackageInstaller.installSplitPackage(vararg apkFiles: Uri): InstallResult {
	apkFiles.forEach {
		if (!it.isSupported) {
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
suspend fun PackageInstaller.installSplitPackage(vararg apkFiles: AssetFileDescriptor) =
	installSplitPackage(*apkFiles.toApkSourceArray())

/**
 * See [installSplitPackage].
 *
 * @param [apkFiles] [File] objects representing split APK files.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
suspend fun PackageInstaller.installSplitPackage(vararg apkFiles: File) =
	installSplitPackage(*apkFiles.toApkSourceArray())

/**
 * See [installPackage].
 *
 * @param [apkFile] [Uri] of APK file. Must be a file: or content: URI.
 * @return [InstallResult]
 */
@JvmSynthetic
suspend fun PackageInstaller.installPackage(apkFile: Uri) = installPackage(UriApkSource(apkFile))

/**
 * See [installPackage].
 *
 * @param [apkFile] [AssetFileDescriptor] of APK file.
 * @return [InstallResult]
 */
@JvmSynthetic
suspend fun PackageInstaller.installPackage(apkFile: AssetFileDescriptor) =
	installPackage(AssetFileDescriptorApkSource(apkFile))

/**
 * See [installPackage].
 *
 * @param [apkFile] [File] object representing APK file.
 * @return [InstallResult]
 */
@JvmSynthetic
suspend fun PackageInstaller.installPackage(apkFile: File) = installPackage(FileApkSource(apkFile))