package io.github.solrudev.simpleinstaller.activityresult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

internal class UninstallPackageContract : ActivityResultContract<String, Boolean>() {

	@Suppress("DEPRECATION")
	override fun createIntent(context: Context, input: String) = Intent().apply {
		action = Intent.ACTION_UNINSTALL_PACKAGE
		data = Uri.parse("package:$input")
		putExtra(Intent.EXTRA_RETURN_RESULT, true)
	}

	override fun parseResult(resultCode: Int, intent: Intent?) = resultCode == Activity.RESULT_OK
}