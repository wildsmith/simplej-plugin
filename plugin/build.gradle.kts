plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler.plugin)
    alias(libs.plugins.kotlin.serialization)
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 27
        branch = 20
    }
}

dependencies {
    implementation(libs.compose.runtime)
    implementation(libs.kotlinx.serialization)
    intellijPlatform {
        pluginModule(implementation(project(":base")))
    }
}
