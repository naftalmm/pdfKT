plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "mm.naftal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            ivy {
                url = uri("http://anonsvn.icesoft.org/repo/maven2/releases/")
                patternLayout {
                    setM2compatible(true)
                    //http://anonsvn.icesoft.org/repo/maven2/releases/org/icepdf/os/icepdf-core/6.3.0/icepdf-core-6.3.0.jar
                    artifact("[organization]/[module]/[revision]/[artifact]-[revision].[ext]")
                    artifact("[organization]/[module]/[revision]/[artifact]-[revision]-[type].[ext]")
                    metadataSources {
                        artifact()
                    }
                }
            }
        }
        filter {
            includeGroup("org.icepdf.os")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("org.icepdf.os:icepdf-core:6.3.0")
//    testImplementation("junit", "junit", "4.12")
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
}