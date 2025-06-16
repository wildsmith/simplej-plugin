pluginManagement {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://plugins.gradle.org/m2/")
    }
}

rootProject.name = "simplej-plugin"

include(
    ":base",
    ":plugin",
    ":playground",
    ":delete-me-no-references"
)
