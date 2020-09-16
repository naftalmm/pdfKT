package com.github.naftalmm.gradle

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class GradleOneJarPluginExtension(objects: ObjectFactory) {
    val depLibs: ConfigurableFileCollection = objects.fileCollection()
    val binLibs: ConfigurableFileCollection = objects.fileCollection()
    val additionalFiles: ConfigurableFileCollection = objects.fileCollection()
    val baseJar: RegularFileProperty = objects.fileProperty()
    val useStable: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val silent: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val oneJarConfiguration: Property<Configuration> = objects.property(Configuration::class.java)
    val mergeManifestFromBaseJar: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
}
