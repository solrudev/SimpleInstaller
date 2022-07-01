package io.github.solrudev.simpleinstaller

import android.content.Context
import androidx.startup.Initializer

internal class SimpleInstallerInitializer : Initializer<SimpleInstaller> {

	override fun create(context: Context): SimpleInstaller {
		SimpleInstaller.initializeInternal(context)
		return SimpleInstaller
	}

	override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}