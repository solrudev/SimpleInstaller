package io.github.solrudev.simpleinstaller.apksource.utils

import android.content.res.AssetFileDescriptor
import android.net.Uri
import io.github.solrudev.simpleinstaller.apksource.AssetFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import java.io.File

fun <T : Uri> Array<T>.toApkSourceArray() = map { UriApkSource(it) }.toTypedArray()
fun <T : File> Array<T>.toApkSourceArray() = map { FileApkSource(it) }.toTypedArray()
fun <T : AssetFileDescriptor> Array<T>.toApkSourceArray() = map { AssetFileDescriptorApkSource(it) }.toTypedArray()