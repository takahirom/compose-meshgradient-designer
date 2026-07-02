// Verification-only module. Its single source set is the directory of .kt files produced by
// :composeApp:generateSamples. Compiling this module against the real Compose API is the regression
// gate: if the generator emits code that does not compile, `./gradlew verifyGeneratedCode` fails.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.animation)
    implementation(compose.ui)
}

// Pull the generated snippets into this module's main source set.
val generatedSamplesDir = project(":composeApp").layout.buildDirectory.dir("generatedSamples")

kotlin {
    sourceSets {
        val main by getting {
            kotlin.srcDir(generatedSamplesDir)
        }
    }
}

// The generated sources must exist and be up to date before we compile them.
tasks.named("compileKotlin") {
    dependsOn(":composeApp:generateSamples")
}
