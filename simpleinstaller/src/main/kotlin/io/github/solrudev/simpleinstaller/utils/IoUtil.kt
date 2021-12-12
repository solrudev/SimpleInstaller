@file:JvmSynthetic

package io.github.solrudev.simpleinstaller.utils

import io.github.solrudev.simpleinstaller.data.ProgressData
import io.github.solrudev.simpleinstaller.data.utils.emit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil

private const val BUFFER_LENGTH: Long = 8192

internal suspend fun copy(
	inputStream: InputStream,
	outputStream: OutputStream,
	totalSize: Long,
	progress: MutableSharedFlow<ProgressData>,
	progressOffsetBytes: Long = 0
) = withContext(Dispatchers.IO) {
	val progressRatio = calculateProgressRatio(totalSize, BUFFER_LENGTH)
	val progressOffset = progressOffsetBytes / BUFFER_LENGTH
	inputStream.source().buffer().use { source ->
		outputStream.sink().buffer().use { sink ->
			val progressMax =
				ceil(totalSize.toDouble() / (BUFFER_LENGTH * progressRatio)).toInt()
			var currentProgress = progressOffset
			Buffer().use { buffer ->
				while (source.read(buffer, BUFFER_LENGTH) > 0) {
					ensureActive()
					sink.write(buffer, buffer.size)
					currentProgress++
					if (currentProgress % progressRatio == 0L) {
						progress.emit((currentProgress / progressRatio).toInt(), progressMax)
					}
				}
			}
		}
	}
}