@file:JvmName("PackageInstallerHelper")

package io.github.solrudev.simpleinstaller

import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.apksource.AssetFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import io.github.solrudev.simpleinstaller.apksource.utils.toApkSourceArray
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.extensions.isSupported
import java.io.File

/**
 * Accepts an array of [Uri] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [Uri] objects representing split APK files. Must be file: or content: URIs.
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
 * Accepts an array of [Uri] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * When called from Java a [PackageInstaller] instance must be passed as the first parameter.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [Uri] objects representing split APK files. Must be file: or content: URIs.
 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
 * Its methods are called on main thread.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PackageInstaller.installSplitPackage(vararg apkFiles: Uri, callback: PackageInstaller.Callback) {
	apkFiles.forEach {
		if (!it.isSupported) {
			throw UnsupportedUriSchemeException(it)
		}
	}
	installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)
}

/**
 * Accepts an array of [AssetFileDescriptor] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [AssetFileDescriptor] objects representing split APK files.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
suspend fun PackageInstaller.installSplitPackage(vararg apkFiles: AssetFileDescriptor) =
	installSplitPackage(*apkFiles.toApkSourceArray())

/**
 * Accepts an array of [AssetFileDescriptor] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * When called from Java a [PackageInstaller] instance must be passed as the first parameter.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [AssetFileDescriptor] objects representing split APK files.
 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
 * Its methods are called on main thread.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PackageInstaller.installSplitPackage(vararg apkFiles: AssetFileDescriptor, callback: PackageInstaller.Callback) =
	installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)

/**
 * Accepts an array of [File] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [File] objects representing split APK files.
 * @return [InstallResult]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
suspend fun PackageInstaller.installSplitPackage(vararg apkFiles: File) =
	installSplitPackage(*apkFiles.toApkSourceArray())

/**
 * Accepts an array of [File] objects, converts it to an array of [ApkSource] objects and calls
 * [PackageInstaller]'s `installSplitPackage()`.
 *
 * When called from Java a [PackageInstaller] instance must be passed as the first parameter.
 *
 * @see PackageInstaller.installSplitPackage
 * @param apkFiles [File] objects representing split APK files.
 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
 * Its methods are called on main thread.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PackageInstaller.installSplitPackage(vararg apkFiles: File, callback: PackageInstaller.Callback) =
	installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)

/**
 * Accepts a [Uri], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [Uri] object representing APK file. Must be a file: or content: URI.
 * @return [InstallResult]
 */
@JvmSynthetic
suspend fun PackageInstaller.installPackage(apkFile: Uri) = installPackage(UriApkSource(apkFile))

/**
 * Accepts a [Uri], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * When called from Java a [PackageInstaller] instance must be passed as the first parameter.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [Uri] object representing APK file. Must be a file: or content: URI.
 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
 * Its methods are called on main thread.
 */
fun PackageInstaller.installPackage(apkFile: Uri, callback: PackageInstaller.Callback) =
	installPackage(UriApkSource(apkFile), callback)

/**
 * Accepts an [AssetFileDescriptor], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [AssetFileDescriptor] object representing APK file.
 * @return [InstallResult]
 */
@JvmSynthetic
suspend fun PackageInstaller.installPackage(apkFile: AssetFileDescriptor) =
	installPackage(AssetFileDescriptorApkSource(apkFile))

/**
 * Accepts an [AssetFileDescriptor], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * When called from Java a [PackageInstaller] instance must be passed as the first parameter.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [AssetFileDescriptor] object representing APK file.
 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
 * Its methods are called on main thread.
 */
fun PackageInstaller.installPackage(apkFile: AssetFileDescriptor, callback: PackageInstaller.Callback) =
	installPackage(AssetFileDescriptorApkSource(apkFile), callback)

/**
 * Accepts a [File], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [File] object representing APK file.
 * @return [InstallResult]
 */
@JvmSynthetic
suspend fun PackageInstaller.installPackage(apkFile: File) = installPackage(FileApkSource(apkFile))

/**
 * Accepts a [File], converts it to an [ApkSource] and calls [PackageInstaller]'s `installPackage()`.
 *
 * When called from Java a [PackageInstaller] instance must be passed as the first parameter.
 *
 * @see PackageInstaller.installPackage
 * @param apkFile [File] object representing APK file.
 * @param callback A callback object implementing [PackageInstaller.Callback] interface.
 * Its methods are called on main thread.
 */
fun PackageInstaller.installPackage(apkFile: File, callback: PackageInstaller.Callback) =
	installPackage(FileApkSource(apkFile), callback)