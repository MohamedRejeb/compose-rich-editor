package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.ui.graphics.Color

/**
 * Slack dark-mode palette. Values sampled from the Slack desktop client so the
 * demo reads as "Slack" rather than a generic chat app. Kept in one place so
 * the screen, composer, messages, and panel stop hardcoding hex values.
 */
internal object SlackColors {
    val Background = Color(0xFF1A1D21)
    val Surface = Color(0xFF222529)
    val SurfaceElevated = Color(0xFF27292D)
    val SurfaceHover = Color(0xFF2C2E33)
    val SidebarSurface = Color(0xFF19171D)

    val Border = Color(0xFF393B3D)
    val BorderStrong = Color(0xFF4A4D50)
    val Divider = Color(0xFF3D3E40)

    val TextPrimary = Color(0xFFD1D2D3)
    val TextStrong = Color(0xFFE8E8E8)
    val TextSecondary = Color(0xFFABABAD)
    val TextMuted = Color(0xFF868686)
    val TextPlaceholder = Color(0xFF868686)

    val Aubergine = Color(0xFF4A154B)
    val AccentBlue = Color(0xFF1D9BD1)
    val AccentYellow = Color(0xFFECB22E)
    val AccentGreen = Color(0xFF007A5A)
    val AccentGreenHover = Color(0xFF148567)
    val AccentRed = Color(0xFFE01E5A)

    val LinkBlue = Color(0xFF1D9BD1)
    val ChannelBlue = Color(0xFF1D9BD1)
    val MentionYellow = Color(0xFFECB22E)

    val CodeSpan = Color(0xFFD7882D)
    val CodeSpanStroke = Color(0xFF494B4D)

    val ReactionPill = Color(0xFF222529)
    val ReactionPillSelected = Color(0x261D9BD1)
    val ReactionStroke = Color(0xFF3D3E40)
    val ReactionStrokeSelected = Color(0xFF1D9BD1)

    val ThreadHint = Color(0xFF1D9BD1)
    val UnreadDividerLine = Color(0x66E01E5A)
    val UnreadDividerText = Color(0xFFE01E5A)

    /** Fixed palette for colored-initial avatars, cycled by user hash. */
    val AvatarPalette = listOf(
        Color(0xFF4A154B), // aubergine
        Color(0xFF1264A3), // blue
        Color(0xFF2EB67D), // green
        Color(0xFFECB22E), // yellow
        Color(0xFFE01E5A), // red
        Color(0xFF9B51E0), // violet
        Color(0xFFF2994A), // orange
        Color(0xFF00A3BF), // teal
    )
}
