plugins {
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform.module")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 56
        branch = 50
    }
}
