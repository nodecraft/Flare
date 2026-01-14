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
    // Local libs directory for HytaleServer JAR
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Use local JAR file if available, otherwise try Maven dependency
    val hytaleServerJar = file("libs/HytaleServer.jar")
    if (hytaleServerJar.exists()) {
        compileOnly(files(hytaleServerJar))
        println("Using local HytaleServer.jar from libs/")
    } else {
        // Fallback to Maven dependency (requires it to be installed in local Maven repo)
        compileOnly("com.hypixel.hytale:HytaleServer-parent:1.0-SNAPSHOT")
    }
    implementation("com.google.code.gson:gson:2.10.1")
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
        inputs.property("version", project.version)
        filesMatching("manifest.json") {
            expand("project" to mapOf("version" to project.version))
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
