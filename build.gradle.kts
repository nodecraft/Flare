import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
    idea
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.3"
    id("com.google.protobuf") version "0.9.4"
}

group = "com.nodecraft.hytale"
version = "1.0.2"

// Hytale home directory (cross-platform)
val hytaleHome = when {
    System.getProperty("os.name").lowercase().contains("windows") -> {
        "${System.getenv("APPDATA") ?: System.getProperty("user.home")}\\Hytale"
    }
    System.getProperty("os.name").lowercase().contains("mac") -> {
        "${System.getProperty("user.home")}/Library/Application Support/Hytale"
    }
    else -> { // Linux and other Unix-like systems
        val xdgDataHome = System.getenv("XDG_DATA_HOME")
        if (xdgDataHome != null) {
            "$xdgDataHome/Hytale"
        } else {
            "${System.getProperty("user.home")}/.local/share/Hytale"
        }
    }
}
val serverRunDir = file("run")
// Ensure it exists
serverRunDir.mkdirs()

var resolvedServerVersion: String? = null

fun resolveServerVersion(): String? {
    if (resolvedServerVersion != null) {
        return resolvedServerVersion
    }

    val artifact = configurations
        .findByName("compileClasspath")
        ?.resolvedConfiguration
        ?.resolvedArtifacts
        ?.find { cand ->
            cand.moduleVersion.id.group == "com.hypixel.hytale" && cand.name == "Server"
        }

    if (artifact == null) {
        resolvedServerVersion = ""
        logger.lifecycle("Failed to resolve HytaleServer version")
    }

    resolvedServerVersion = artifact?.moduleVersion?.id?.version
    return resolvedServerVersion
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.hytale.com/release")
    }
    // Local libs directory for HytaleServer JAR
    flatDir {
        dirs("libs")
    }
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:2026.02.17-255364b8e")
    compileOnly("com.google.code.gson:gson:2.10.1")
    implementation("com.google.protobuf:protobuf-java:3.25.3")
    
    // AP-Loader: Bundles async-profiler with native libraries for all platforms
    // This simplifies deployment by handling platform-specific binaries automatically
    // See: https://github.com/jvm-profiling-tools/ap-loader
    implementation("me.bechberger:ap-loader-all:4.2-10")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
}

tasks {

    processResources {
        val serverVer = resolveServerVersion() ?: ""
        inputs.property("version", project.version)
        inputs.property("serverVersion", serverVer)
        filesMatching("manifest.json") {
            expand(
                "project" to mapOf("version" to project.version),
                "serverVersion" to serverVer
            )
        }
    }

    jar {
        archiveBaseName.set("Flare")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Multi-Release" to "true"
                )
            )
        }
    }

    shadowJar {
        archiveBaseName.set("Flare")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Multi-Release" to "true"
                )
            )
        }
        // Exclude signature files (equivalent to maven-shade-plugin filters)
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        // Merge service files (equivalent to ServicesResourceTransformer)
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}

// IDEA run configuration for HytaleServer
idea {
    project {
        settings {
            runConfigurations {
                create("HytaleServer", org.jetbrains.gradle.ext.Application::class.java) {
                    mainClass = "com.hypixel.hytale.Main"
                    val assetsPath = when {
                        System.getProperty("os.name").lowercase().contains("windows") -> {
                            "$hytaleHome\\install\\release\\package\\game\\latest\\Assets.zip"
                        }
                        else -> {
                            "$hytaleHome/install/release/package/game/latest/Assets.zip"
                        }
                    }
                    programParameters = "--allow-op --assets=\"$assetsPath\" --accept-early-plugins"
                    workingDirectory = serverRunDir.absolutePath
                }
            }
        }
    }
}
