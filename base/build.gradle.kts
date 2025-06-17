plugins {
    id("simplej.java-library")
    id("org.jetbrains.intellij.platform.module")
}

simpleJ {
    intellijPlugin = true
    unitTestCoverageMinimums {
        instruction = 12
        branch = 17
    }
}
