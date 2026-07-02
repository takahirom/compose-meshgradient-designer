@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // The published app targets wasmJs only. The jvm target is tooling-only: it lets the code
    // generator run on the JVM (see :composeApp:generateSamples) and compile-check its output
    // against the real Compose API (see the :codegenVerify module and ./gradlew verifyGeneratedCode).
    jvmToolchain(21)

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.ui)
            }
        }
    }
}

// Runs the code generator on fixed sample states and writes each snippet as a compilable .kt file.
// The output feeds the :codegenVerify module, which compiles the files against Compose.
val generatedSamplesDir: Provider<Directory> = layout.buildDirectory.dir("generatedSamples")

val generateSamples = tasks.register<JavaExec>("generateSamples") {
    group = "verification"
    description = "Generates representative MeshGradient code snippets as .kt files."
    val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")
    dependsOn(jvmMainCompilation.compileTaskProvider)
    classpath(jvmMainCompilation.output.allOutputs, jvmMainCompilation.runtimeDependencyFiles)
    mainClass.set("mesh.tools.GenerateSamplesKt")
    args(generatedSamplesDir.get().asFile.absolutePath)
    outputs.dir(generatedSamplesDir)
}
