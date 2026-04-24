package com.mohamedrejeb.richeditor.sample.common.github

import androidx.compose.ui.graphics.Color

/** GitHub Dark Default palette, sampled from github.com. */
internal object GitHubColors {
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val SurfaceHigh = Color(0xFF1C2128)
    val Border = Color(0xFF30363D)
    val BorderMuted = Color(0xFF21262D)

    val Text = Color(0xFFE6EDF3)
    val TextMuted = Color(0xFF8B949E)
    val TextSubtle = Color(0xFF6E7681)

    val Link = Color(0xFF58A6FF)
    val Mention = Color(0xFF79C0FF)
    val IssueRef = Color(0xFF7EE787)

    // Inline code: solid muted gray block, no stroke, regular text color on top.
    val CodeBackground = Color(0xFF343941)

    val Success = Color(0xFF238636)
    val SuccessHover = Color(0xFF2EA043)

    val OpenPill = Color(0xFF1F6FEB)
    val OpenPillBg = Color(0xFF1F6FEB).copy(alpha = 0.15f)

    val ToolbarHover = Color(0xFF30363D).copy(alpha = 0.6f)
    val ToolbarSelected = Color(0xFF1F6FEB).copy(alpha = 0.25f)
}
