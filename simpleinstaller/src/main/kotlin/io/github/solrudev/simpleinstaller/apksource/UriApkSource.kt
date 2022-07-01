package io.github.solrudev.simpleinstaller.apksource

import android.content.ContentResolver
import android.net.Uri
import io.github.solrudev.simpleinstaller.SimpleInstaller.applicationContext
import io.github.solrudev.simpleinstaller.exceptions.UnsupportedUriSchemeException
import io.github.solrudev.simpleinstaller.utils.extensions.length
import java.io.File

class UriApkSource(private val apkUri: Uri) : ApkSource() {

	override val length get() = apkUri.length

	override val file: File
		get() = when (apkUri.scheme) {
			ContentResolver.SCHEME_FILE -> File(apkUri.path ?: "")
			else -> super.file
		}

	override fun openInputStream() = applicationContext.contentResolver.openInputStream(apkUri)

	override suspend fun getUri() = when (apkUri.scheme) {
		ContentResolver.SCHEME_CONTENT -> createTempCopy()
		ContentResolver.SCHEME_FILE -> apkUri
		else -> throw UnsupportedUriSchemeException(apkUri)
	}
}