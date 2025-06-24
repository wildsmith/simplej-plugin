// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import com.simplej.plugin.scripts.dsl.SimpleJOptions
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import java.math.BigDecimal

/**
 * Configures basic project settings and quality tools for the project.
 *
 * This function sets up essential project configurations including:
 * - Repository configurations:
 *   - Maven Central repository
 *   - Google's Maven repository
 * - Code quality tools:
 *   - Checkstyle for Java code style checking
 *   - Lint configuration
 *   - Detekt for Kotlin static code analysis
 */
internal fun Project.configureBaseProject(simpleJOptions: SimpleJOptions) {
    group = "com.simplej"

    repositories {
        mavenCentral()
        google()
    }
    loadLocalProperties()
    configureCheckstyle()
    configureLint(simpleJOptions)
    configureDetekt()
    configureTestTasks()
}

private fun Project.loadLocalProperties() {
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        val properties = java.util.Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.forEach { (name, value) ->
            extensions.extraProperties[name.toString()] = value
        }
    }
}

private fun Project.configureCheckstyle() {
    apply(plugin = "checkstyle")

    tasks.register("checkstyle", Checkstyle::class.java) {
        source("src/main/java", "src/test/java", "src/androidTest/java")
        include("**/*.java")
        exclude("$projectDir/build/**")
        classpath = files()
        configFile = rootProject.file("config/checkstyle.xml")
        ignoreFailures = false
        isShowViolations = true
        maxErrors = 0
        maxWarnings = 0
    }
}

private fun Project.configureLint(simpleJOptions: SimpleJOptions) {
    if (simpleJOptions.isAndroidLibrary) {
        androidLibrary {
            lint {
                abortOnError = true
                warningsAsErrors = true
                checkReleaseBuilds = false
            }
        }
    } else {
        apply(plugin = "com.android.lint")
        @Suppress("DEPRECATION")
        lint {
            isAbortOnError = true
            isWarningsAsErrors = true
            isCheckReleaseBuilds = false
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }
}

private fun Project.configureDetekt() {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        toolVersion = getDetektGradleVersion()
        config.setFrom(rootProject.file("config/detekt.yml"))
        parallel = true
    }

    tasks.withType(Detekt::class.java) {
        include("**/*.kt", "**/*.kts")
        exclude("$projectDir/build/**")
        reports {
            html.required.set(true)
        }
    }
}

private fun Project.configureTestTasks() {
    tasks.withType(Test::class.java).configureEach {
        maxParallelForks = Runtime.getRuntime().availableProcessors()
        useJUnitPlatform()
    }

    dependencies {
        val versionCatalog = getVersionCatalog()
        testImplementation(versionCatalog.findLibrary("mockk").get())
        testImplementation(versionCatalog.findLibrary("jupiter-api").get())
        testImplementation(versionCatalog.findLibrary("jupiter-engine").get())
        testImplementation(versionCatalog.findLibrary("kotlin-test").get())
    }
}

internal fun Project.configureJacoco(simpleJOptions: SimpleJOptions) {
    if (simpleJOptions.isAndroidLibrary) {
        // The only Android project in the codebase is used for previews, no code from it is bundled with the
        // plugin's artifact, ignore it for now
        return
    }

    apply(plugin = "jacoco")

    tasks.withType(Test::class.java).configureEach {
        val jacoco = extensions["jacoco"] as JacocoTaskExtension
        jacoco.isIncludeNoLocationClasses = true
        jacoco.setExcludes(listOf("jdk.internal.*"))

        finalizedBy("jacocoTestCoverageVerification")
    }

    tasks.named("jacocoTestCoverageVerification").configure {
        dependsOn("jacocoTestReport")
    }

    jacocoTestReport {
        reports {
            html.required.set(true)
        }
    }

    val coverageMinimums = simpleJOptions.coverageMinimums
    jacocoTestCoverageVerification {
        violationRules {
            rule {
                element = "BUNDLE"
                limit {
                    counter = "INSTRUCTION"
                    minimum = coverageMinimums.instruction.toJacocoBigDecimal()
                }
                limit {
                    counter = "BRANCH"
                    minimum = coverageMinimums.branch.toJacocoBigDecimal()
                }
                limit {
                    counter = "LINE"
                    minimum = coverageMinimums.line.toJacocoBigDecimal()
                }
                limit {
                    counter = "COMPLEXITY"
                    minimum = coverageMinimums.complexity.toJacocoBigDecimal()
                }
                limit {
                    counter = "METHOD"
                    minimum = coverageMinimums.method.toJacocoBigDecimal()
                }
                limit {
                    counter = "CLASS"
                    minimum = coverageMinimums.clazz.toJacocoBigDecimal()
                }
            }
        }
    }
}

/**
 * Jacoco's coverage ratio is between 0.0 and 1.0, convert the minimum from the SimpleJ Options to a compatible value
 * by dividing them by 100.0.
 */
private fun Int.toJacocoBigDecimal(): BigDecimal =
    if (this == 0) BigDecimal.ZERO else (this / 100.0).toBigDecimal()
