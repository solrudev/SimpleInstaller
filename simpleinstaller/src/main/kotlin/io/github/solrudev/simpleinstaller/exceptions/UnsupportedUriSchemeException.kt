package io.github.solrudev.simpleinstaller.exceptions

import android.net.Uri

class UnsupportedUriSchemeException(uri: Uri) : Exception() {
	override val message: String = "Scheme of the provided URI is not supported: $uri"
}