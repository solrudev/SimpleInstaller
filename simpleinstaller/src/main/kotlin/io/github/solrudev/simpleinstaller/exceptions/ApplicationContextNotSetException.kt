package io.github.solrudev.simpleinstaller.exceptions

class ApplicationContextNotSetException : Exception() {
	override val message = "SimpleInstaller was not properly initialized. " +
			"Check that SimpleInstallerInitializer is not disabled in your manifest file."
}