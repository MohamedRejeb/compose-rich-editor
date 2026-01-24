package com.mohamedrejeb.richeditor.ui

import android.content.ComponentName
import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.window.DialogWindowProvider

@Composable
internal actual fun ProvideNoSelectionToolbar(
    disableSelectionToolbar: Boolean,
    content: @Composable () -> Unit,
) {
    if (!disableSelectionToolbar) {
        content()
        return
    }

    val context = LocalContext.current
    val view = LocalView.current

    // Bitno: u Dialogu je drugi Window
    val dialogWindow = remember(view) {
        (view.parent as? DialogWindowProvider)?.window
    }

    // 1) Presijeci Android ActionMode u tom Window-u
    DisposableEffect(dialogWindow) {
        if (dialogWindow == null) return@DisposableEffect onDispose { }

        val original = dialogWindow.callback

        dialogWindow.callback = object : Window.Callback {
            // ---- delegate sve ostalo na original ----
            override fun dispatchKeyEvent(event: android.view.KeyEvent) = original.dispatchKeyEvent(event)
            override fun dispatchKeyShortcutEvent(event: android.view.KeyEvent) = original.dispatchKeyShortcutEvent(event)
            override fun dispatchTouchEvent(event: android.view.MotionEvent) = original.dispatchTouchEvent(event)
            override fun dispatchTrackballEvent(event: android.view.MotionEvent) = original.dispatchTrackballEvent(event)
            override fun dispatchGenericMotionEvent(event: android.view.MotionEvent) = original.dispatchGenericMotionEvent(event)
            override fun dispatchPopulateAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent) =
                original.dispatchPopulateAccessibilityEvent(event)

            override fun onCreatePanelView(featureId: Int): View? = original.onCreatePanelView(featureId)
            override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean = original.onCreatePanelMenu(featureId, menu)
            override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean = original.onPreparePanel(featureId, view, menu)
            override fun onMenuOpened(featureId: Int, menu: Menu): Boolean = original.onMenuOpened(featureId, menu)
            override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean = original.onMenuItemSelected(featureId, item)
            override fun onWindowAttributesChanged(attrs: android.view.WindowManager.LayoutParams) = original.onWindowAttributesChanged(attrs)
            override fun onContentChanged() = original.onContentChanged()
            override fun onWindowFocusChanged(hasFocus: Boolean) = original.onWindowFocusChanged(hasFocus)
            override fun onAttachedToWindow() = original.onAttachedToWindow()
            override fun onDetachedFromWindow() = original.onDetachedFromWindow()
            override fun onPanelClosed(featureId: Int, menu: Menu) = original.onPanelClosed(featureId, menu)
            override fun onSearchRequested(): Boolean = original.onSearchRequested()
            override fun onSearchRequested(searchEvent: android.view.SearchEvent): Boolean = original.onSearchRequested(searchEvent)
            override fun onActionModeStarted(mode: ActionMode) = original.onActionModeStarted(mode)
            override fun onActionModeFinished(mode: ActionMode) = original.onActionModeFinished(mode)

            override fun onWindowStartingActionMode(callback: ActionMode.Callback): ActionMode {
                return NoOpActionMode(context)
            }

            override fun onWindowStartingActionMode(callback: ActionMode.Callback, type: Int): ActionMode? {
                return NoOpActionMode(context)
            }
        }

        onDispose {
            dialogWindow.callback = original
        }
    }

    // 2) Compose TextToolbar (no-op)
    val noToolbar: TextToolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus = TextToolbarStatus.Hidden
            override fun hide() = Unit
            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) = Unit
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides noToolbar) {
        content()
    }
}

/**
 * No-op ActionMode koji vraća prazan Menu da Android nema šta nacrtati.
 * (nema AppCompat zavisnosti)
 */
private class NoOpActionMode(
    context: Context
) : ActionMode() {

    private val inflater = MenuInflater(context)
    private val menu: Menu = EmptyMenu

    override fun setTitle(title: CharSequence?) {}
    override fun setTitle(resId: Int) {}
    override fun setSubtitle(subtitle: CharSequence?) {}
    override fun setSubtitle(resId: Int) {}
    override fun setCustomView(view: View?) {}
    override fun invalidate() {}
    override fun finish() {}

    override fun getMenu(): Menu = menu
    override fun getTitle(): CharSequence? = null
    override fun getSubtitle(): CharSequence? = null
    override fun getCustomView(): View? = null
    override fun getMenuInflater(): MenuInflater = inflater
}

/**
 * Minimalni prazan Menu (implementira sve, ali ništa ne radi).
 * Dovoljno da ActionMode ne pukne i da nema item-a.
 */
private object EmptyMenu : Menu {
    override fun add(title: CharSequence?): MenuItem = EmptyMenuItem
    override fun add(titleRes: Int): MenuItem = EmptyMenuItem
    override fun add(groupId: Int, itemId: Int, order: Int, title: CharSequence?): MenuItem = EmptyMenuItem
    override fun add(groupId: Int, itemId: Int, order: Int, titleRes: Int): MenuItem = EmptyMenuItem

