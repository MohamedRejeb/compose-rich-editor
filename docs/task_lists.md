# Task Lists

A task list is a checklist: every item has a checkbox that is checked or unchecked, like the task lists of GitHub Markdown (`- [ ] todo`, `- [x] done`), Notion and Google Docs. A task list item is a list item of its own kind, next to the [ordered and unordered](ordered_unordered_lists.md) ones, so it nests, indents and reacts to `Enter` and `Backspace` the same way. The user checks an item by tapping its checkbox, and the state is part of the content: it is saved in HTML, Markdown, the document model and JSON.

!!! note

    The task list API is marked `@ExperimentalRichTextApi` and may change in a future release.

## Basic Usage

```kotlin
// Turn the selected paragraphs into task list items, or back into paragraphs.
richTextState.toggleTaskList()

// The one-way versions.
richTextState.addTaskList()
richTextState.removeTaskList()
```

A new item is unchecked. `addTaskList` on a paragraph that is a task list item already keeps its checked state, and converts an ordered or unordered item at the same nesting level.

To read the state at the selection, for a toolbar:

```kotlin
// Every selected paragraph is a task list item.
val isTaskList = richTextState.isTaskList

// Every selected paragraph is a checked task list item.
val isChecked = richTextState.isTaskListItemChecked
```

`richTextState.isList` is true for a task list too, and `canIncreaseListLevel`, `canDecreaseListLevel`, `increaseListLevel()` and `decreaseListLevel()` work on task list items like on the other lists.

## Checking Items

In the editor, a tap or a click on the checkbox of an item checks or unchecks it. From code:

```kotlin
// Check the selected items, or uncheck them when the first one is checked.
richTextState.toggleTaskListItemChecked()

// Set the state of the selected items.
richTextState.setTaskListItemsChecked(true)
```

Both act on every task list item of the selection, the item of the caret when the selection is collapsed, and leave other paragraphs alone. Checking an item does not move the caret or the selection.

A read-only or disabled editor does not toggle on tap, and neither does the read-only `RichText`. The functions above still work on their state, so an app can build its own interaction.

## Parameter Reference

| API | Kind | Description |
|---|---|---|
| `toggleTaskList()` | function | Turns the selected paragraphs into unchecked items, or back into paragraphs when the first one is an item |
| `addTaskList()` | function | Turns the selected paragraphs into items |
| `removeTaskList()` | function | Turns the selected items back into paragraphs |
| `toggleTaskListItemChecked()` | function | Checks the selected items, or unchecks them when the first one is checked |
| `setTaskListItemsChecked(checked)` | function | Sets the checked state of the selected items |
| `isTaskList` | state | Whether every selected paragraph is a task list item |
| `isTaskListItemChecked` | state | Whether every selected paragraph is a checked task list item |
| `RichTextFeature.TaskList` | feature | Allows task lists in an editor, see [Editor features](features.md) |
| `RichTextBlockType.TaskItem(checked, indent)` | document block type | A task list item in the [document model](rich_text_document.md) |

## Keyboard and Typing

Task list items follow the [list behavior](ordered_unordered_lists.md#list-behavior) of the other lists:

- `Enter` in an item starts a new item, which is unchecked whatever the state of the item it came from.
- `Enter` on an empty item leaves the list (configurable with `config.exitListOnEmptyItem`).
- `Backspace` at the start of an item lifts a nested item one level and keeps its state, and turns a first-level item into a paragraph.
- `Tab` and `Shift + Tab` indent and outdent.

Typing a box at the start of a paragraph or of an unordered list item turns it into a task list item:

- `[ ] `: an unchecked item
- `[x] ` or `[X] `: a checked item

Because `- ` starts an unordered item first, typing the Markdown form `- [ ] ` ends up as a task list item too. These shortcuts follow `config.listTypingShortcutsEnabled`, like the other list shortcuts.

## Undo

Turning paragraphs into task list items is one undo step, and so is checking or unchecking, whether by a tap or from code.

## Appearance

The checkbox is a character in the text, `☐` for an unchecked item and `☑` for a checked one, in the place where an unordered item has its bullet. It follows the marker settings of the other lists:

```kotlin
// The style of the markers of every list, the checkboxes included.
richTextState.config.listMarkerStyle = SpanStyle(color = Color.Gray)

// Whether a marker inherits the typography of the item's text.
richTextState.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.InheritFromText

// Task list items are indented like unordered items.
richTextState.config.unorderedListIndent = 38
```

## HTML

A task list exports as the HTML that GitHub renders for one:

```html
<ul class="contains-task-list">
  <li class="task-list-item"><input type="checkbox" disabled> todo</li>
  <li class="task-list-item"><input type="checkbox" disabled checked> done</li>
</ul>
```

On import, a list item that starts with an `<input type="checkbox">` is a task list item, checked when the input has the `checked` attribute. The classes and the `disabled` attribute are not required, so the bare form loads too:

```html
<ul>
  <li><input type="checkbox" checked>done</li>
</ul>
```

A list can mix task and plain items. They share one `<ul>`, and only the task items carry the class and the checkbox.

## Markdown

A task list exports and imports as GitHub Flavored Markdown task list items:

```markdown
- [ ] todo
- [x] done
    - [ ] nested
- a plain item in the same list
```

## Document Model and JSON

In the [document model](rich_text_document.md) a task list item is a block of type `RichTextBlockType.TaskItem(checked, indent)`, where `indent` is the 0-based nesting depth:

```kotlin
RichTextBlock(text = "done", type = RichTextBlockType.TaskItem(checked = true))
```

In [JSON](json_import_export.md) it is an unordered `list-item` block with a `checked` field:

```json
{"id": "b0", "type": "list-item", "ordered": false, "indent": 0, "checked": true, "text": "done", "spans": []}
```

A reader that does not know the field loads the block as an unordered list item, so the schema version is unchanged.

## Restricting the Feature

Task lists have their own entry in the [feature set](features.md). Without `RichTextFeature.TaskList`, task list items in loaded or pasted content become plain paragraphs, `addTaskList` and `toggleTaskList` do not create items, and the typed box stays text.

```kotlin
// Everything except task lists.
richTextState.config.features = RichTextFeature.All - RichTextFeature.TaskList
```

## Limitations

- The checkboxes are the characters `☐` (U+2610) and `☑` (U+2611), drawn by the font of the text. A font without these characters falls back to a system font, and where no fallback has them they do not render. They cannot be replaced by other characters or by a custom drawing.
- A tap toggles an item in the editors, except when `singleParagraph` is true.
- A task list item is an unordered item with a checkbox. A numbered task item (`1. [ ] todo`, or a checkbox in an `<ol>` item) loads as a task list item and loses its number.
- The checkbox is not a separate accessibility element: it is read as part of the text of the editor.

## Related

- [Ordered and Unordered Lists](ordered_unordered_lists.md)
- [Editor features](features.md)
- [HTML Import and Export](html_import_export.md)
- [Markdown Import and Export](markdown_import_export.md)
- [JSON Import and Export](json_import_export.md)
