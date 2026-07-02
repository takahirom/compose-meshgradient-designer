@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package mesh.ui

// The Compose ClipboardManager on wasmJs does not expose a synchronous plain-text write, so we go
// straight to the browser Async Clipboard API via JS interop. writeText returns a Promise; we
// intentionally ignore it because the copy button is fire-and-forget.
private fun writeTextToClipboard(text: String): Unit =
    js("{ navigator.clipboard.writeText(text); }")

actual fun copyToClipboard(text: String) {
    writeTextToClipboard(text)
}

// Builds a Blob, points a temporary anchor at an object URL, clicks it to start the download, then
// revokes the URL. This is the standard browser file-save idiom; there is no synchronous CMP API.
private fun triggerDownload(fileName: String, text: String): Unit = js(
    """{
        const blob = new Blob([text], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }""",
)

actual fun downloadTextFile(fileName: String, text: String) {
    triggerDownload(fileName, text)
}
