package com.mohamedrejeb.richeditor.ui

internal const val PASTE_TAG = "RichPaste"

/** Thin expect/actual so Android uses Log.d and other platforms use println. */
internal expect fun pasteLog(tag: String, msg: String)
