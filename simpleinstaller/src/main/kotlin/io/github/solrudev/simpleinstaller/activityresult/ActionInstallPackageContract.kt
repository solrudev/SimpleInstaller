package io.github.solrudev.simpleinstaller.activityresult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import io.github.solrudev.simpleinstaller.SimpleInstaller.installerPackageName

internal class ActionInstallPackageContract : ActivityResultContract<Uri, Boolean>() {

	@Suppress("DEPRECATION")
	override fun createIntent(context: Context, input: Uri) = Intent().apply {
		action = Intent.ACTION_INSTALL_PACKAGE
		data = input
		flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
		putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, false)
		putExtra(Intent.EXTRA_RETURN_RESULT, true)
		putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, installerPackageName)
	}

	override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == Activity.RESULT_OK
}