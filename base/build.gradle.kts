plugins {
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform.module")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 51
        branch = 50
    }
}

dependencies {
    implementation(libs.androidx.annotations)
}
