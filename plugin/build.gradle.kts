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
        instruction = 20
        branch = 16
    }
}

dependencies {
    implementation(libs.compose.desktop.jvm.mac.os.arm)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.kotlinx.serialization)
    intellijPlatform {
        pluginModule(implementation(project(":base")))
    }
}
