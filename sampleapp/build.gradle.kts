plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("kapt")
}

android {
	compileSdk = 31

	defaultConfig {
		applicationId = "io.github.solrudev.simpleinstaller.sampleapp"
		minSdk = 16
		targetSdk = 31
		versionCode = 1
		versionName = "1.0"
	}

	buildTypes {
		named("release") {
			isMinifyEnabled = false
			setProguardFiles(
				listOf(
					getDefaultProguardFile("proguard-android-optimize.txt"),
					"proguard-rules.pro"
				)
			)
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}
	kotlinOptions {
		jvmTarget = "1.8"
	}
	buildFeatures {
		dataBinding = true
		viewBinding = true
	}
}

dependencies {
	kapt("androidx.databinding:databinding-compiler:7.0.3")
	implementation("androidx.core:core-ktx:1.7.0")
	implementation("androidx.appcompat:appcompat:1.4.0")
	implementation("com.google.android.material:material:1.4.0")
	implementation("androidx.constraintlayout:constraintlayout:2.1.2")
	implementation(project(":simpleinstaller"))
}