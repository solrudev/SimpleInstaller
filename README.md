# SimpleInstaller
[![Build Status](https://github.com/solrudev/SimpleInstaller/workflows/Publish/badge.svg)](https://github.com/solrudev/SimpleInstaller/actions?query=workflow%3A%22Publish%22)
![Maven Central](https://img.shields.io/maven-central/v/io.github.solrudev/simpleinstaller.svg)

## Overview
SimpleInstaller is an Android library which provides easy to use abstraction over Android packages install and uninstall functionality leveraging Kotlin coroutines.

It supports Android versions starting from 4.1 Jelly Bean. Package installer provides a Kotlin SharedFlow property which can be collected for installation progress updates. Split packages installation is also supported (note that this is only available on Android versions starting from 5.0 Lollipop).

SimpleInstaller was developed with background execution in mind. You can launch an install or uninstall session from background, for example, while foreground service is running and your application was removed from recents. The way it works is that the user is shown a high-priority notification which launches a standard Android confirmation by clicking on it, or full-screen intent with confirmation (depends on firmware).

## Gradle dependencies
All versions are available [here](https://s01.oss.sonatype.org/#nexus-search;gav~io.github.solrudev~simpleinstaller~~~).
```kotlin
implementation("io.github.solrudev:simpleinstaller:1.0.0")
```

## Usage
First, you need to initialize SimpleInstaller. To do this, add the following line to your Application class' `onCreate()` method:
```kotlin
SimpleInstaller.initialize(this)
```

### Installation
There are two methods for installation.
```kotlin
suspend fun PackageInstaller.installPackage(apkFile: Uri): InstallResult
```
```kotlin
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun PackageInstaller.installSplitPackage(vararg apkFiles: Uri): InstallResult
```
URIs must have `file:` or `content:` scheme. These methods return an `InstallResult` object, which can be either `Success` or `Failure`. `Failure` object may contain a cause of failure in its `cause` property.

### Uninstallation
```kotlin
suspend fun PackageUninstaller.uninstallPackage(packageName: String): Boolean
```
Returns true if uninstall succeeded, false otherwise.

### Install permission
On Oreo and higher `PackageInstaller` sets an install reason `PackageManager.INSTALL_REASON_USER`, so on first install there should be a prompt from Android to allow installation. There's also `InstallPermissionContract` in `activityresult` package which you can use to request user to turn on install from unknown sources for your app.

In your Activity:
```kotlin
val requestInstallPermissionLauncher = registerForActivityResult(InstallPermissionContract()) { booleanResult ->
    if (booleanResult) { /* do something */ }
}
```
...
```kotlin
requestInstallPermissionLauncher.launch(Unit)
```

## Sample app
There's a simple sample app available. It can install chosen APK file and uninstall an application selected from the installed apps list. Go [here](https://github.com/solrudev/SimpleInstaller/tree/master/sampleapp) to see sources.

## License
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://github.com/solrudev/SimpleInstaller/blob/master/LICENSE)