package io.github.solrudev.simpleinstaller.apksource

import android.net.Uri
import java.io.File
import java.io.FileInputStream

class FileApkSource(private val apkFile: File) : ApkSource() {
	override val length get() = apkFile.length()
	override fun openInputStream() = FileInputStream(apkFile)
	override suspend fun getUri(): Uri = Uri.fromFile(apkFile)
}