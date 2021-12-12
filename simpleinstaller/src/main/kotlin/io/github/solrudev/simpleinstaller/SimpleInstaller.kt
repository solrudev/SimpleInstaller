package io.github.solrudev.simpleinstaller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import io.github.solrudev.simpleinstaller.exceptions.SimpleInstallerReinitializeException
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.requireContextNotNull

/**
 * Easy to use abstraction over Android packages install and uninstall functionality leveraging Kotlin coroutines.
 */
object SimpleInstaller {

	private var _applicationContext: Context? = null
	internal val applicationContext: Context get() = requireContextNotNull(_applicationContext)
	internal val packageName: String get() = applicationContext.packageName

	internal var notificationIconId = android.R.drawable.ic_dialog_alert
		private set

	/**
	 * Initializes `SimpleInstaller` with provided application context and, optionally, notifications icon.
	 *
	 * This should be called in your Application class' [android.app.Application.onCreate] method.
	 *
	 * Example: `SimpleInstaller.initialize(this)`
	 */
	@JvmStatic
	fun initialize(
		applicationContext: Context,
		@DrawableRes notificationIconId: Int = android.R.drawable.ic_dialog_alert
	) {
		if (_applicationContext != null) {
			throw SimpleInstallerReinitializeException()
		}
		_applicationContext = applicationContext.applicationContext
		this.notificationIconId = notificationIconId
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val channelIdString = this.applicationContext.getString(R.string.ssi_notification_channel_id)
		val channelName = this.applicationContext.getString(R.string.ssi_notification_channel_name)
		val channelDescription = this.applicationContext.getString(R.string.ssi_notification_channel_description)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannel(channelIdString, channelName, importance).apply {
			description = channelDescription
		}
		notificationManager.createNotificationChannel(channel)
	}
}