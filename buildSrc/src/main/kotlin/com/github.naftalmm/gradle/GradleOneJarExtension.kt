package com.github.naftalmm.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class GradleOneJarExtension(objects: ObjectFactory) {
    val depLibs: ConfigurableFileCollection = objects.fileCollection()
    val binLibs: ConfigurableFileCollection = objects.fileCollection()
    val additionalFiles: ConfigurableFileCollection = objects.fileCollection()
    val baseJar: RegularFileProperty = objects.fileProperty()
    val useStable: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val mergeManifestFromJar: Property<Boolean> = objects.property(Boolean::class.java)
}
