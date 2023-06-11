<h1 align="center">Compose Rich Editor</h1><br>
<p align="center">
:richeditor-compose: Rich text editor library for both Jetpack Compose and Compose Multiplatform, fully customizable and supports the common rich text editor features.
</p>
<br>
<p align="center">
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://github.com/MohamedRejeb"><img alt="Profile" src="https://raw.githubusercontent.com/MohamedRejeb/MohamedRejeb/main/badges/mohamedrejeb.svg"/></a>
  <a href="https://search.maven.org/search?q=g:%22com.mohamedrejeb.richeditor%22%20AND%20a:%22richeditor-compose%22"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.mohamedrejeb.richeditor/richeditor-compose"/></a>
</p> <br>

<p align="center">
<img src="https://user-images.githubusercontent.com/41842296/235645097-6ddd1d96-0777-40dc-a18f-730e913b6461.png" alt="Compose Rich Text Editor"/>
<br>
<br>
<img src="https://user-images.githubusercontent.com/41842296/235653455-2e4998c1-a24f-40c6-9709-77f23e027b8a.png" alt="Compose Rich Text Editor"/>
</p>

## Why Compose Rich Editor?
Compose Rich Editor is a rich text editor library for both Jetpack Compose and Compose Multiplatform, fully customizable and supports the common rich text editor features. It's built on top of `TextField` and it's going to help you to create a rich text editor easily. <br>

## Including in your project
[![Maven Central](https://img.shields.io/maven-central/v/com.mohamedrejeb.richeditor/richeditor-compose)](https://search.maven.org/search?q=g:%22com.mohamedrejeb.richeditor%22%20AND%20a:%22richeditor-compose%22)

### Gradle
Add the dependency below to your **module**'s `build.gradle.kts` or `build.gradle` file:

```gradle
dependencies {
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:$version")
}
```

## How to Use
Compose Rich Editor supports both Jetpack Compose and Compose Multiplatform projects,.

### Create Rich Text Editor with Compose UI
We can easily use Compose Rich Editor by calling the `RichTextEditor` Composable and pass a `RichTextValue`.

```kotlin
var richTextValue by remember { mutableStateOf(RichTextValue()) }

RichTextEditor(
    value = richTextValue,
    onValueChange = {
        richTextValue = it
    },
)
```
> **Note**: You may notice that it's similar to `TextField`, it's because `RichTextEditor`is built on top of `TextField` and it's available with 5 composables:
> - `BasicRichTextEditor`
> - `RichTextEditor` (material2)
> - `OutlinedRichTextEditor`  (material2)
> - `RichTextEditor` (material3)
> - `OutlinedRichTextEditor`  (material3)

> All `RichTextEditor` composables are fully customisable with same parameters that are available for a normal `TextField`
> - `TextFieldColors`
> - `Shape`
> - `enabled`
> - `keyboardOptions`
> - `keyboardActions`
> - ...

### Update Rich Text Styles
We have some available methods under `RichTextValue` to update styles. If we use `addStyle` method, we add a style. If we use `removeStyle` method, we remove a style. Also, we can toggle a style using `toggleStyle` method and all of these methods accepts a `RichTextStyle` as a parameter. <br>

```kotlin
var richTextValue by remember { mutableStateOf(RichTextValue()) }

IconButton(
    onClick = {
        richTextValue = richTextValue.toggleStyle(RichTextStyle.Bold)
    }
) {
    Icon(
        imageVector = Icons.Outlined.FormatBold,
        contentDescription = "Bold"
    )
}
```

The added styles are going to be applied to the written text in the `RichTextEditor`. Also, you can get the current styles using `richTextValue.currentStyles`, you may need it to check if a certain style is added. <br>

```kotlin
var richTextValue by remember { mutableStateOf(RichTextValue()) }

IconButton(
    onClick = {
        richTextValue = richTextValue.toggleStyle(RichTextStyle.Bold)
    }
) {
    Icon(
        imageVector = Icons.Outlined.FormatBold,
        contentDescription = "Bold",
        modifier = Modifier
            // Mark the icon with a background color is the style is selected
            .background(
                color = if (richTextValue.currentStyles.contains(RichTextStyle.Bold)) {
                    Color.Blue
                } else {
                    Color.Transparent
                }
            )
    )
}
```

> **Note**: You can add and remove the styles easily, so you can build your own custom styles panel. Take a look on the sample to know more about creating your own styles panel.

### Available Rich Text Styles
There are some available styles that you can use with `RichTextEditor` and you can create your own custom styles: <br>

#### Bold
We can add bold style to the text using `RichTextStyle.Bold` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.Bold)
```

#### Italic
We can add italic style to the text using `RichTextStyle.Italic` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.Italic)
```

#### Underline
We can add underline style to the text using `RichTextStyle.Underline` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.Underline)
```

#### Strikethrough
We can add strikethrough style to the text using `RichTextStyle.Strikethrough` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.Strikethrough)
```

#### TextColor
We can add text color style to the text using `RichTextStyle.TextColor` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.TextColor(Color.Red))
```

#### BackgroundColor
We can add background color style to the text using `RichTextStyle.BackgroundColor` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.BackgroundColor(Color.Red))
```

#### FontSize
We can add font size style to the text using `RichTextStyle.FontSize` style.

```kotlin
richTextValue = richTextValue.addStyle(RichTextStyle.FontSize(20.sp))
```

#### Create a custom style
We can create a custom style with implementing `RichTextStyle` interface.

```kotlin
data class FirstCustomStyle(
    val color: Color, 
    val background: Color
) : RichTextStyle {

    override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
        return SpanStyle(
            color = color, 
            background = background
        )
    }
}

richTextValue = richTextValue.addStyle(FirstCustomStyle(Color.Red, Color.Blue))

object SecondCustomStyle : RichTextStyle {
    override fun applyStyle(spanStyle: SpanStyle): SpanStyle {
        return SpanStyle(
            color = Color.White,
            background = Color.Blue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        )
    }
}

richTextValue = richTextValue.addStyle(SecondCustomStyle)
```

### Create `RichTextValue` from HTML String
We can create a `RichTextValue` from HTML string using `RichTextValue.fromHtml` factory method.

```kotlin
val html = "<b>Hello</b> <i>World</i>"
val richTextValue = RichTextValue.from(html)
```

### Export `RichTextValue` to HTML String
We can export a `RichTextValue` to HTML string using `RichTextValue.toHtml` method.

```kotlin
val richTextValue = RichTextValue()
val html = richTextValue.toHtml()
```

### Create Rich Text with Compose UI
The library provides a `RichText` composable that can be used to display rich text. It's similar to `Text` composable, but it supports rich text styles.

```kotlin
var richTextValue by remember { mutableStateOf(RichTextValue()) }

RichText(
    richText = richTextValue
)
```

## Supported Features
There are some supported features that you can use with `RichTextEditor`:
- [x] Bold
- [x] Italic
- [x] Underline
- [x] Strikethrough
- [x] Text color
- [x] Background color
- [x] Font size
- [x] Create a custom style
- [X] Add unordered lists
- [X] Support importing and exporting HTML

## Coming Features
The library still in its early stages, so there are some features that are coming soon:

- [ ] Add link
- [ ] Add paragraph alignment (left, center, right)
- [ ] Add ordered lists
- [ ] Add Blockquote
- [ ] Add code block style
- [ ] Add undo and redo
- [ ] Add checkbox
- [ ] Add image support
- [ ] Add video support
- [ ] Add audio support
- [ ] Support importing and exporting Markdown
- [ ] Add add prebuilt styles panel

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
