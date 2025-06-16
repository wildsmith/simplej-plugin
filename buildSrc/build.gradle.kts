plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

repositories {
    mavenCentral()
    google()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.intellij.platform.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("JavaLibraryConventionPlugin") {
            id = "simplej.java-library"
            implementationClass = "com.simplej.plugin.scripts.plugins.JavaLibraryConventionPlugin"
        }
        create("AndroidLibraryConventionPlugin") {
            id = "simplej.android.library"
            implementationClass = "com.simplej.plugin.scripts.plugins.AndroidLibraryConventionPlugin"
        }
    }
}

// Register these default tasks so that the IDE Plugin doesn't get tripped up
tasks.register("checkstyle")
tasks.register("detekt")
tasks.register("lint")
