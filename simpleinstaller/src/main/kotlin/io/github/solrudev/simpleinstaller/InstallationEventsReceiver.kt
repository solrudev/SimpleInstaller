package io.github.solrudev.simpleinstaller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

internal class InstallationEventsReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (PackageInstaller.hasActiveSession) {
			PackageInstaller.onPackageInstallerStatusChanged(context, intent)
		}
	}
}