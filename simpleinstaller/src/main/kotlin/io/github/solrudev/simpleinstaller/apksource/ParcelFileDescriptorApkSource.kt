package io.github.solrudev.simpleinstaller.apksource

import android.os.ParcelFileDescriptor

class ParcelFileDescriptorApkSource(private val apkParcelFileDescriptor: ParcelFileDescriptor) : ApkSource() {
	override val length get() = apkParcelFileDescriptor.statSize
	override fun openInputStream() = ParcelFileDescriptor.AutoCloseInputStream(apkParcelFileDescriptor)
	override suspend fun getUri() = createTempCopy()
}