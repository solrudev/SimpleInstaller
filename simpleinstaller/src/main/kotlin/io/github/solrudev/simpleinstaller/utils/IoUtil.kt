package io.github.solrudev.simpleinstaller.utils

import io.github.solrudev.simpleinstaller.data.ProgressData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil

private const val BUFFER_LENGTH = 8192L

@JvmSynthetic
@Suppress("BlockingMethodInNonBlockingContext")
internal suspend fun copy(
	inputStream: InputStream,
	outputStream: OutputStream,
	totalSize: Long,
	progressOffsetBytes: Long = 0,
	onProgressChanged: suspend (ProgressData) -> Unit
) = withContext(Dispatchers.IO) {
	val progressRatio = calculateProgressRatio(totalSize, BUFFER_LENGTH)
	val progressOffset = progressOffsetBytes / BUFFER_LENGTH
	inputStream.source().buffer().use { source ->
		outputStream.sink().buffer().use { sink ->
			val progressMax = ceil(totalSize.toDouble() / (BUFFER_LENGTH * progressRatio)).toInt()
			var currentProgress = progressOffset
			Buffer().use { buffer ->
				while (source.read(buffer, BUFFER_LENGTH) > 0) {
					ensureActive()
					sink.write(buffer, buffer.size)
					currentProgress++
					if (currentProgress % progressRatio == 0L) {
						onProgressChanged(ProgressData((currentProgress / progressRatio).toInt(), progressMax))
					}
				}
				sink.flush()
			}
		}
	}
}