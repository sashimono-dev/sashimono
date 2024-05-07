
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

java {
    withSourcesJar()
    withJavadocJar()
}
dependencies {
    implementation("dev.sashimono:config-generator:" + project.version)
}

group = "dev.sashimono"

gradlePlugin {
    plugins.create("sashimonoPlugin") {
        id = "dev.sashimono"
        implementationClass = "dev.sashimono.gradle.SashimonoPlugin"
        displayName = "Sashimono Plugin"
        description =
            "Generates a Sashimno build config"
        tags.addAll("sashimono")
    }
}
