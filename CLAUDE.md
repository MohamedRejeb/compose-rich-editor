# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Compose Rich Editor is a WYSIWYG rich text editor library for Jetpack Compose and Compose Multiplatform (Android, iOS, Desktop, Web JS, Web Wasm). Published to Maven Central as `com.mohamedrejeb.richeditor:richeditor-compose`.

## Build Commands

```bash
# Build the library
./gradlew :richeditor-compose:build

# Run all tests (all platforms - requires macOS for iOS)
./gradlew allTests

# Run common + desktop tests only (fastest feedback loop)
./gradlew :richeditor-compose:desktopTest

# Run a single test class
./gradlew :richeditor-compose:desktopTest --tests "com.mohamedrejeb.richeditor.model.RichTextStateTest"

# API compatibility check (Binary Compatibility Validator)
./gradlew apiCheck

# Dump new API after public API changes
./gradlew apiDump

# Run the desktop sample app
./gradlew :sample:desktop:run
```

Requires JDK 17. Uses Kotlin 2.1.10, Compose Multiplatform 1.7.3.

## Architecture

### Core Model (tree-based)

`RichTextState` is the central class — a Compose state holder managing the editor's content as a list of `RichParagraph`, each containing a tree of `RichSpan` nodes.

```
RichTextState
  └── richParagraphList: List<RichParagraph>
        └── children: List<RichSpan>  (tree — each span has parent/children)
              ├── text: String
              ├── spanStyle: SpanStyle        (Compose text styling)
              └── richSpanStyle: RichSpanStyle (semantic: Link, Code, Image, Default)
```

- **Style inheritance**: `RichSpan.fullSpanStyle` merges styles up through the parent chain
- **Dual style system**: `SpanStyle` for visual formatting (bold, color, etc.); `RichSpanStyle` for semantic span types that need custom drawing or behavior
- **Paragraph types**: `DefaultParagraph`, `OrderedList`, `UnorderedList` — each manages its own prefix text (bullet/number) via `ParagraphType.startRichSpan`

### Modules

| Module | Purpose |
|---|---|
| `richeditor-compose` | Core library — models, parsers, UI composables |
| `richeditor-compose-coil3` | Optional Coil3 image loading integration |
| `convention-plugins` | Gradle convention plugins for Maven Central publishing |
| `sample/` | Demo app (common/android/desktop/web submodules) |

### Key Packages (`richeditor-compose/src/commonMain/`)

| Package | Contains |
|---|---|
| `model/` | `RichTextState`, `RichSpan`, `RichSpanStyle`, `RichTextConfig`, `ImageLoader` |
| `paragraph/` | `RichParagraph`, paragraph types (`OrderedList`, `UnorderedList`, `DefaultParagraph`) |
| `parser/html/` | HTML encode/decode using Ksoup |
| `parser/markdown/` | Markdown encode/decode using intellij-markdown |
| `ui/` | `BasicRichTextEditor`, `RichTextEditor` (Material), `OutlinedRichTextEditor` (Material3) |
| `clipboard/` | Platform-specific clipboard managers (expect/actual) |

### Platform Source Sets

Each platform provides `actual` implementations for:
- `Platform` enum value (`currentPlatform`)
- `Modifier.adjustTextIndicatorOffset(...)` — platform-specific BasicTextField cursor fixes
- `RichTextClipboardManager` — clipboard read/write (desktop has the richest implementation with HTML clipboard support via AWT `DataFlavor`)

### Parsers

`RichTextStateParser<T>` interface with `encode(T): RichTextState` and `decode(RichTextState): T`. Two implementations:
- `RichTextStateHtmlParser` — uses Ksoup for HTML parsing
- `RichTextStateMarkdownParser` — uses JetBrains intellij-markdown

### Image Extensibility

`ImageLoader` interface + `LocalImageLoader` CompositionLocal. The `richeditor-compose-coil3` module provides `Coil3ImageLoader` as a drop-in implementation.

## API Conventions

- Library uses `explicitApi()` — all public declarations must have explicit visibility modifiers
- `@ExperimentalRichTextApi` marks unstable public API (e.g., `RichSpanStyle`)
- `@InternalRichTextApi` marks internal-to-library API that shouldn't be used externally
- Most model classes (`RichParagraph`, `RichSpan`) are `internal` — public API surface is primarily `RichTextState` and the composable functions

## Testing

Tests use `kotlin.test` framework. Desktop tests use `runDesktopComposeUiTest` for Compose UI testing.

Key test locations:
- `richeditor-compose/src/commonTest/` — model, parser, and list behavior tests
- `richeditor-compose/src/desktopTest/` — key event handling and selection tests

## CI

GitHub Actions runs on PRs to `main`:
1. `apiCheck` on ubuntu (binary compatibility)
2. `allTests` on macOS (all platform tests)
3. Snapshot deploy on push to `main`
