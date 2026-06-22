import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `maven-publish`
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.sonarqube)
}

allprojects {
    group = "io.github.softwarej"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    pluginManager.withPlugin("java-library") {
        pluginManager.apply("jacoco")
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }

            withSourcesJar()
            withJavadocJar()
        }

        extensions.configure<JacocoPluginExtension> {
            toolVersion = libs.versions.jacoco.get()
        }

        extensions.configure<SpotlessExtension> {
            java {
                target("src/**/*.java")
                googleJavaFormat()
                removeUnusedImports()
                trimTrailingWhitespace()
                endWithNewline()
            }

            kotlinGradle {
                target("*.gradle.kts")
                trimTrailingWhitespace()
                endWithNewline()
            }

            format("misc") {
                target("*.md", ".gitignore")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED
            )
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
            showStandardStreams = false
        }

        addTestListener(object : TestListener {
            override fun beforeSuite(suite: TestDescriptor) = Unit

            override fun beforeTest(testDescriptor: TestDescriptor) = Unit

            override fun afterTest(
                testDescriptor: TestDescriptor,
                result: TestResult
            ) = Unit

            override fun afterSuite(
                suite: TestDescriptor,
                result: TestResult
            ) {
                if (suite.parent == null) {
                    println()
                    println("Test result: ${result.resultType}")
                    println(
                        "Tests: ${result.testCount}, " +
                                "passed: ${result.successfulTestCount}, " +
                                "failed: ${result.failedTestCount}, " +
                                "skipped: ${result.skippedTestCount}"
                    )
                    println()
                }
            }
        })

        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.withType<Test>())

        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "CamilYed_spring-reactive-transaction-boundary")
        property("sonar.organization", "camilyed")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            listOf(
                "reactive-transaction-api/build/reports/jacoco/test/jacocoTestReport.xml",
                "reactive-transaction-spring/build/reports/jacoco/test/jacocoTestReport.xml",
                "reactive-transaction-spring-boot-autoconfigure/build/reports/jacoco/test/jacocoTestReport.xml"
            ).joinToString(",")
        )
    }
}