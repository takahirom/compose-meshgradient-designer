package mesh.ui

// The jvm target exists only for tooling (running the code generator and compile-checking its
// output). It renders no UI, so clipboard and file-download have no meaningful JVM behavior.

actual fun copyToClipboard(text: String) {
    // No-op: not used by the tooling entry points.
}

actual fun downloadTextFile(fileName: String, text: String) {
    // No-op: not used by the tooling entry points.
}
