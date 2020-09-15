package com.github.naftalmm.gradle

import com.github.naftalmm.gradle.task.OneJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import java.util.jar.JarFile

/**
 * This plugin rolls up your current project's jar and all of its dependencies
 * into the layout expected by One-JAR, producing a single runnable
 * fat-jar, similar to the following:
 *
 * <pre>
 * my-awesome-thing-1.2.3-standalone.jar
 * |
 * +---- com
 * |   +---- simontuffs
 * |       +---- onejar
 * |           +---- Boot.class
 * |           +---- (etc., etc.)
 * |           +---- OneJarURLConnection.class
 * +---- doc
 * |   +---- one-jar-license.txt
 * +---- lib
 * |   +---- other-cool-lib-1.7.jar
 * |   +---- some-cool-lib-2.5.jar
 * +---- main
 * |   +-- main.jar
 * +---- META-INF
 * |   +---- MANIFEST.MF
 * +---- OneJar.class
 * +---- .version
 *
 */

class GradleOneJarPlugin : Plugin<Project> {
    private lateinit var oneJarStable : Configuration
    private lateinit var oneJarRC : Configuration
    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class.java)

        val extension = project.extensions.create("oneJar", GradleOneJarPluginExtension::class.java).apply {
            val javaConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
            val runtimeClasspathConfigurationName =
                javaConvention.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspathConfigurationName
            depLibs.from(project.configurations.getByName(runtimeClasspathConfigurationName))
            baseJar.convention((project.tasks.getByName("jar") as Jar).archiveFile)
        }

        configureOneJarBootDependency(project)
        registerOneJarTask(project, extension)
    }

    private fun configureOneJarBootDependency(project: Project) {
        project.repositories.ivy { ivy ->
            ivy.url = project.uri("https://sourceforge.net/projects")
            ivy.patternLayout { layout ->
                //https://sourceforge.net/projects/one-jar/files/one-jar/0.98%20RCs/one-jar-0.98-RC2/one-jar-boot-0.98.jar
                //https://sourceforge.net/projects/one-jar/files/one-jar/one-jar-0.97.1/one-jar-boot-0.97.jar
                layout.artifact("[organization]/files/[organization]/0.98%20RCs/one-jar-0.98-RC2/[artifact]-[revision].[ext]")
                layout.artifact("[organization]/files/[organization]/one-jar-0.97.1/[artifact]-[revision].[ext]")
                ivy.metadataSources { it.artifact() }
            }
        }

        oneJarStable = project.configurations.create("onejar").apply {
            isVisible = false
            isTransitive = false
            description = "The oneJar boot stable configuration for this project"
            defaultDependencies {
                it.add(project.dependencies.create("one-jar:one-jar-boot:0.97"))
            }
        }

        oneJarRC = project.configurations.create("onejarRC").apply {
            isVisible = false
            isTransitive = false
            description = "The oneJar boot RC configuration for this project"
            defaultDependencies {
                it.add(project.dependencies.create("one-jar:one-jar-boot:0.98"))
            }
        }
    }

    private fun registerOneJarTask(project: Project, extension: GradleOneJarPluginExtension) {
        project.tasks.register("onejar", OneJar::class.java) { oneJar ->
            with(oneJar) {
                group = "build"

                depLibs.setFrom(extension.depLibs)
                binLibs.setFrom(extension.binLibs)
                additionalFiles.setFrom(extension.additionalFiles)
                baseJar.set(extension.baseJar)
                useStable.set(extension.useStable)
                mergeManifestFromBaseJar.set(extension.mergeManifestFromBaseJar)
                oneJarConfiguration.convention(if (useStable.get()) oneJarStable else oneJarRC)
                extension.oneJarConfiguration.map { oneJarConfiguration.set(it) }

                into("lib") {
                    it.from(depLibs)
                }
                into("binlib") {
                    it.from(binLibs)
                }
                from(additionalFiles)
                into("main") {
                    it.from(baseJar).apply {
                        rename { "main.jar" }
                    }
                }

                val oneJarBootContents = oneJarConfiguration.get().map { project.zipTree(it) }
                from(oneJarBootContents) {
                    if (oneJarConfiguration.get() == oneJarStable || oneJarConfiguration.get() == oneJarRC) {
                        exclude("src/**")
                    }
                }

                manifest.apply {
                    //generated manifest overrides the one from oneJarBoot, so they need to be merged with each other
                    oneJarBootContents.forEach {
                        it.visit(object : FileVisitor {
                            override fun visitDir(dirDetails: FileVisitDetails) {}

                            override fun visitFile(fileDetails: FileVisitDetails) {
                                if (fileDetails.path == "META-INF/MANIFEST.MF") {
                                    from(fileDetails.file)
                                    fileDetails.stopVisiting()
                                }
                            }
                        })
                    }

                    if (mergeManifestFromBaseJar.get()) {
                        println("mergeManifestFromBaseJar")
                        JarFile(baseJar.get().asFile).use {
                            from(it.manifest.mainAttributes)
                        }
                    }
                }
            }
        }
    }
}