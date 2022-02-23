package io.github.solrudev.simpleinstaller.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import io.github.solrudev.simpleinstaller.R
import io.github.solrudev.simpleinstaller.SimpleInstaller

@get:JvmSynthetic
internal val notificationManager
	get() = SimpleInstaller.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

@get:JvmSynthetic
internal val pendingIntentUpdateCurrentFlags
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
	} else {
		PendingIntent.FLAG_UPDATE_CURRENT
	}

@get:JvmSynthetic
internal val pendingIntentCancelCurrentFlags
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
	} else {
		PendingIntent.FLAG_CANCEL_CURRENT
	}

@JvmSynthetic
internal fun showNotification(intent: PendingIntent, notificationId: Int, titleId: Int, messageId: Int) {
	val channelId = SimpleInstaller.applicationContext.getString(R.string.ssi_notification_channel_id)
	val title = SimpleInstaller.applicationContext.getString(titleId)
	val message = SimpleInstaller.applicationContext.getString(messageId)
	val notification = NotificationCompat.Builder(SimpleInstaller.applicationContext, channelId).apply {
		setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
		setContentTitle(title)
		setContentText(message)
		setContentIntent(intent)
		priority = NotificationCompat.PRIORITY_MAX
		setDefaults(NotificationCompat.DEFAULT_ALL)
		setSmallIcon(SimpleInstaller.notificationIconId)
		setOngoing(true)
		setFullScreenIntent(intent, true)
		setAutoCancel(true)
	}.build()
	notificationManager.notify(notificationId, notification)
}