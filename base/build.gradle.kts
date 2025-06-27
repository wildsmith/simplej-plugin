plugins {
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform.module")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 40
        branch = 43
    }
}

dependencies {
    implementation(libs.androidx.annotations)
}
