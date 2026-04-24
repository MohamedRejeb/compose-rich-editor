package com.mohamedrejeb.richeditor.ui

/**
 * Controls whether a rich-text editor intercepts the platform's undo/redo keyboard
 * shortcuts (`Ctrl/Cmd+Z`, `Ctrl/Cmd+Shift+Z`).
 *
 * - [Enabled] - default. Shortcuts route through `state.history` which rewinds the
 *   full rich-text model.
 * - [Disabled] - shortcuts fall through to `BasicTextField`'s native behavior. You
 *   can still call `state.history.undo()` / `redo()` directly.
 */
public enum class UndoBehavior {
    Enabled,
    Disabled,
}
