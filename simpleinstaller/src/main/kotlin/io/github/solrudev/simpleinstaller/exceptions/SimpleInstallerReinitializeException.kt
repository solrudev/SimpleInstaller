package io.github.solrudev.simpleinstaller.exceptions

class SimpleInstallerReinitializeException : Exception() {
	override val message: String = "Attempt of SimpleInstaller re-initialization."
}