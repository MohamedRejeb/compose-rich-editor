# HTML Import and Export

The Rich Text Editor supports converting between HTML and rich text content. This allows you to:
- Save editor content as HTML
- Load content from HTML sources
- Integrate with HTML-based systems

## Importing HTML

To convert HTML to `RichTextState`, use the `setHtml` method:

```kotlin
// Basic formatting
val simpleHtml = """
    <p><b>Bold</b> and <i>italic</i> text with <u>underline</u></p>
"""
richTextState.setHtml(simpleHtml)

// Complex structure
val complexHtml = """
    <div>
        <h1>Title</h1>
        <p>Paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
        <ul>
            <li>Unordered list item 1</li>
            <li>Unordered list item 2</li>
        </ul>
        <ol>
            <li>Ordered list item 1</li>
            <li>Ordered list item 2</li>
        </ol>
        <p>Link to <a href="https://example.com">Example</a></p>
        <pre><code>Code block example</code></pre>
    </div>
"""
richTextState.setHtml(complexHtml)
```

## Exporting to HTML

To convert `RichTextState` to HTML, use the `toHtml` method:

```kotlin
val html = richTextState.toHtml()
println(html) // Outputs formatted HTML string
```

## Supported HTML Tags

The following HTML tags are supported:

### Text Formatting
- `<b>`, `<strong>` - Bold text
- `<i>`, `<em>` - Italic text
- `<u>` - Underlined text
- `<s>`, `<del>` - Strikethrough text
- `<code>` - Code spans

### Structure
- `<p>` - Paragraphs
- `<div>` - Divisions
- `<br>` - Line breaks
- `<h1>`..`<h6>` - Headings (see [Headings](headings.md))
- `<ul>` - Unordered lists
- `<ol>` - Ordered lists
- `<li>` - List items

### Links
- `<a href="...">` - Hyperlinks

### Rich Content
- `<img src="..." width="..." height="..." alt="...">` - Inline images (see [Images](images.md))
- `<span data-trigger-id="..." data-token-id="...">` - Mention/hashtag/command tokens (see [Mentions & Triggers](mentions_and_triggers.md))

## Line breaks and empty blocks

`<br>` and empty blocks follow HTML semantics on both import and export:

- `<br>` inside a block is a soft line break within one paragraph. `<p>a<br>b</p>` imports as one paragraph broken over two lines and exports as-is.
- An empty block is written with its own tag and a `<br>` child, so it keeps its type and style: an empty paragraph exports as `<p><br></p>`, an empty heading as `<h1><br></h1>`, an empty list item as `<li><br></li>`.
- A bare `<br>` between blocks counts as exactly one empty paragraph on import: `<p>a</p><br>` imports as `a` followed by one empty paragraph.
- A single trailing `<br>` inside a non-empty block adds nothing on import: `<p>a<br></p>` imports as just `a`, the way a browser renders it.

The one exception is a document that is a single empty, unstyled paragraph, which exports as `<p></p>`.

HTML saved by earlier versions of the library, which wrote empty paragraphs as bare `<br>` between blocks, imports correctly under these rules and is written in the new form on the next export.

## Notes

- Unsupported HTML tags will be ignored during import
- Nested lists are supported
- Custom styles (using style attribute) are not currently supported
- The HTML output is clean and properly formatted
- Register triggers **before** calling `setHtml` with content that contains tokens, otherwise tokens fall back to plain text
