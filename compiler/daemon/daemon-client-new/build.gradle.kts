/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Daemon Client New"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

val nativePlatformVariants = listOf(
    "windows-amd64",
    "windows-i386",
    "osx-amd64",
    "osx-i386",
    "linux-amd64",
    "linux-i386",
    "freebsd-amd64-libcpp",
    "freebsd-amd64-libstdcpp",
    "freebsd-i386-libcpp",
    "freebsd-i386-libstdcpp"
)

val ktorExcludesForDaemon : List<Pair<String, String>> by rootProject.extra

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:daemon-common-new"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":kotlin-daemon-client"))
    embeddedComponents(project(":kotlin-daemon-client")) { isTransitive = false }
    compileOnly(project(":js:js.frontend"))
    compileOnly(commonDep("net.rubygrapefruit", "native-platform"))
    compileOnly(intellijDep()) { includeIntellijCoreJarDependencies(project) }

    embeddedComponents(project(":compiler:daemon-common")) { isTransitive = false }
    embeddedComponents(commonDep("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embeddedComponents(commonDep("net.rubygrapefruit", "native-platform", "-$it"))
    }
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8")) {
        isTransitive = false
    }
    compile(commonDep("io.ktor", "ktor-network")) {
        ktorExcludesForDaemon.forEach { (group, module) ->
            exclude(group = group, module = module)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

noDefaultJar()

runtimeJar(task<ShadowJar>("shadowJar")) {
    from(mainSourceSet.output)
    fromEmbeddedComponents()
}

sourcesJar()

javadocJar()

dist()

ideaPlugin()
