plugins {
    java
    kotlin("jvm") version "1.4.10"
    id("edu.sc.seis.launch4j") version "2.4.8"
    id("my-gradle-one-jar")
//    id("com.github.onslip.gradle-one-jar") version "1.0.5"
}

group = "mm.naftal"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("org.icepdf.os:icepdf-core:6.3.0")
    implementation("com.itextpdf:itext7-core:7.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("org.assertj", "assertj-swing-junit", "3.9.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes("Main-Class" to "AppKt")
        }
    }
    //TODO delete after launch4j update
    createExe {
        jar = onejar.get().archiveFile.get().toString()
        dependsOn(onejar)
        outputs.upToDateWhen { !onejar.get().didWork }
    }
}

launch4j {
    copyConfigurable = emptySet<File>()
    downloadUrl = "https://jdk.java.net/"
//    jarTask = tasks.onejar //TODO uncomment after launch4j update
//    icon = "${projectDir}/icons/myApp.ico"
}