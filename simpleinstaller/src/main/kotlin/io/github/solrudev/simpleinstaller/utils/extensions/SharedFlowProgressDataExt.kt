package io.github.solrudev.simpleinstaller.utils.extensions

import io.github.solrudev.simpleinstaller.data.ProgressData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

internal suspend inline fun MutableSharedFlow<ProgressData>.emit(
	currentProgress: Int,
	progressMax: Int
) = emit(ProgressData(currentProgress, progressMax))

internal fun MutableSharedFlow<ProgressData>.tryEmit(currentProgress: Int, progressMax: Int) =
	tryEmit(ProgressData(currentProgress, progressMax))

internal suspend inline fun MutableSharedFlow<ProgressData>.reset() {
	delay(15)
	emit(ProgressData())
}

internal suspend inline fun MutableSharedFlow<ProgressData>.makeIndeterminate() {
	delay(15)
	emit(ProgressData(isIndeterminate = true))
}