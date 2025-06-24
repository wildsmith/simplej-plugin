plugins {
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform.module")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 43
        branch = 49
    }
}

dependencies {
    implementation(libs.androidx.annotations)
}
