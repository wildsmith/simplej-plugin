plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler.plugin)
    id("simplej.java.library")
    id("org.jetbrains.intellij.platform")
}

simpleJ.intellijPlugin = true

dependencies {
    implementation(libs.compose.runtime)
    intellijPlatform {
        pluginModule(implementation(project(":core")))
    }
}
