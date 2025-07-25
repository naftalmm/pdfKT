plugins {
    kotlin("jvm") version "2.2.0"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
    sourceSets.all {
        languageSettings.apply {
            languageVersion = "2.2"
            progressiveMode = true
        }
    }
}

gradlePlugin {
    plugins {
        create("gradle-one-jar") {
            id = "my-gradle-one-jar"
            implementationClass = "com.github.naftalmm.gradle.GradleOneJarPlugin"
        }
    }
}
