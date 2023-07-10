package com.mohamedrejeb.richeditor.utils

internal actual fun openUrl(url: String?) {
    url?.let { window.open(it) }
}