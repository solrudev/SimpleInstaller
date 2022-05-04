package io.github.solrudev.simpleinstaller.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal class InstallationEventsReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		if (PackageInstallerImpl.hasActiveSession) {
			PackageInstallerImpl.onPackageInstallerStatusChanged(context, intent)
		}
	}
}