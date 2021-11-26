plugins {
	id("com.android.library")
	kotlin("android")
}

android {
	compileSdk = 31

	defaultConfig {
		minSdk = 16
		targetSdk = 31
		version = "1.0.0"
		consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
	val coroutinesVersion = "1.5.2-native-mt"
	api("androidx.activity:activity-ktx:1.4.0")
	api("androidx.core:core-ktx:1.7.0")
	api("androidx.appcompat:appcompat:1.4.0")
	api("com.google.android.material:material:1.4.0")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
	implementation("com.squareup.okio:okio:2.10.0")
}