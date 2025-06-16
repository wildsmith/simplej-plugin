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
    ":demo-modules:delete-me-no-references",
    ":demo-modules:delete-me-with-references",
    ":playground",
    ":plugin",
)
