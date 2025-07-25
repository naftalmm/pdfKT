plugins {
    kotlin("jvm") version "1.9.24"
    id("edu.sc.seis.launch4j") version "3.0.3"
    id("my-gradle-one-jar")
    id("com.github.ben-manes.versions") version "0.47.0"
//    id("com.github.onslip.gradle-one-jar") version "1.0.5"
}

group = "mm.naftal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.github.pcorless.icepdf:icepdf-core:7.3.0")
    val iTextPdfVersion = "9.2.0"
    implementation("com.itextpdf:kernel:$iTextPdfVersion")
    testImplementation("com.itextpdf:layout:$iTextPdfVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj", "assertj-swing-junit", "3.17.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
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

tasks {
    test {
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes("Main-Class" to "AppKt")
        }
    }
    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = "current"
    }
}

launch4j {
    copyConfigurable.set(emptySet<File>())
    downloadUrl.set("https://jdk.java.net/")
    setJarTask(tasks.onejar.get())
//    icon = "${projectDir}/icons/myApp.ico"
}