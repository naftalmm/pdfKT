package com.github.naftalmm.gradle.task

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.jvm.tasks.Jar

open class OneJar : Jar() {
    @InputFiles
    val depLibs: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    val binLibs: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFiles
    val additionalFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @InputFile
    val baseJar: RegularFileProperty = project.objects.fileProperty()

    @Input
    val useStable: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    val oneJarConfiguration: Property<Configuration> = project.objects.property(Configuration::class.java)

    @Input
    val mergeManifestFromBaseJar: Property<Boolean> = project.objects.property(Boolean::class.java)
//
//    @InputFile
//    val manifestFile: RegularFileProperty = project.objects.fileProperty()
//
//    boolean showExpand = false ???
//    boolean confirmExpand = false ???
//
//    @Input
//    val mainClass: Property<String> = project.objects.property(String::class.java)

    init {
        description = "Create a One-JAR runnable archive from the current project using a given main Class."
        archiveClassifier.set("standalone")
    }
}