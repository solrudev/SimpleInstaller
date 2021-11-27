package io.github.solrudev.simpleinstaller.sampleapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import io.github.solrudev.simpleinstaller.activityresult.InstallPermissionContract
import io.github.solrudev.simpleinstaller.sampleapp.databinding.ActivityMainBinding
import io.github.solrudev.simpleinstaller.sampleapp.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {

	private lateinit var binding: ActivityMainBinding
	private val viewModel: MainViewModel by viewModels()

	@RequiresApi(Build.VERSION_CODES.O)
	private val requestInstallPermissionLauncher = registerForActivityResult(InstallPermissionContract()) {
		binding.installButton.isEnabled = it
	}

	private val pickApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
		if (it != null) viewModel.installPackage(it)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)
		binding.lifecycleOwner = this
		binding.activity = this
		binding.viewModel = viewModel
		if (savedInstanceState == null) requestInstallPermission()
	}

	fun onInstallButtonClick() {
		if (!viewModel.isInstalling) {
			try {
				pickApkLauncher.launch("application/vnd.android.package-archive")
			} catch (_: ActivityNotFoundException) {
			}
			return
		}
		viewModel.cancel()
	}

	fun onUninstallButtonClick() {
		val intent = Intent(this, UninstallActivity::class.java)
		startActivity(intent)
	}

	private fun requestInstallPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			binding.installButton.isEnabled = packageManager.canRequestPackageInstalls()
			if (!packageManager.canRequestPackageInstalls()) {
				requestInstallPermissionLauncher.launch(Unit)
			}
		}
	}
}