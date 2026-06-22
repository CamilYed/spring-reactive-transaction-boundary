plugins {
    `java-library`
}

dependencies {
    api(project(":reactive-transaction-api"))
    api(libs.spring.tx)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.assertj.core)
    testImplementation(libs.reactor.test)

    testImplementation(libs.spring.r2dbc)
    testImplementation(libs.r2dbc.postgresql)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}
