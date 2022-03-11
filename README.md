# SimpleInstaller
[![Build Status](https://github.com/solrudev/SimpleInstaller/workflows/Publish/badge.svg)](https://github.com/solrudev/SimpleInstaller/actions?query=workflow%3A%22Publish%22)
![Maven Central](https://img.shields.io/maven-central/v/io.github.solrudev/simpleinstaller.svg)

## Contents
* [Overview](#overview)
    + [Gradle](#gradle)
* [Usage](#usage)
    + [Installation](#installation)
        - [Kotlin](#kotlin)
        - [Java](#java)
        - [Install permission](#install-permission)
        - [ApkSource](#apksource)
    + [Uninstallation](#uninstallation)
        - [Kotlin](#kotlin-1)
        - [Java](#java-1)
* [Sample app](#sample-app)
* [License](#license)

## Overview
SimpleInstaller is an Android library which provides easy to use abstraction over Android packages install and uninstall functionality leveraging Kotlin coroutines.

It supports Android versions starting from 4.1 Jelly Bean. Package installer provides a Kotlin SharedFlow property which can be collected for installation progress updates. Split packages installation is also supported (note that this is only available on Android versions starting from 5.0 Lollipop).

SimpleInstaller was developed with deferred execution in mind. You can launch an install or uninstall session when user is not interacting with your app directly, for example, while foreground service is running and your application was removed from recents. The way it works is that the user is shown a high-priority notification which launches a standard Android confirmation by clicking on it, or full-screen intent with confirmation (depends on firmware).

### Gradle
All versions are available [here](https://s01.oss.sonatype.org/#nexus-search;gav~io.github.solrudev~simpleinstaller~~~).
```kotlin
implementation("io.github.solrudev:simpleinstaller:x.y.z")
```
Replace "x.y.z" with the latest version.

## Usage
First, you need to initialize SimpleInstaller. To do this, add the following line to your Application class' `onCreate()` method (valid for both Kotlin and Java):
```kotlin
SimpleInstaller.initialize(this)
```
Optionally, you can provide an icon for SimpleInstaller notifications:
```kotlin
SimpleInstaller.initialize(this, R.drawable.your_notification_icon)
```

### Installation
Installation functionality is provided by `PackageInstaller` singleton class (`object` in Kotlin).

There are two separate methods for monolithic and split packages.

#### Kotlin
```kotlin
suspend fun installPackage(apkFile: TYPE): InstallResult
```
```kotlin
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun installSplitPackage(vararg apkFiles: TYPE): InstallResult
```
These methods return an `InstallResult` object, which can be either `Success` or `Failure`. `Failure` object may contain a cause of failure in its `cause` property.

You can get if `PackageInstaller` has an active session through a property:
```kotlin
var hasActiveSession: Boolean
```

#### Java
```java
static void installPackage(ApkSource apkFile, PackageInstallerCallback callback)
```
```java
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
static void installSplitPackage(ApkSource[] apkFiles, PackageInstallerCallback callback)
```
`PackageInstallerCallback` has the following interface:
```java
interface PackageInstallerCallback {
    void onSuccess();
    void onFailure(@Nullable InstallFailureCause cause);
    void onException(@NonNull Throwable exception);
    void onCanceled();
    void onProgressChanged(@NonNull ProgressData progress);
}
```
Java variants accept only [`ApkSource`](#ApkSource). If you need to pass [other types supported by SimpleInstaller out-of-the-box](#ApkSource), use methods from `PackageInstallerHelper`.
```java
static void installPackage(TYPE apkFile, PackageInstallerCallback callback)
```
```java
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
static void installSplitPackage(TYPE[] apkFiles, PackageInstallerCallback callback)
```
You can get if `PackageInstaller` has an active session through a getter method:
```java
static boolean getHasActiveSession()
```
Also it's possible to cancel current install session:
```java
static void cancel()
```

#### Install permission
On Oreo and higher `PackageInstaller` sets an install reason `PackageManager.INSTALL_REASON_USER`, so on first install there should be a prompt from Android to allow installation. But relying on this is not recommended, because your app will be restarted if user chooses Always allow, so the result and progress won't be received anymore (this is the case for MIUI). There's `InstallPermissionContract` in `activityresult` package which you should use to request user to turn on install from unknown sources for your app.

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

#### ApkSource
SimpleInstaller provides an abstract `ApkSource` class with the following public interface:
```kotlin
val progress: SharedFlow<ProgressData>
abstract val length: Long
abstract fun openInputStream(): InputStream?
abstract suspend fun getUri(): Uri
open fun clearTempFiles()
```
SimpleInstaller has out-of-the-box implementations for `Uri` (URIs must have `file:` or `content:` scheme), `AssetFileDescriptor` and `File`. You can provide your own implementation and pass it to `installPackage()` or `installSplitPackage()`.

### Uninstallation
Uninstallation functionality is provided by `PackageUninstaller` singleton class (`object` in Kotlin).

#### Kotlin
```kotlin
suspend fun uninstallPackage(packageName: String): Boolean
```
Returns true if uninstall succeeded, false otherwise.

You can get if `PackageUninstaller` has an active session through a property:
```kotlin
var hasActiveSession: Boolean
```

#### Java
```java
static void uninstallPackage(String packageName, PackageUninstallerCallback callback)
```
`PackageUninstallerCallback` has the following interface:
```java
interface PackageUninstallerCallback {
    void onFinished(boolean success);
    void onException(@NonNull Throwable exception);
    void onCanceled();
}
```
You can get if `PackageUninstaller` has an active session through a getter method:
```java
static boolean getHasActiveSession()
```

## Sample app
There's a simple sample app available. It can install chosen APK file and uninstall an application selected from the installed apps list. Go [here](https://github.com/solrudev/SimpleInstaller/tree/master/sampleapp) to see sources.

## License
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://github.com/solrudev/SimpleInstaller/blob/master/LICENSE)