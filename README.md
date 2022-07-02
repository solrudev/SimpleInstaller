# SimpleInstaller
[![Build Status](https://github.com/solrudev/SimpleInstaller/workflows/Publish/badge.svg)](https://github.com/solrudev/SimpleInstaller/actions?query=workflow%3A%22Publish%22)
![Maven Central](https://img.shields.io/maven-central/v/io.github.solrudev/simpleinstaller.svg)

## Contents
* [Overview](#overview)
    + [Gradle](#gradle)
* [Usage](#usage)
    + [Installation](#installation)
        - [Install permission](#install-permission)
        - [ApkSource](#apksource)
    + [Uninstallation](#uninstallation)
* [Testing](#testing)
* [Sample app](#sample-app)
* [License](#license)

## Overview
SimpleInstaller is an Android library which provides easy to use abstraction over Android packages
install and uninstall functionality leveraging Kotlin coroutines.

It supports Android versions starting from 4.1 Jelly Bean. Package installer provides a Kotlin
SharedFlow property which can be collected for installation progress updates. Split packages
installation is also supported (note that this is only available on Android versions starting from
5.0 Lollipop).

SimpleInstaller was developed with deferred execution in mind. You can launch an install or
uninstall session when user is not interacting with your app directly, for example, while foreground
service is running and your application was removed from recents. The way it works is that the user
is shown a high-priority notification which launches a standard Android confirmation by clicking on
it, or full-screen intent with confirmation (depends on firmware).

### Gradle
All versions are available
[here](https://s01.oss.sonatype.org/#nexus-search;gav~io.github.solrudev~simpleinstaller~~~).
```kotlin
implementation("io.github.solrudev:simpleinstaller:x.y.z")
```
Replace "x.y.z" with the latest version.

## Usage
As an option, you can provide an icon for SimpleInstaller notifications:
```kotlin
SimpleInstaller.setNotificationIcon(R.drawable.your_notification_icon)
```
`android.R.drawable.ic_dialog_alert` is used by default.

### Installation
Installation functionality is provided by `PackageInstaller` interface.

There are two separate methods for monolithic and split packages.

<details open>
  <summary>Kotlin</summary>

```kotlin
suspend fun installPackage(apkFile: TYPE): InstallResult
```
```kotlin
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
suspend fun installSplitPackage(vararg apkFiles: TYPE): InstallResult
```
These methods return an `InstallResult` object, which can be either `Success` or `Failure`.
`Failure` object may contain a cause of failure in its `cause` property.

To use `PackageInstaller` from Kotlin one can just treat it as a singleton `object` because its
companion object implements `PackageInstaller` interface. For example:
```kotlin
val result = PackageInstaller.installPackage(apk)
val packageInstallerInstance = PackageInstaller
```
You can get if `PackageInstaller` has an active session through a property:
```kotlin
val hasActiveSession: Boolean
```
</details>

<details>
  <summary>Java</summary>

```java
void installPackage(ApkSource apkFile, PackageInstaller.Callback callback)
```
```java
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
void installSplitPackage(ApkSource[] apkFiles, PackageInstaller.Callback callback)
```
`PackageInstaller.Callback` has the following interface:
```java
interface Callback {
    void onSuccess();
    void onFailure(@Nullable InstallFailureCause cause);
    void onException(@NonNull Throwable exception);
    void onCanceled();
    void onProgressChanged(@NonNull ProgressData progress);
}
```
Java variants accept only [`ApkSource`](#ApkSource). If you need to pass [other types supported
by SimpleInstaller out-of-the-box](#ApkSource), use helper methods from `PackageInstallerHelper`.
You must pass an instance of `PackageInstaller` as the first parameter.
```java
static void installPackage(PackageInstaller packageInstaller, TYPE apkFile, PackageInstaller.Callback callback)
```
```java
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
static void installSplitPackage(PackageInstaller packageInstaller, TYPE[] apkFiles, PackageInstaller.Callback callback)
```
To obtain an instance of `PackageInstaller` use static `getInstance()` method:
```java
PackageInstaller packageInstaller = PackageInstaller.getInstance();
```
You can get if `PackageInstaller` has an active session through a getter method:
```java
boolean getHasActiveSession()
```
Also it's possible to cancel current install session:
```java
void cancel()
```
</details>

#### Install permission
On Oreo and higher `PackageInstaller` sets an install reason `PackageManager.INSTALL_REASON_USER`,
so on first install there should be a prompt from Android to allow installation. But relying on this
is not recommended, because your app will be restarted if user chooses Always allow, so the result
and progress won't be received anymore (this is the case for MIUI). There's
`InstallPermissionContract` in `activityresult` package which you should use to request user to turn
on install from unknown sources for your app.

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
SimpleInstaller has out-of-the-box implementations for `Uri` (URIs must have `file:` or `content:`
scheme), `AssetFileDescriptor` and `File`. You can provide your own implementation and pass it to
`installPackage()` or `installSplitPackage()`.

### Uninstallation
Uninstallation functionality is provided by `PackageUninstaller` interface.

<details open>
  <summary>Kotlin</summary>

```kotlin
suspend fun uninstallPackage(packageName: String): Boolean
```
Returns true if uninstall succeeded, false otherwise.

To use `PackageUninstaller` from Kotlin one can just treat it as a singleton `object` because its
companion object implements `PackageUninstaller` interface. For example:
```kotlin
val result = PackageUninstaller.uninstallPackage(packageName)
val packageUninstallerInstance = PackageUninstaller
```
You can get if `PackageUninstaller` has an active session through a property:
```kotlin
val hasActiveSession: Boolean
```
</details>

<details>
  <summary>Java</summary>

```java
void uninstallPackage(String packageName, PackageUninstaller.Callback callback)
```
`PackageUninstaller.Callback` has the following interface:
```java
interface Callback {
    void onFinished(boolean success);
    void onException(@NonNull Throwable exception);
    void onCanceled();
}
```
To obtain an instance of `PackageUninstaller` use static `getInstance()` method:
```java
PackageUninstaller packageUninstaller = PackageUninstaller.getInstance();
```
You can get if `PackageUninstaller` has an active session through a getter method:
```java
boolean getHasActiveSession()
```
Also it's possible to cancel current uninstall session:
```java
void cancel()
```
</details>

## Testing
`PackageInstaller` and `PackageUninstaller` are interfaces, so you can provide your own fake
implementation for tests. For example, you could create an implementation of `PackageInstaller`
which will always return `InstallResult.Failure` with `InstallFailureCause.Storage` cause:
```kotlin
class FailingPackageInstaller : PackageInstaller {

	override val hasActiveSession = false
	override val progress = MutableSharedFlow<ProgressData>()

	private val result = InstallResult.Failure(
		InstallFailureCause.Storage("Insufficient storage.")
	)

	override suspend fun installSplitPackage(vararg apkFiles: ApkSource) = result

	override fun installSplitPackage(vararg apkFiles: ApkSource, callback: PackageInstaller.Callback) {
		callback.onFailure(result.cause)
	}

	override suspend fun installPackage(apkFile: ApkSource) = result

	override fun installPackage(apkFile: ApkSource, callback: PackageInstaller.Callback) {
		callback.onFailure(result.cause)
	}

	override fun cancel() {}
}
```

## Sample app
There's a simple sample app available. It can install chosen APK file and uninstall an application
selected from the installed apps list. Go
[here](https://github.com/solrudev/SimpleInstaller/tree/master/sampleapp) to see sources.

## License
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://github.com/solrudev/SimpleInstaller/blob/master/LICENSE)
