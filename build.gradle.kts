buildscript {
	repositories {
		google()
		mavenCentral()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:7.0.3")
		classpath(kotlin("gradle-plugin", "1.6.0"))
	}
}

tasks.register<Delete>("clean").configure {
	delete(rootProject.buildDir)
}