// Root build file. Plugins are declared here (without applying) so subprojects can apply them.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

// Lifecycle task that proves the generator's output compiles against the real Compose API.
// It regenerates representative snippets (see :composeApp generateSamples) and compiles them in
// the :codegenVerify module, so a generator change that produces uncompilable code fails the build.
tasks.register("verifyGeneratedCode") {
    group = "verification"
    description = "Compiles generated MeshGradient samples against the real Compose API."
    dependsOn(":codegenVerify:classes")
}
