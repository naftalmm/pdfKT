package com.github.naftalmm.gradle

import com.github.naftalmm.gradle.task.OneJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion
import java.io.File
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
 * </pre>
 */

class GradleOneJarPlugin : Plugin<Project> {
    private lateinit var oneJarStable: Configuration
    private lateinit var oneJarRC: Configuration

    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class.java)

        val extension = project.extensions.create("oneJar", GradleOneJarPluginExtension::class.java).apply {
            @Suppress("DEPRECATION")
            val javaSourceSets = if (GradleVersion.current() >= "7.1")
                project.extensions.getByType(JavaPluginExtension::class.java).sourceSets
            else
                project.convention.getPlugin(org.gradle.api.plugins.JavaPluginConvention::class.java).sourceSets

            val runtimeClasspathConfigurationName =
                javaSourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspathConfigurationName
            depLibs.from(project.configurations.getByName(runtimeClasspathConfigurationName))
            baseJar.convention((project.tasks.getByName("jar") as Jar).archiveFile)
        }

        configureOneJarBootDependency(project)
        registerOneJarTask(project, extension)
    }

    private fun configureOneJarBootDependency(project: Project) {
        val oneJarRepository = project.repositories.ivy { ivy ->
            ivy.url = project.uri("https://sourceforge.net/projects")
            ivy.patternLayout { layout ->
                //https://sourceforge.net/projects/one-jar/files/one-jar/0.98%20RCs/one-jar-0.98-RC2/one-jar-boot-0.98.jar
                //https://sourceforge.net/projects/one-jar/files/one-jar/one-jar-0.97.1/one-jar-boot-0.97.jar
                layout.artifact("[organization]/files/[organization]/0.98%20RCs/one-jar-0.98-RC2/[artifact]-[revision].[ext]")
                layout.artifact("[organization]/files/[organization]/one-jar-0.97.1/[artifact]-[revision].[ext]")
                ivy.metadataSources { it.artifact() }
            }
        }
        if (GradleVersion.current() >= "6.2") {
            project.repositories.exclusiveContent { exclusive ->
                exclusive.forRepository { oneJarRepository }
                exclusive.filter {
                    it.includeVersionByRegex("one-jar", "one-jar-boot", "0.97|0.98")
                }
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
        val oneJarTask = project.tasks.register("onejar", OneJar::class.java) { oneJar ->
            with(oneJar) {
                group = "build"

                depLibs.setFrom(extension.depLibs)
                binLibs.setFrom(extension.binLibs)
                additionalFiles.setFrom(extension.additionalFiles)
                baseJar.set(extension.baseJar)
                useStable.set(extension.useStable)
                silent.set(extension.silent)
                oneJarConfiguration.set(extension.oneJarConfiguration)
                mergeManifestFromBaseJar.set(extension.mergeManifestFromBaseJar)

                into("main") {
                    it.from(baseJar).apply {
                        rename { "main.jar" }
                    }
                }
                into("lib") {
                    it.from(depLibs)
                }
                into("binlib") {
                    it.from(binLibs)
                }
                from(additionalFiles)
            }
        }

        project.afterEvaluate {
            with(oneJarTask.get()) {
                val oneJarConfiguration: Configuration = when {
                    oneJarConfiguration.isPresent -> oneJarConfiguration.get()
                    useStable.get() -> oneJarStable
                    else -> oneJarRC
                }
                val oneJarBootContents = oneJarConfiguration.map { project.zipTree(it) }
                from(oneJarBootContents) {
                    if (oneJarConfiguration == oneJarStable || oneJarConfiguration == oneJarRC) {
                        exclude("src/**")
                    }
                }

                if (silent.get() && !this.oneJarConfiguration.isPresent) {
                    from(File(temporaryDir, "one-jar.properties").apply { writeText("one-jar.silent=true") })
                }

                //generated manifest of task overrides the one copied from oneJarBoot,
                // so they need to be merged with each other
                val oneJarBootManifest = oneJarBootContents.flatten().first {
                    it.isFile && it.name.endsWith("MANIFEST.MF")
                }
                manifest.from(oneJarBootManifest) { mergeSpec ->
                    //attributes in onejar task manifest
                    // takes precedence over the oneJarBoot ones
                    mergeSpec.eachEntry {
                        it.value = it.baseValue ?: it.mergeValue
                    }
                }

                if (mergeManifestFromBaseJar.get()) {
                    JarFile(baseJar.get().asFile).use { baseJar ->
                        manifest.attributes(baseJar.manifest.mainAttributes
                            .mapKeys { (k, _) -> k.toString() }
                            //attributes in onejar task manifest/oneJarBoot manifest
                            //takes precedence over the ones from baseJar
                            .filterKeys { !manifest.effectiveManifest.attributes.keys.contains(it) })
                    }
                }
            }
        }
    }
}

private operator fun GradleVersion.compareTo(version: String): Int = this.compareTo(GradleVersion.version(version))