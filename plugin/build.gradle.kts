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
        instruction = 15
        branch = 14
    }
}

dependencies {
    implementation(libs.compose.desktop.jvm.mac.os.arm) {
        // Excluding the transitive coroutine dependency as it
        // conflicts with the bundled platform coroutine deps
        // leading to jvm linkage errors.
        exclude(module = "kotlinx-coroutines-core-jvm")
    }
    implementation(libs.compose.material3) {
        // Excluding the transitive coroutine dependency as it
        // conflicts with the bundled platform coroutine deps
        // leading to jvm linkage errors.
        exclude(module = "kotlinx-coroutines-core-jvm")
    }
    implementation(libs.compose.runtime)
    implementation(libs.kotlinx.serialization)
    intellijPlatform {
        pluginModule(implementation(project(":base")))
    }
}
