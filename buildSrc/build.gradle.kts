plugins {
    kotlin("jvm") version "1.7.0"
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    sourceSets.all {
        languageSettings.apply {
            languageVersion = "1.7"
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
