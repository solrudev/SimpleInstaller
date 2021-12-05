package io.github.solrudev.simpleinstaller.apksource

import android.content.res.AssetFileDescriptor
import java.io.FileInputStream

class AssetFileDescriptorApkSource(private val apkAssetFileDescriptor: AssetFileDescriptor) : ApkSource() {
	override val length get() = apkAssetFileDescriptor.declaredLength
	override fun openInputStream(): FileInputStream? = apkAssetFileDescriptor.createInputStream()
	override suspend fun getUri() = createTempCopy()
}