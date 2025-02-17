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
- `<ul>` - Unordered lists
- `<ol>` - Ordered lists
- `<li>` - List items

### Links
- `<a href="...">` - Hyperlinks

## Notes

- Unsupported HTML tags will be ignored during import
- Nested lists are supported
- Custom styles (using style attribute) are not currently supported
- The HTML output is clean and properly formatted
