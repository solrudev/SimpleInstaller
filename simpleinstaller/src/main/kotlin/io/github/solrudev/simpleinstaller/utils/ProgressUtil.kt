@file:JvmSynthetic

package io.github.solrudev.simpleinstaller.utils

import kotlin.math.ceil

private const val EMIT_COUNT = 100

internal fun calculateProgressRatio(totalSize: Long, bufferLength: Long) =
	ceil(totalSize.toDouble() / (bufferLength * EMIT_COUNT)).toInt().coerceAtLeast(1)