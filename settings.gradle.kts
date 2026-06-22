pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "spring-reactive-transaction-boundary"

include(
    "reactive-transaction-api",
    "reactive-transaction-spring",
    "reactive-transaction-spring-boot-autoconfigure",
    "reactive-transaction-spring-boot-starter"
)