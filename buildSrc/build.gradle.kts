plugins {
    kotlin("jvm") version "1.9.24"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.9"
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
