package io.github.solrudev.simpleinstaller.sampleapp.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.solrudev.simpleinstaller.PackageInstaller
import io.github.solrudev.simpleinstaller.data.InstallResult
import io.github.solrudev.simpleinstaller.installPackage
import io.github.solrudev.simpleinstaller.sampleapp.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

	private var installJob: Job? = null
	private val _progress = MutableStateFlow(0)
	private val _progressMax = MutableStateFlow(100)
	private val _isProgressIndeterminate = MutableStateFlow(false)
	private val _text = MutableStateFlow(R.string.app_name)
	private val _isInstallEnabled = MutableStateFlow(false)
	private val _installButtonText = MutableStateFlow(R.string.install)
	private val _uninstallButtonText = MutableStateFlow(R.string.uninstall)

	val progress = _progress.asStateFlow()
	val progressMax = _progressMax.asStateFlow()
	val isProgressIndeterminate = _isProgressIndeterminate.asStateFlow()
	val text = _text.asStateFlow()
	val isInstallEnabled = _isInstallEnabled.asStateFlow()
	val installButtonText = _installButtonText.asStateFlow()
	val uninstallButtonText = _uninstallButtonText.asStateFlow()
	val isInstalling get() = PackageInstaller.hasActiveSession

	init {
		viewModelScope.launch {
			PackageInstaller.progress.collect {
				_progress.value = it.progress
				_progressMax.value = it.max
				_isProgressIndeterminate.value = it.isIndeterminate
			}
		}
	}

	fun enableInstallButton() {
		_isInstallEnabled.value = true
	}

	fun installPackage(uri: Uri) {
		_installButtonText.value = R.string.cancel
		_text.value = R.string.installing
		installJob = viewModelScope.launch {
			try {
				when (val result = PackageInstaller.installPackage(uri)) {
					is InstallResult.Success -> _text.value = R.string.installed_successfully
					is InstallResult.Failure -> {
						_text.value = R.string.install_failed
						result.cause?.message?.let { Log.i(this@MainViewModel::class.java.name, it) }
					}
				}
			} catch (e: Throwable) {
				_text.value = R.string.install_failed
				if (e !is CancellationException) {
					Log.e(this@MainViewModel::class.java.name, "", e)
				}
			} finally {
				_installButtonText.value = R.string.install
			}
		}
	}

	fun cancel() {
		installJob?.cancel()
		_text.value = R.string.install_canceled
	}
}