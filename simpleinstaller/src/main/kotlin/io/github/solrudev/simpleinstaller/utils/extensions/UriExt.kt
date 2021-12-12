@file:JvmSynthetic

package io.github.solrudev.simpleinstaller.utils.extensions

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import io.github.solrudev.simpleinstaller.SimpleInstaller
import java.io.File
import java.io.FileNotFoundException

internal val Uri.length: Long
	get() {
		if (scheme == ContentResolver.SCHEME_FILE) {
			return path?.let { File(it).length() } ?: -1L
		}
		val contentResolver = SimpleInstaller.applicationContext.contentResolver
		try {
			contentResolver.openAssetFileDescriptor(this, "r")
		} catch (e: FileNotFoundException) {
			null
		}?.let {
			// AssetFileDescriptor doesn't implement Closable on old Android versions
			@Suppress("ConvertTryFinallyToUseCall")
			try {
				return@let it.length
			} finally {
				it.close()
			}
		}
		if (scheme != ContentResolver.SCHEME_CONTENT) {
			return -1L
		}
		return contentResolver.query(this, arrayOf(OpenableColumns.SIZE), null, null, null)
			?.use { cursor ->
				val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
				if (sizeIndex == -1) {
					return@use -1L
				}
				cursor.moveToFirst()
				return try {
					cursor.getLong(sizeIndex)
				} catch (_: Throwable) {
					-1L
				}
			} ?: -1L
	}