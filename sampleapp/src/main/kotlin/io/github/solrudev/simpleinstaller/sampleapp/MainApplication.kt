package io.github.solrudev.simpleinstaller.sampleapp

import android.app.Application
import io.github.solrudev.simpleinstaller.SimpleInstaller

class MainApplication : Application() {

	override fun onCreate() {
		super.onCreate()
		SimpleInstaller.initialize(this)
	}
}