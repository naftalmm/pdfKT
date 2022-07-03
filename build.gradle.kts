plugins {
    kotlin("jvm") version "1.7.0"
    id("edu.sc.seis.launch4j") version "2.5.1"
    id("my-gradle-one-jar")
    id("com.github.ben-manes.versions") version "0.39.0"
//    id("com.github.onslip.gradle-one-jar") version "1.0.5"
}

group = "mm.naftal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            ivy {
                isAllowInsecureProtocol = true
                url = uri("http://anonsvn.icesoft.org/repo/maven2/releases/")
                patternLayout {
                    setM2compatible(true)
                    //http://anonsvn.icesoft.org/repo/maven2/releases/org/icepdf/os/icepdf-core/6.3.0/icepdf-core-6.3.0.jar
                    artifact("[organization]/[module]/[revision]/[artifact]-[revision].[ext]")
                    artifact("[organization]/[module]/[revision]/[artifact]-[revision]-[type].[ext]")
                    metadataSources { artifact() }
                }
            }
        }
        filter {
            includeGroup("org.icepdf.os")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.icepdf.os:icepdf-core:6.3.0")
    implementation("com.itextpdf:kernel:7.1.16")
    testImplementation("com.itextpdf:layout:7.1.16")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.assertj", "assertj-swing-junit", "3.17.1")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
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
    copyConfigurable = emptySet<File>()
    downloadUrl = "https://jdk.java.net/"
    jarTask = tasks.onejar.get()
//    icon = "${projectDir}/icons/myApp.ico"
}