    override fun addSubMenu(title: CharSequence?): SubMenu? = null
    override fun addSubMenu(titleRes: Int): SubMenu? = null
    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, title: CharSequence?): SubMenu? = null
    override fun addSubMenu(groupId: Int, itemId: Int, order: Int, titleRes: Int): SubMenu? = null

    override fun addIntentOptions(
        groupId: Int,
        itemId: Int,
        order: Int,
        caller: ComponentName?,
        specifics: Array<android.content.Intent>?,
        intent: android.content.Intent?,
        flags: Int,
        outSpecificItems: Array<MenuItem>?
    ): Int = 0

    override fun removeItem(id: Int) {}
    override fun removeGroup(groupId: Int) {}
    override fun clear() {}
    override fun setGroupCheckable(group: Int, checkable: Boolean, exclusive: Boolean) {}
    override fun setGroupVisible(group: Int, visible: Boolean) {}
    override fun setGroupEnabled(group: Int, enabled: Boolean) {}
    override fun hasVisibleItems(): Boolean = false
    override fun findItem(id: Int): MenuItem? = null
    override fun size(): Int = 0
    override fun getItem(index: Int): MenuItem = EmptyMenuItem
    override fun close() {}
    override fun performShortcut(keyCode: Int, event: android.view.KeyEvent?, flags: Int): Boolean = false
    override fun isShortcutKey(keyCode: Int, event: android.view.KeyEvent?): Boolean = false
    override fun performIdentifierAction(id: Int, flags: Int): Boolean = false
    override fun setQwertyMode(isQwerty: Boolean) {}
}

private object EmptyMenuItem : MenuItem {
    override fun getItemId(): Int = 0
    override fun getGroupId(): Int = 0
    override fun getOrder(): Int = 0
    override fun setTitle(title: CharSequence?): MenuItem = this
    override fun setTitle(title: Int): MenuItem = this
    override fun getTitle(): CharSequence = ""
    override fun setTitleCondensed(title: CharSequence?): MenuItem = this
    override fun getTitleCondensed(): CharSequence = ""
    override fun setIcon(icon: android.graphics.drawable.Drawable?): MenuItem = this
    override fun setIcon(iconRes: Int): MenuItem = this
    override fun getIcon(): android.graphics.drawable.Drawable? = null
    override fun setIntent(intent: android.content.Intent?): MenuItem = this
    override fun getIntent(): android.content.Intent? = null
    override fun setShortcut(numericChar: Char, alphaChar: Char): MenuItem = this
    override fun setNumericShortcut(numericChar: Char): MenuItem = this
    override fun getNumericShortcut(): Char = 0.toChar()
    override fun setAlphabeticShortcut(alphaChar: Char): MenuItem = this
    override fun setAlphabeticShortcut(alphaChar: Char, alphaModifiers: Int): MenuItem = this
    override fun getAlphabeticShortcut(): Char = 0.toChar()
    override fun setCheckable(checkable: Boolean): MenuItem = this
    override fun isCheckable(): Boolean = false
    override fun setChecked(checked: Boolean): MenuItem = this
    override fun isChecked(): Boolean = false
    override fun setVisible(visible: Boolean): MenuItem = this
    override fun isVisible(): Boolean = false
    override fun setEnabled(enabled: Boolean): MenuItem = this
    override fun isEnabled(): Boolean = false
    override fun hasSubMenu(): Boolean = false
    override fun getSubMenu(): SubMenu? = null
    override fun setOnMenuItemClickListener(menuItemClickListener: MenuItem.OnMenuItemClickListener?): MenuItem = this
    override fun getMenuInfo(): android.view.ContextMenu.ContextMenuInfo? = null
    override fun setShowAsAction(actionEnum: Int) {}
    override fun setShowAsActionFlags(actionEnum: Int): MenuItem = this
    override fun setActionView(view: View?): MenuItem = this
    override fun setActionView(resId: Int): MenuItem = this
    override fun getActionView(): View? = null
    override fun setActionProvider(actionProvider: android.view.ActionProvider?): MenuItem = this
    override fun getActionProvider(): android.view.ActionProvider? = null
    override fun expandActionView(): Boolean = false
    override fun collapseActionView(): Boolean = false
    override fun isActionViewExpanded(): Boolean = false
    override fun setOnActionExpandListener(listener: MenuItem.OnActionExpandListener?): MenuItem = this
    override fun setContentDescription(contentDescription: CharSequence?): MenuItem = this
    override fun getContentDescription(): CharSequence? = null
    override fun setTooltipText(tooltipText: CharSequence?): MenuItem = this
    override fun getTooltipText(): CharSequence? = null
    override fun setIconTintList(tint: android.content.res.ColorStateList?): MenuItem = this
    override fun getIconTintList(): android.content.res.ColorStateList? = null
    override fun setIconTintMode(tintMode: android.graphics.PorterDuff.Mode?): MenuItem = this
    override fun getIconTintMode(): android.graphics.PorterDuff.Mode? = null
    override fun setNumericShortcut(numericChar: Char, numericModifiers: Int): MenuItem = this
    override fun getNumericModifiers(): Int = 0
    override fun getAlphabeticModifiers(): Int = 0
    override fun setShortcut(numericChar: Char, alphaChar: Char, numericModifiers: Int, alphaModifiers: Int): MenuItem = this
}
