<h1 align="center">Compose Rich Editor</h1><br>

<a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
<a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
<a href="https://github.com/MohamedRejeb"><img alt="Profile" src="https://raw.githubusercontent.com/MohamedRejeb/MohamedRejeb/main/badges/mohamedrejeb.svg"/></a>
<a href="https://search.maven.org/search?q=g:%22com.mohamedrejeb.richeditor%22%20AND%20a:%22richeditor-compose%22"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.mohamedrejeb.richeditor/richeditor-compose"/></a>

![Compose Rich Editor](docs/images/logo-large-light.svg)

A rich text editor library for both Jetpack Compose and Compose Multiplatform, fully customizable and supports the common rich text editor features

- **Multiplatform**: Compose Rich Editor supports Compose Multiplatform (Android, iOS, Desktop, Web).
- **Easy to use**: Compose Rich Editor's API leverages Kotlin's language features for simplicity and minimal boilerplate.
- **WYSIWYG**: Compose Rich Editor is a WYSIWYG editor that supports the most common text styling features.

## Download

[![Maven Central](https://img.shields.io/maven-central/v/com.mohamedrejeb.richeditor/richeditor-compose)](https://search.maven.org/search?q=g:%22com.mohamedrejeb.richeditor%22%20AND%20a:%22richeditor-compose%22)

Compose Rich Editor is available on `mavenCentral()`.

```kotlin
implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-beta05")
```

## Quick Start

#### RichTextState

Use `RichTextEditor` composable to create a rich text editor.

The `RichTextEditor` composable requires a `RichTextState` to manage the editor's state.

To create a `RichTextState`, use the `rememberRichTextState` function:

```kotlin
val state = rememberRichTextState()

RichTextEditor(
    state = state,
)
```

#### Styling Spans

To style spans, `RichTextState` provides `toggleSpanStyle` method:

```kotlin
// Toggle a span style.
richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
```

To get the current span style of the selection, use `RichTextState.currentSpanStyle`:

```kotlin
// Get the current span style.
val currentSpanStyle = richTextState.currentSpanStyle
val isBold = currentSpanStyle.fontWeight = FontWeight.Bold
```

#### Styling Paragraphs

To style paragraphs, `RichTextState` provides `toggleParagraphStyle` method:

```kotlin
// Toggle a paragraph style.
richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
```

To get the current paragraph style of the selection, use `RichTextState.currentParagraphStyle`:

```kotlin
// Get the current paragraph style.
val currentParagraphStyle = richTextState.currentParagraphStyle
val isCentered = currentParagraphStyle.textAlign = TextAlign.Center
```

#### Add links

To add links, `RichTextState` provides `addLink` method:

```kotlin
// Add link after selection.
richTextState.addLink(
    text = "Compose Rich Editor",
    url = "https://github.com/MohamedRejeb/Compose-Rich-Editor"
)
```

To get if the current selection is a link, use `RichTextState.isLink`:

```kotlin
// Get if the current selection is a link.
val isLink = richTextState.isLink
```

#### Add Code Blocks

To add code blocks, `RichTextState` provides `toggleCodeSpan` method:

```kotlin
// Toggle code span.
richTextState.toggleCodeSpan()
```

To get if the current selection is a code block, use `RichTextState.isCodeSpan`:

```kotlin
// Get if the current selection is a code span.
val isCodeSpan = richTextState.isCodeSpan
```

#### Ordered and Unordered Lists

You can add ordered and unordered lists using `RichTextState`:

```kotlin
// Toggle ordered list.
richTextState.toggleOrderedList()

// Toggle unordered list.
richTextState.toggleUnorderedList()
```

You can get if the current selection is an ordered or unordered list, using `RichTextState`:

```kotlin
// Get if the current selection is an ordered list.
val isOrderedList = richTextState.isOrderedList

// Get if the current selection is an unordered list.
val isUnorderedList = richTextState.isUnorderedList
```

#### Customizing the rich text configuration

Some of the rich text editor's features can be customized, such as the color of the links and the code blocks.

```kotlin
richTextState.setConfig(
    linkColor = Color.Blue,
    linkTextDecoration = TextDecoration.Underline,
    codeColor = Color.Yellow,
    codeBackgroundColor = Color.Transparent,
    codeStrokeColor = Color.LightGray,
)
```

#### HTML import and export

To convert HTML to `RichTextState`, use `RichTextState.setHtml` method:

```kotlin
val html = "<p><b>Compose Rich Editor</b></p>"
richTextState.setHtml(html)
```

To convert `RichTextState` to HTML, use `RichTextState.toHtml` method:

```kotlin
val html = richTextState.toHtml()
```

#### Markdown import and export

To convert Markdown to `RichTextState`, use `RichTextState.setMarkdown` method:

```kotlin
val markdown = "**Compose** *Rich* Editor"
richTextState.setMarkdown(markdown)
```

To convert `RichTextState` to Markdown, use `RichTextState.toMarkdown` method:

```kotlin
val markdown = richTextState.toMarkdown()
```

Check out Compose Rich Editor's [full documentation](https://mohamedrejeb.github.io/Compose-Rich-Editor/) for more details.

## Web live demo
You can try out the web demo [here](https://compose-richeditor.netlify.app/).

## Contribution
If you've found an error in this sample, please file an issue. <br>
Feel free to help out by sending a pull request :heart:.

[Code of Conduct](https://github.com/MohamedRejeb/Compose-Rich-Editor/blob/main/CODE_OF_CONDUCT.md)

## Find this library useful? :heart:
Support it by joining __[stargazers](https://github.com/MohamedRejeb/Compose-Rich-Editor/stargazers)__ for this repository. :star: <br>
Also, __[follow me](https://github.com/MohamedRejeb)__ on GitHub for more libraries! 🤩

You can always <a href="https://www.buymeacoffee.com/MohamedRejeb" target="_blank"><img src="https://img.buymeacoffee.com/button-api/?text=Buy me a coffee&emoji=&slug=MohamedRejeb&button_colour=FFDD00&font_colour=000000&font_family=Cookie&outline_colour=000000&coffee_colour=ffffff"></a>

# License
```markdown
Copyright 2023 Mohamed Rejeb

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
