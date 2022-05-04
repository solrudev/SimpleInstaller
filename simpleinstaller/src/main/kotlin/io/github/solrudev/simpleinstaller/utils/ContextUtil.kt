package io.github.solrudev.simpleinstaller.utils

import android.content.Context
import io.github.solrudev.simpleinstaller.exceptions.ApplicationContextNotSetException

/**
 * Throws an [ApplicationContextNotSetException] if the [applicationContext] is null.
 * Otherwise returns the not null value.
 */
@JvmSynthetic
internal fun requireContextNotNull(applicationContext: Context?) =
	applicationContext ?: throw ApplicationContextNotSetException()