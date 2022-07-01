package io.github.solrudev.simpleinstaller

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.DrawableRes
import io.github.solrudev.simpleinstaller.exceptions.SimpleInstallerReinitializeException
import io.github.solrudev.simpleinstaller.utils.notificationManager
import io.github.solrudev.simpleinstaller.utils.requireContextNotNull

/**
 * Easy to use Android package installer wrapper leveraging Kotlin coroutines (API 16+).
 */
object SimpleInstaller {

	@get:JvmSynthetic
	internal val applicationContext: Context
		get() = requireContextNotNull(_applicationContext)

	@get:JvmSynthetic
	internal val installerPackageName: String
		get() = applicationContext.packageName

	@get:JvmSynthetic
	internal val packageManager: PackageManager
		get() = applicationContext.packageManager

	@DrawableRes
	@get:JvmSynthetic
	internal var notificationIconId = android.R.drawable.ic_dialog_alert
		private set

	private var _applicationContext: Context? = null

	private val configurationChangesCallback = object : ComponentCallbacks {
		override fun onConfigurationChanged(newConfig: Configuration) = createNotificationChannel()
		override fun onLowMemory() {}
	}

	private val lock = Any()

	/**
	 * Initializes `SimpleInstaller` with provided application context and, optionally, notifications icon.
	 *
	 * This should be called in your Application class' `onCreate()` method.
	 *
	 * Example: `SimpleInstaller.initialize(this)`
	 */
	@JvmStatic
	@Deprecated(message = "Manual initialization is deprecated. SimpleInstaller now uses App Startup library.")
	@Suppress("UNUSED_PARAMETER")
	fun initialize(applicationContext: Context) {
	}

	/**
	 * Initializes `SimpleInstaller` with provided application context and, optionally, notifications icon.
	 *
	 * This should be called in your Application class' `onCreate()` method.
	 *
	 * Example: `SimpleInstaller.initialize(this)`
	 */
	@JvmStatic
	@Deprecated(
		message = "Manual initialization is deprecated. SimpleInstaller now uses App Startup library.",
		replaceWith = ReplaceWith(expression = "SimpleInstaller.setNotificationIcon(notificationIconId)")
	)
	@Suppress("UNUSED_PARAMETER")
	fun initialize(
		applicationContext: Context,
		@DrawableRes notificationIconId: Int
	) {
		// For compatibility reasons
		this.notificationIconId = notificationIconId
	}

	/**
	 * Sets an icon for SimpleInstaller notifications. By default its value is equal to
	 * [android.R.drawable.ic_dialog_alert].
	 */
	@JvmStatic
	fun setNotificationIcon(@DrawableRes notificationIcon: Int) {
		notificationIconId = notificationIcon
	}

	@JvmSynthetic
	internal fun initializeInternal(context: Context) {
		synchronized(lock) {
			if (_applicationContext != null) {
				throw SimpleInstallerReinitializeException()
			}
			_applicationContext = context.applicationContext
			createNotificationChannel()
			val application = _applicationContext as? Application
			application?.registerComponentCallbacks(configurationChangesCallback)
		}
	}

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return
		}
		val channelIdString = applicationContext.getString(R.string.ssi_notification_channel_id)
		val channelName = applicationContext.getString(R.string.ssi_notification_channel_name)
		val channelDescription = applicationContext.getString(R.string.ssi_notification_channel_description)
		val importance = NotificationManager.IMPORTANCE_HIGH
		val channel = NotificationChannel(channelIdString, channelName, importance).apply {
			description = channelDescription
		}
		notificationManager.createNotificationChannel(channel)
	}
}