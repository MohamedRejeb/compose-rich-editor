# Markdown Import and Export

The Rich Text Editor supports converting between Markdown and rich text content. This allows you to:
- Save editor content as Markdown
- Load content from Markdown files
- Integrate with Markdown-based systems
- Support platforms like GitHub that use Markdown

## Importing Markdown

To convert Markdown to `RichTextState`, use the `setMarkdown` method:

```kotlin
// Basic formatting
val simpleMarkdown = """
    **Bold** and *italic* text with __underline__
""".trimIndent()
richTextState.setMarkdown(simpleMarkdown)

// Complex structure
val complexMarkdown = """
    # Heading 1
    ## Heading 2

    Paragraph with **bold** and *italic* text.

    * Unordered list item 1
    * Unordered list item 2

    1. Ordered list item 1
    2. Ordered list item 2

    [Link to Example](https://example.com)

    `Code span example`
""".trimIndent()
richTextState.setMarkdown(complexMarkdown)
```

## Exporting to Markdown

To convert `RichTextState` to Markdown, use the `toMarkdown` method:

```kotlin
val markdown = richTextState.toMarkdown()
println(markdown) // Outputs formatted Markdown string
```

## Supported Markdown Syntax

The following Markdown syntax elements are supported:

### Text Formatting
- `**text**` or `__text__` - Bold text
- `*text*` or `_text_` - Italic text
- `~~text~~` - Strikethrough text
- `` `code` `` - Code spans

### Headings
- `# Title` through `###### Title` - ATX headings H1..H6 (see [Headings](headings.md))

### Lists
- `* item` or `- item` - Unordered list items
- `1. item` - Ordered list items
- Nested lists with proper indentation

### Links
- `[text](url)` - Hyperlinks

### Rich Content
- `![alt](url)` - Inline images (see [Images](images.md))
- `[label](trigger:<triggerId>:<tokenId>)` - Mention/hashtag/command tokens (see [Mentions & Triggers](mentions_and_triggers.md))

## Notes

- Unsupported Markdown syntax will be preserved as plain text
- Nested lists are supported with proper indentation
- The Markdown output is clean and properly formatted
- Markdown has no native width/height syntax for images, so explicit dimensions are lost in a Markdown round-trip. Use HTML if you need to preserve them
- Tables are planned for future releases
