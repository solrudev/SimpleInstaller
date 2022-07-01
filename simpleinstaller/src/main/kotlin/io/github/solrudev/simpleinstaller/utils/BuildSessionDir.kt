package io.github.solrudev.simpleinstaller.utils

// Reference code:
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/pm/PackageInstallerService.java;l=950;drc=bfd98fb906398de6487ed484cb8abd0f615c7ea8

import android.annotation.SuppressLint
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

private const val INSTALL_APEX = 0x00020000
private const val ENV_ANDROID_DATA = "ANDROID_DATA"
private val DIR_ANDROID_DATA_PATH = System.getenv(ENV_ANDROID_DATA) ?: "/data"
private val DIR_ANDROID_DATA = File(DIR_ANDROID_DATA_PATH)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
@JvmSynthetic
internal fun PackageInstaller.SessionInfo.buildSessionDir(params: PackageInstaller.SessionParams): File {
	if (isStagedCompat || params.installFlags and INSTALL_APEX != 0) {
		val sessionStagingDir = getDataStagingDirectory(params.volumeUuid ?: "")
		return File(sessionStagingDir, "session_${sessionId}")
	}
	return buildTmpSessionDir(sessionId, params.volumeUuid ?: "")
}

private fun buildTmpSessionDir(sessionId: Int, volumeUuid: String): File {
	val sessionStagingDir = getTmpSessionDir(volumeUuid)
	return File(sessionStagingDir, "vmdl$sessionId.tmp")
}

private fun getTmpSessionDir(volumeUuid: String) = getDataAppDirectory(volumeUuid)
private fun getDataAppDirectory(volumeUuid: String) = File(getDataDirectory(volumeUuid), "app")
private fun getDataStagingDirectory(volumeUuid: String) = File(getDataDirectory(volumeUuid), "app-staging")

private fun getDataDirectory(volumeUuid: String) = if (volumeUuid.isEmpty()) {
	DIR_ANDROID_DATA
} else {
	File("/mnt/expand/$volumeUuid")
}

private val PackageInstaller.SessionInfo.isStagedCompat: Boolean
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		isStaged
	} else {
		false
	}

private val PackageInstaller.SessionParams.installFlags: Int
	@SuppressLint("DiscouragedPrivateApi")
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	get() = try {
		val field = PackageInstaller.SessionParams::class.java.getDeclaredField("installFlags")
		field.get(this) as Int
	} catch (_: Exception) {
		0
	}

private val PackageInstaller.SessionParams.volumeUuid: String?
	@SuppressLint("SoonBlockedPrivateApi")
	@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
	get() = try {
		val field = PackageInstaller.SessionParams::class.java.getDeclaredField("volumeUuid")
		field.get(this) as? String
	} catch (_: Exception) {
		null
	}