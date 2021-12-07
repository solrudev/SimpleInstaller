package io.github.solrudev.simpleinstaller.apksource.utils

import android.content.res.AssetFileDescriptor
import android.net.Uri
import io.github.solrudev.simpleinstaller.apksource.ApkSource
import io.github.solrudev.simpleinstaller.apksource.AssetFileDescriptorApkSource
import io.github.solrudev.simpleinstaller.apksource.FileApkSource
import io.github.solrudev.simpleinstaller.apksource.UriApkSource
import java.io.File

/**
 * Creates a new [ApkSource] array containing [Uri] objects wrapped as [UriApkSource].
 */
fun <T : Uri> Array<T>.toApkSourceArray(): Array<ApkSource> = Array(size) { UriApkSource(get(it)) }

/**
 * Creates a new [ApkSource] array containing [File] objects wrapped as [FileApkSource].
 */
fun <T : File> Array<T>.toApkSourceArray(): Array<ApkSource> = Array(size) { FileApkSource(get(it)) }

/**
 * Creates a new [ApkSource] array containing [AssetFileDescriptor] objects wrapped as [AssetFileDescriptorApkSource].
 */
fun <T : AssetFileDescriptor> Array<T>.toApkSourceArray(): Array<ApkSource> =
	Array(size) { AssetFileDescriptorApkSource(get(it)) }