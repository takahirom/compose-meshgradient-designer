# MeshGradient Designer

https://github.com/user-attachments/assets/5bd5cf52-d1e8-4f62-8bd2-2b197659c27a

A browser-based design tool for Compose mesh gradients. Edit a mesh gradient visually on the
left — drag vertices, bend edges with bezier handles, animate between keyframes — and copy
ready-to-paste Kotlin code on the right.

Built with [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) for Web
(Kotlin/Wasm), rendering with the same `MeshGradientPainter` API the generated code uses, so the
preview is pixel-faithful to what you ship.

## Features

- **Visual editing** — drag vertices, tap to recolor, and shape edges with four-directional
  bezier control handles on a live canvas.
- **Keyframe animation** — capture keyframes on a timeline, interpolate between them with
  per-segment easing (linear / ease-in / ease-out / ease-in-out), and loop as ping-pong or a
  seamless one-way cycle.
- **Kotlin code generation** — the right pane always shows compilable Kotlin for the current
  design (static or animated) targeting `MeshGradientPainter`, which works on both Compose
  Multiplatform 1.12.0-beta01+ and Jetpack Compose UI 1.12.0-beta01+.
- **Animated presets** — Sunset, Aurora, and Ocean load complete animations showcasing easing
  and bezier motion.
- **Export / Import** — save the whole project as versioned JSON (clipboard + file download) and
  load it back later. Straight edges (`Offset.Unspecified`) survive the round trip losslessly.

## Development

Requires JDK 21. The app targets Kotlin/Wasm only.

```sh
# Run the dev server (http://localhost:8080)
./gradlew wasmJsBrowserDevelopmentRun

# Run unit tests (headless Chrome; point CHROME_BIN at a Chrome binary)
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :composeApp:wasmJsBrowserTest

# Verify that generated code snippets compile against the real Compose API
./gradlew verifyGeneratedCode

# Production build (static site under composeApp/build/dist/wasmJs/productionExecutable)
./gradlew wasmJsBrowserDistribution
```

## Deployment

Pushes to `main` build the production distribution and deploy it to GitHub Pages via
`.github/workflows/deploy.yml`. Enable it once in the repository settings:
**Settings → Pages → Build and deployment → Source: GitHub Actions**.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).

The mesh editing interactions are ported from the mesh gradient playground demo in the Android
Open Source Project samples (Apache-2.0).
