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
}
