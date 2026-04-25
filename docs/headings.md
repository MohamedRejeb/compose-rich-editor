# Headings

The Rich Text Editor models headings as a **first-class paragraph field**, not
as a visual style fingerprint. Every paragraph carries a `HeadingStyle` (levels
`Normal` and `H1`..`H6`), so heading identity survives theme changes, font
customization, and round-trips through HTML and Markdown.

## Basic Usage

### Applying a heading

```kotlin
// Make the current paragraph an H2
richTextState.setHeadingStyle(HeadingStyle.H2)

// Remove heading level (back to a normal paragraph)
richTextState.setHeadingStyle(HeadingStyle.Normal)
```

`setHeadingStyle` applies to **every paragraph that intersects the current
selection**. Wrap the call in `recordHistory` automatically so undo/redo
restores heading changes alongside other formatting.

### Reading the current level

```kotlin
val current = richTextState.currentHeadingStyle

val isH1 = current == HeadingStyle.H1
val level = current.level  // 0 for Normal, 1..6 for H1..H6
```

Use this from toolbar code to highlight the active heading button:

```kotlin
HeadingButton(
    label = "H2",
    isActive = state.currentHeadingStyle == HeadingStyle.H2,
    onClick = {
        val next = if (state.currentHeadingStyle == HeadingStyle.H2)
            HeadingStyle.Normal
        else
            HeadingStyle.H2
        state.setHeadingStyle(next)
    },
)
```

## Heading Levels

`HeadingStyle` is an enum with seven values:

| Style | Level | HTML | Markdown | Default size | Weight |
|---|---|---|---|---|---|
| `Normal` | 0 | *(none)* | *(none)* | inherited | inherited |
| `H1` | 1 | `<h1>` | `# ` | `2.0em` | Bold |
| `H2` | 2 | `<h2>` | `## ` | `1.5em` | Bold |
| `H3` | 3 | `<h3>` | `### ` | `1.17em` | Bold |
| `H4` | 4 | `<h4>` | `#### ` | `1.12em` | Bold |
| `H5` | 5 | `<h5>` | `##### ` | `0.83em` | Bold |
| `H6` | 6 | `<h6>` | `###### ` | `0.75em` | Bold |

The defaults are framework-agnostic (em-based) so the core library doesn't
depend on Material 2 or 3. Your app's `TextStyle` on the editor composable is
still respected as the base from which `em` sizes derive.

### Converting between levels

```kotlin
// From an integer (e.g. a toolbar picker)
val style = HeadingStyle.fromLevel(3)  // H3

// From an HTML tag name
val fromHtml = HeadingStyle.fromHtmlTag("h2")  // H2
val unknown  = HeadingStyle.fromHtmlTag("div") // Normal

// Access the serialization constants directly
HeadingStyle.H3.markdownPrefix   // "### "
HeadingStyle.H3.htmlTag          // "h3"
```

## Serialization

### HTML

Paragraphs with a heading level serialize to their corresponding tag:

```kotlin
state.setMarkdown("# Title\n\nBody paragraph.")
val html = state.toHtml()
// <h1>Title</h1><p>Body paragraph.</p>
```

On import, recognized tags (`h1`..`h6`) become the matching `HeadingStyle`;
other block tags stay as `Normal` paragraphs.

### Markdown

ATX-style headings (`#`..`######` followed by a space) import and export
losslessly:

```kotlin
state.setHtml("<h2>Section</h2><p>Body.</p>")
val markdown = state.toMarkdown()
// "## Section\n\nBody."
```

Setext-style headings (`Title\n=====`) are not supported on export but may be
parsed on import depending on the Markdown dialect.

## Why a first-class field?

Earlier revisions of the library modeled headings as a bundle of
`SpanStyle`/`ParagraphStyle` (e.g. "this paragraph looks bold at 2em, so it
must be an H1"). That approach breaks the moment a user applies partial
formatting - italicizing one word in an H1 shouldn't demote it.

Storing the level directly on `RichParagraph.headingStyle` means:

- **Toolbar highlighting stays accurate** regardless of partial inline styling.
- **Round-trips are lossless** - `setHtml → toHtml` and `setMarkdown →
  toMarkdown` preserve the heading, even if the user tweaks font size mid-line.
- **Theme changes update visuals without losing structure** - swap the app
  typography and every H2 re-renders at the new size without re-tagging.

## Related

- For inline formatting on heading text, see [Span Style](span_style.md).
- For other paragraph-level properties (alignment, indentation), see
  [Paragraph Style](paragraph_style.md).
- For HTML/Markdown conversion details, see
  [HTML Import and Export](html_import_export.md) and
  [Markdown Import and Export](markdown_import_export.md).
