plugins {
    alias(libs.plugins.compose.compiler.plugin)
    id("simplej.android.library")
}

simpleJ.intellijPlugin = true

android {
    namespace = "com.simplej.preview"
    buildFeatures {
        compose = true
    }
    resourcePrefix = "sp"
}

dependencies {
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(project(":demo-modules:delete-me-with-references"))
    implementation(project(":plugin"))
}
