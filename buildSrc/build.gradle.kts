plugins {
    kotlin("jvm") version "1.4.10"
    `java-gradle-plugin`
}

repositories {
    jcenter()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
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