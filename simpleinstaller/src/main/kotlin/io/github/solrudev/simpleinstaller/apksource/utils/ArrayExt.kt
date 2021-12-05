package io.github.solrudev.simpleinstaller.apksource.utils

import android.net.Uri
import android.os.ParcelFileDescriptor
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.ParcelFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import java.io.File

fun <T : Uri> Array<T>.toApkSourceArray() = map { UriApkSource(it) }.toTypedArray()
fun <T : File> Array<T>.toApkSourceArray() = map { FileApkSource(it) }.toTypedArray()
fun <T : ParcelFileDescriptor> Array<T>.toApkSourceArray() = map { ParcelFileDescriptorApkSource(it) }.toTypedArray()