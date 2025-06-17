plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler.plugin)
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 58
        branch = 49
    }
}

dependencies {
    implementation(libs.compose.runtime)
    intellijPlatform {
        pluginModule(implementation(project(":base")))
    }
}
