val androidGradleVersion: String by rootProject.extra

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
	kapt("androidx.databinding:databinding-compiler:$androidGradleVersion")
	implementation("androidx.activity:activity-ktx:1.4.0")
	implementation("com.google.android.material:material:1.5.0")
	implementation(project(":simpleinstaller"))
}