import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.sonarqube)
}

allprojects {
    group = "io.github.camilyed"
    version = providers.gradleProperty("releaseVersion")
        .orElse("0.1.0-SNAPSHOT")
        .get()
}

subprojects {
    pluginManager.withPlugin("java-library") {
        pluginManager.apply("jacoco")
        pluginManager.apply("maven-publish")
        pluginManager.apply("signing")
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

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])

                    pom {
                        name.set(project.name)
                        description.set(projectDescription(project.name))
                        url.set("https://github.com/CamilYed/spring-reactive-transaction-boundary")
                        inceptionYear.set("2026")

                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }

                        developers {
                            developer {
                                id.set("CamilYed")
                                name.set("CamilYed")
                                url.set("https://github.com/CamilYed")
                            }
                        }

                        scm {
                            url.set("https://github.com/CamilYed/spring-reactive-transaction-boundary")
                            connection.set(
                                "scm:git:git://github.com/CamilYed/spring-reactive-transaction-boundary.git"
                            )
                            developerConnection.set(
                                "scm:git:ssh://git@github.com:CamilYed/spring-reactive-transaction-boundary.git"
                            )
                        }
                    }
                }
            }

            repositories {
                maven {
                    name = "localBuild"
                    url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
                }

                maven {
                    name = "centralSnapshots"
                    url = uri("https://central.sonatype.com/repository/maven-snapshots/")

                    mavenContent {
                        snapshotsOnly()
                    }

                    credentials {
                        username = providers.gradleProperty("centralUsername")
                            .orElse(providers.environmentVariable("CENTRAL_USERNAME"))
                            .orNull
                        password = providers.gradleProperty("centralPassword")
                            .orElse(providers.environmentVariable("CENTRAL_PASSWORD"))
                            .orNull
                    }
                }
            }
        }

        val publishing = extensions.getByType(PublishingExtension::class.java)

        extensions.configure<SigningExtension> {
            val signingKey = providers.gradleProperty("signingKey")
                .orElse(providers.environmentVariable("SIGNING_KEY"))
                .orNull
            val signingPassword = providers.gradleProperty("signingPassword")
                .orElse(providers.environmentVariable("SIGNING_PASSWORD"))
                .orNull

            isRequired = isRemotePublishingRequested()

            if (!signingKey.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKey, signingPassword)
            }

            sign(publishing.publications)
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

fun projectDescription(projectName: String): String =
    when (projectName) {
        "reactive-transaction-api" ->
            "Spring-independent reactive transaction boundary API for application code."
        "reactive-transaction-spring" ->
            "Spring Framework adapter for the reactive transaction boundary API."
        "reactive-transaction-spring-boot-autoconfigure" ->
            "Spring Boot auto-configuration for the reactive transaction boundary API."
        "reactive-transaction-spring-boot-starter" ->
            "Spring Boot starter for the reactive transaction boundary API."
        else ->
            "Reactive transaction boundary support for Spring applications."
    }

fun Project.isRemotePublishingRequested(): Boolean =
    gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("publish", ignoreCase = true) &&
                !taskName.contains("MavenLocal", ignoreCase = true) &&
                !taskName.contains("LocalBuild", ignoreCase = true)
    }