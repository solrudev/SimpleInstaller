package io.github.solrudev.simpleinstaller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import io.github.solrudev.simpleinstaller.exceptions.SimpleInstallerReinitializeException
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.requireContextNotNull

object SimpleInstaller {

	private var _applicationContext: Context? = null

	internal val applicationContext: Context
		get() = requireContextNotNull(_applicationContext)

	internal var notificationIconId: Int = android.R.drawable.ic_dialog_alert
		private set

	internal val packageName: String get() = requireContextNotNull(_applicationContext).packageName

	/**
	 * Initializes `SimpleInstaller` with provided application context and, optionally, notifications icon.
	 *
	 * This should be called in your Application class' `onCreate()` method.
	 *
	 * Example: `SimpleInstaller.initialize(this)`
	 */
	fun initialize(
		applicationContext: Context,
		@DrawableRes notificationIconId: Int = android.R.drawable.ic_dialog_alert
	) {
		if (_applicationContext != null) {
			throw SimpleInstallerReinitializeException()
		}
		_applicationContext = applicationContext
		SimpleInstaller.notificationIconId = notificationIconId
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val channelIdString = SimpleInstaller.applicationContext.getString(R.string.ssi_notification_channel_id)
		val channelName = SimpleInstaller.applicationContext.getString(R.string.ssi_notification_channel_name)
		val channelDescription =
			SimpleInstaller.applicationContext.getString(R.string.ssi_notification_channel_description)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannel(channelIdString, channelName, importance).apply {
			description = channelDescription
		}
		notificationManager.createNotificationChannel(channel)
	}
}