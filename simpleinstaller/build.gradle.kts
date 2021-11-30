val publishGroupId = "io.github.solrudev"
val publishArtifactId = "simpleinstaller"
val publishVersion = "1.0.0"

plugins {
	id("com.android.library")
	kotlin("android")
	`maven-publish`
	signing
}

android {
	compileSdk = 31

	defaultConfig {
		minSdk = 16
		targetSdk = 31
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
	val coroutinesVersion = "1.5.2"
	api("androidx.activity:activity-ktx:1.4.0")
	api("androidx.core:core-ktx:1.7.0")
	api("androidx.appcompat:appcompat:1.4.0")
	api("com.google.android.material:material:1.4.0")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
	api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
	implementation("com.squareup.okio:okio:3.0.0")
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
				create("release", MavenPublication::class) {
					groupId = publishGroupId
					artifactId = publishArtifactId
					version = publishVersion
					from(components.getByName("release"))
					artifact(androidSourcesJar)

					pom {
						name.set(publishArtifactId)
						description.set("Android packages un/install library")
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