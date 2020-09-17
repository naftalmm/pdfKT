## What is this?
This plugin extends the [Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html) 
and rolls up your current project's jar and all of its dependencies into the layout expected by One-JAR,
producing a single runnable fat-jar, similar to the following:

```
my-awesome-thing-1.2.3-standalone.jar
|
+---- com
|   +---- simontuffs
|       +---- onejar
|           +---- Boot.class
|           +---- (etc., etc.)
|           +---- OneJarURLConnection.class
+---- doc
|   +---- one-jar-license.txt
+---- lib
|   +---- other-cool-lib-1.7.jar
|   +---- some-cool-lib-2.5.jar
+---- main
|   +-- main.jar
+---- META-INF
|   +---- MANIFEST.MF
+---- OneJar.class
+---- one-jar.properties
+---- .version
```

You can read more about the layout of a One-JAR archive from the [official site](http://one-jar.sourceforge.net/).

## Quick Start
First, you'll want to add the plugin to your build, as in:

```kotlin
plugins {
    id("my-gradle-one-jar")
}
```

If jar task was properly configured, you are good to go:
```kotlin
tasks {
    jar {
        manifest {
            attributes("Main-Class" to "AppKt")
        }
    }
}
```

Alternatively, at a minimum, default onejar task may be configured in this way:

```kotlin
tasks {
    onejar {
        manifest {
            attributes("One-Jar-Main-Class" to "AppKt")
        }
    }
}
```

Then you can run the task with:

```bash
gradlew onejar
```

If you don't like the name of the final artifact, you can change it just like
any other Gradle `Jar` task with:
```kotlin
tasks.onejar {
    archiveFileName.set("koala.jar")
}
```

Plugin adds one new Task type - `com.github.naftalmm.gradle.task.OneJar`, 
extending built-in [Jar](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html) task with the following additional properties:
| Property | Type | Description | Default value |
|----------|------|-------------|---------------|
 depLibs | [ConfigurableFileCollection](https://docs.gradle.org/current/javadoc/org/gradle/api/file/ConfigurableFileCollection.html) | Set of files to be packed as libs |`configurations.runtimeClasspath`
 binLibs | [ConfigurableFileCollection](https://docs.gradle.org/current/javadoc/org/gradle/api/file/ConfigurableFileCollection.html) | Set of files to be packed as binary libs | null
 additionalFiles | [ConfigurableFileCollection](https://docs.gradle.org/current/javadoc/org/gradle/api/file/ConfigurableFileCollection.html) | Set of files to be packed additionaly in jar root | null
 baseJar | [RegularFileProperty](https://docs.gradle.org/current/javadoc/org/gradle/api/file/RegularFileProperty.html) | Jar to be packed as main.jar |`tasks.jar.archiveFile`
 useStable | Property<Boolean> | see [Selectable One-JAR version](#useStable), will be ignored if `oneJarConfiguration` property set | true |
 silent | Property<Boolean> | see [(Almost) Silent one-jar](#silent), will be ignored if `oneJarConfiguration` property set | true |
 oneJarConfiguration | Property<Configuration> | see [Bring your own One-JAR version](#oneJarConfiguration) | |
 mergeManifestFromBaseJar | see [Merge baseJar MANIFEST.MF entries](#mergeManifestFromBaseJar) | false |
 
Also, plugin registers one default task instance of this type (`onejar` in `build` group) 
and adds a `onejar` plugin extension to redefine default values of additional properties for all tasks of this type.

## Advanced Features
The current incarnation of the `my-gradle-one-jar` plugin exists as a highly
configurable Gradle task implementation built as an extension of the built-in
`Jar` task. The following is a non-exhaustive list of some of the more advanced
features that the plugin can perform to meet the varying needs of deploying
standardized artifacts.

### <a name="useStable">Selectable One-JAR version</a>
By default, the `one-jar-boot` version used is the stable
`one-jar-boot-0.97.jar` which is available from the One-JAR homepage (last
updated 2012-08-15). However, if you'd prefer to use the latest development
version `one-jar-boot-0.98.jar` (last updated 2010-08-25) then you can do so
with the following:

```kotlin
tasks.onejar {
    useStable.set(false)
}
```

### <a name="oneJarConfiguration">Bring your own One-JAR version</a>
By default, plugin registers two configurations: `onejar` and `onejarRC` (the first one used when `useStable=true`).

You can redefine their default dependencies to use your own customized version of a `one-jar-boot` jar
(assuming your root project directory contains the jar at `custom-boot/one-jar-boot-0.97.2-custom.jar`):
```kotlin
dependencies {
    onejar(files("custom-boot/one-jar-boot-0.97.2-custom.jar"))
}
```

This customization will be applied for all OneJar tasks. 
For more granular setting, define a new configuration with respectful dependency, 
and pass it to particular OneJar task(s) by using the oneJarConfiguration setting:

```kotlin
val unojar by configurations.creating

dependencies {
    unojar("com.needhamsoftware.unojar:core:+")
}

tasks.onejar {
    oneJarConfiguration.set(unojar)
}
```

### Use custom configuration for dependencies
By default, the plugin uses the current project's `runtimeClasspath` configuration to
resolve which dependencies are to be included in the final One-JAR archive. If
you would rather use your own custom configuration, you can set it as follows in
the task:

```kotlin
// add your own configuration
val fatJarBuild by configurations.creating

// declare dependencies for this configuration
dependencies {
    // only for compile
    implementation("org.slf4j:slf4j-api:1.7.2")

    // dependencies in fat jar
    fatJarBuild("org.slf4j:slf4j-api:1.7.2")
    fatJarBuild("org.slf4j:slf4j-simple:1.7.2")
}

// override target configuration
tasks.onejar {
    depLibs.setFrom(fatJarBuild)
}
```

### Custom MANIFEST.MF entries
By default, the MANIFEST.MF added to the final One-JAR archive contains only the
bare minimum number of attributes expected for `one-jar-boot` to behave
correctly (copied from manifest of one-jar-boot.jar). 
You can add your own custom attributes to the `manifest` property of a `OneJar` task just like a `Jar` task, such as in:

```kotlin
tasks.onejar {
    manifest {
        attributes("Timestamp" to System.currentTimeMillis(), "ContainsXML" to "No")
    }
}
```

They override ones from the manifest of `one-jar-boot.jar` (for matching attribute names).

See also: [One-JAR manifest attributes](http://one-jar.sourceforge.net/index.php?page=details&file=manifest).

### <a name="mergeManifestFromBaseJar">Merge baseJar MANIFEST.MF entries</a>
If you just want all the MANIFEST.MF entries that are present in your
baseJar to be merged with the entries defined in manifest of the final archive, then you can do so with:

```kotlin
tasks.onejar {
    mergeManifestFromBaseJar.set(true)
}
```

They DO NOT override ones from the manifest of `one-jar-boot.jar` and from the OneJar task manifest (for matching attribute names).

The overall priority for an attribute values collision:
OneJar task manifest > `one-jar-boot.jar` manifest > baseJar manifest

### Add native libraries
Files added to the `/binlib` directory within an archive get expanded to a
temporary directory on startup, and the One-JAR JarClassLoader loads them
automatically. To get your own native library files included in your archive,
try something like this:

```kotlin
tasks.onejar {
    binLibs.setFrom(files("libFoo.so"))
}
```

### Add any files to the root archive
If you just want to be able to drop arbitrary files into the root of the
generated archive, then you can specify a directory (which will also include its
children) to be copied over the top of the existing files with:

```kotlin
tasks.onejar {
    additionalFiles.setFrom(file("someDirFilledWithGoodies"))
}
```

### Framework ClassLoader customizations
[Spring](http://www.springsource.org/), [Guice](https://code.google.com/p/google-guice/),
and even [JavaFX](http://docs.oracle.com/javafx/)'s [FXML](http://docs.oracle.com/javafx/2/api/javafx/fxml/doc-files/introduction_to_fxml.html)
make certain assumptions about class loading that may not hold when bundling
projects in a One-JAR archive. The workaround for these cases is documented
[here](http://one-jar.sourceforge.net/index.php?page=frameworks). In order to
enable this functionality in the plugin, you can simply add one of the included
factories to the manifest, as in:

```kotlin
tasks.onejar {
    manifest {
        attributes("One-Jar-URL-Factory" to "com.simontuffs.onejar.JarClassLoader$OneJarURLFactory")
    }
}
```

### Override the base jar
By default, the `java` plugin's `jar` task 'archiveFile' is a source for OneJar to create the `main/main.jar`
entry in the final One-JAR archive. However, it is possible to override this default with:

```kotlin
tasks.onejar {
    baseJar.set(someOtherJarTask.archiveFile)
}
```

### <a name="silent">(Almost) Silent one-jar</a>
By default, `one-jar-boot.jar` (both stable and RC versions) makes logging with INFO level. 
To mitigate this annoying logs, plugin by default (if `oneJarConfiguration` is not overridden) 
includes a `one-jar.properties` file (with `one-jar.silent=true` setting) into the root of created one-jar.
This eliminates almost all logs, except for the initial:
```
[Boot] INFO:  loading properties from one-jar.properties
```
(for stable version)

```
myJarPath=...
oneJarPath=...
```
(for RC version)

To return default logging level:
                                       
```kotlin
tasks.onejar {
   silent.set(false)
}
```

## Building from source
<!-- TODO rework -->
The `gradle-one-jar` build plugin uses a [Gradle](http://gradle.org)-based build system. In the instructions
below, [`./gradlew`](http://vimeo.com/34436402) is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build. The only
prerequisites are [Git](https://help.github.com/articles/set-up-git) and JDK 1.8+.

### check out sources
`git clone git://github.com/rholder/gradle-one-jar.git`

### compile and test, build all jars
`./gradlew build`

### install all jars into your local Maven cache
`./gradlew install`

## License
The `gradle-one-jar` build plugin is released under version 2.0 of the
[Apache License](http://www.apache.org/licenses/LICENSE-2.0). Distributions
built with this plugin are subject to the terms set forth
[here](http://one-jar.sourceforge.net/index.php?page=documents&file=license).
The One-JAR license is a BSD-style license. Compliance with this license is
assured by including the one-jar-license.txt file in the One-JAR archive, which
this plugin does automatically.

## Contributors
* Jochen Schalanda (joschi)
* Christian S. (squiddle)
* Ben Manes (ben-manes)
* Adam Walczak (walec51)
