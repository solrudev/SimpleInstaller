package io.github.solrudev.simpleinstaller.utils

import kotlin.math.ceil

private const val EMIT_COUNT = 100

@JvmSynthetic
internal fun calculateProgressRatio(totalSize: Long, bufferLength: Long) =
	ceil(totalSize.toDouble() / (bufferLength.coerceAtLeast(1) * EMIT_COUNT)).toInt().coerceAtLeast(1)