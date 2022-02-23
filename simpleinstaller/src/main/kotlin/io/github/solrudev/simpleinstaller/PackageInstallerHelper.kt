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
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.extensions.isSupported
import java.io.File

/**
 * Helper methods for [PackageInstaller].
 */
object PackageInstallerHelper {

	/**
	 * See [PackageInstaller.installSplitPackage].
	 *
	 * @param [apkFiles] [Uri]s of split APK files. Must be file: or content: URIs.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	@JvmStatic
	fun installSplitPackage(vararg apkFiles: Uri, callback: PackageInstallerCallback) {
		apkFiles.forEach {
			if (!it.isSupported) {
				throw UnsupportedUriSchemeException(it)
			}
		}
		installSplitPackage(apkFiles = apkFiles.toApkSourceArray(), callback)
	}

	/**
	 * See [PackageInstaller.installSplitPackage].
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
	 * See [PackageInstaller.installSplitPackage].
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
	 * See [PackageInstaller.installPackage].
	 *
	 * @param [apkFile] [Uri] of APK file. Must be a file: or content: URI.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: Uri, callback: PackageInstallerCallback) =
		installPackage(UriApkSource(apkFile), callback)

	/**
	 * See [PackageInstaller.installPackage].
	 *
	 * @param [apkFile] [AssetFileDescriptor] of APK file.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: AssetFileDescriptor, callback: PackageInstallerCallback) =
		installPackage(AssetFileDescriptorApkSource(apkFile), callback)

	/**
	 * See [PackageInstaller.installPackage].
	 *
	 * @param [apkFile] [File] object representing APK file.
	 * @param [callback] A callback object implementing [PackageInstallerCallback] interface.
	 * Its methods are called on main thread.
	 */
	@JvmStatic
	fun installPackage(apkFile: File, callback: PackageInstallerCallback) =
		installPackage(FileApkSource(apkFile), callback)
}