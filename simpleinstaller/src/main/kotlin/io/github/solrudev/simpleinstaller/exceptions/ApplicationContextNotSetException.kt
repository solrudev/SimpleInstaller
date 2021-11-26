package io.github.solrudev.simpleinstaller.exceptions

class ApplicationContextNotSetException : Exception() {
	override val message: String =
		"Application context for SimpleInstaller was not set. Perhaps you forgot to add \'SimpleInstaller.initialize(this)\' to your Application class\' onCreate() method?"
}