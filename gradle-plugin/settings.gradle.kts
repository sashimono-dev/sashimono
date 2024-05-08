
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()

        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
}
