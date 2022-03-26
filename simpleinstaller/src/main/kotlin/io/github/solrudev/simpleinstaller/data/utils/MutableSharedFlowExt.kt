package io.github.solrudev.simpleinstaller.data.utils

import io.github.solrudev.simpleinstaller.data.ProgressData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

@JvmSynthetic
internal fun MutableSharedFlow<ProgressData>.tryEmit(currentProgress: Int, progressMax: Int) =
	tryEmit(ProgressData(currentProgress, progressMax))

@JvmSynthetic
internal suspend inline fun MutableSharedFlow<ProgressData>.makeIndeterminate() {
	delay(15)
	emit(ProgressData(isIndeterminate = true))
}