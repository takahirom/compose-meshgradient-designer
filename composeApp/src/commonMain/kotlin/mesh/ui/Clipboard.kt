package mesh.ui

/** Copies [text] to the system clipboard. Implemented per target. */
expect fun copyToClipboard(text: String)

/** Triggers a browser download of [text] as a file named [fileName]. Implemented per target. */
expect fun downloadTextFile(fileName: String, text: String)
