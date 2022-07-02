val publishGroupId: String by rootProject.extra
val publishVersion: String by rootProject.extra
val publishArtifactId = "simpleinstaller"

plugins {
	id("com.android.library")
	kotlin("android")
	`maven-publish`
	signing
}

android {
	compileSdk = 32
	buildToolsVersion = "33.0.0"

	defaultConfig {
		minSdk = 16
		targetSdk = 32
		version = publishVersion
		consumerProguardFiles("consumer-rules.pro")
	}

	sourceSets {
		named("main").get().java.srcDirs("src/main/kotlin")
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
	api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.3")
	implementation("androidx.appcompat:appcompat:1.4.2")
	implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.0")
	implementation("androidx.startup:startup-runtime:1.1.1")
	implementation("com.squareup.okio:okio:3.2.0")
}

tasks {
	val androidSourcesJar by creating(Jar::class) {
		archiveClassifier.set("sources")
		from(android.sourceSets.getByName("main").java.srcDirs)
	}

	artifacts {
		archives(androidSourcesJar)
	}

	afterEvaluate {
		publishing {
			publications {
				create<MavenPublication>("release") {
					groupId = publishGroupId
					artifactId = publishArtifactId
					version = publishVersion
					from(components.getByName("release"))
					artifact(androidSourcesJar)

					pom {
						name.set(publishArtifactId)
						description.set("Easy to use Android package installer wrapper leveraging Kotlin coroutines (API 16+)")
						url.set("https://github.com/solrudev/SimpleInstaller")

						licenses {
							license {
								name.set("SimpleInstaller License")
								url.set("https://github.com/solrudev/SimpleInstaller/blob/master/LICENSE")
							}
						}

						developers {
							developer {
								id.set("solrudev")
								name.set("Ilya Fomichev")
							}
						}

						scm {
							connection.set("scm:git:github.com/solrudev/SimpleInstaller.git")
							developerConnection.set("scm:git:ssh://github.com/solrudev/SimpleInstaller.git")
							url.set("https://github.com/solrudev/SimpleInstaller/tree/master")
						}
					}
				}
			}
		}
	}

	signing {
		val keyId = rootProject.extra["signing.keyId"] as String
		val key = rootProject.extra["signing.key"] as String
		val password = rootProject.extra["signing.password"] as String
		useInMemoryPgpKeys(keyId, key, password)
		sign(publishing.publications)
	}
}