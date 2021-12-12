package io.github.solrudev.simpleinstaller.sampleapp.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.solrudev.simpleinstaller.sampleapp.AppData
import io.github.solrudev.simpleinstaller.sampleapp.databinding.UninstallActivityBinding
import io.github.solrudev.simpleinstaller.sampleapp.viewmodels.UninstallViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class UninstallActivity : AppCompatActivity() {

	private lateinit var binding: UninstallActivityBinding
	private val viewModel: UninstallViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = UninstallActivityBinding.inflate(layoutInflater)
		setContentView(binding.root)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		val adapter = AppsListAdapter { onAppClick(it) }
		binding.recyclerView.adapter = adapter

		if (savedInstanceState == null) {
			binding.appsLoadingSpinner.visibility = View.VISIBLE
		}

		lifecycleScope.launch {
			if (savedInstanceState == null) loadApps()
			viewModel.appsList
				.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
				.collect { adapter.submitList(it) }
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressed()
		return true
	}

	private fun CoroutineScope.loadApps() = launch {
		withContext(Dispatchers.Default) {
			val appsList = getInstalledAppsList()
			viewModel.setAppsList(appsList)
		}
		binding.appsLoadingSpinner.animate()
			.alpha(0f)
			.setListener(object : AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: Animator) {
					binding.appsLoadingSpinner.visibility = View.GONE
				}
			})
	}

	private fun getInstalledAppsList() = packageManager.getInstalledApplications(0)
		.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
		.map {
			val icon = packageManager.getApplicationIcon(it)
			val appName = packageManager.getApplicationLabel(it) as String
			val packageName = it.packageName
			AppData(Random.nextInt(), icon, appName, packageName)
		}
		.sortedBy { it.name }

	private fun onAppClick(appData: AppData) {
		if (!viewModel.isUninstalling) {
			viewModel.uninstallPackage(appData)
		}
	}
}