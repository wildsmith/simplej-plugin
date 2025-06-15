pluginManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "simplej-plugin"

include(
    ":core",
    ":host",
    ":playground"
)