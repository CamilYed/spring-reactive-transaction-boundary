plugins {
    `java-library`
}

dependencies {
    implementation(project(":reactive-transaction-spring"))
    implementation(libs.spring.boot.autoconfigure)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.assertj.core)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.boot.test)
}
