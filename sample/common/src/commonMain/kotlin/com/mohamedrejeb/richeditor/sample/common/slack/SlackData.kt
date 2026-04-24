package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState

internal data class SlackUser(
    val id: String,
    val name: String,
    val handle: String,
    val title: String,
)

internal data class SlackChannel(
    val id: String,
    val name: String,
    val topic: String,
    val memberCount: Int,
)

internal data class SlackReaction(
    val emoji: String,
    val count: Int,
    val reactedByMe: Boolean = false,
)

internal data class ThreadHint(
    val replies: Int,
    val lastReply: String,
    val avatars: List<SlackUser>,
)

internal data class SlackMessage(
    val author: SlackUser,
    val timestamp: String,
    val body: RichTextState,
    val reactions: List<SlackReaction> = emptyList(),
    val thread: ThreadHint? = null,
    val edited: Boolean = false,
    val pending: Boolean = false,
)

internal val currentUser = SlackUser(
    id = "u-mohamed",
    name = "Mohamed Rejeb",
    handle = "@mohamed",
    title = "Library maintainer",
)

internal val slackWorkspaceUsers = listOf(
    currentUser,
    SlackUser("u-alice", "Alice Johnson", "@alice", "Android engineer"),
    SlackUser("u-bob", "Bob Smith", "@bob", "Desktop engineer"),
    SlackUser("u-carol", "Carol Diaz", "@carol", "Product designer"),
    SlackUser("u-david", "David Lee", "@david", "Technical writer"),
    SlackUser("u-elena", "Elena Park", "@elena", "Community lead"),
)

internal val slackChannels = listOf(
    SlackChannel("c-compose-rich-text-editor", "compose-rich-text-editor",
        "Building a WYSIWYG rich text editor for Jetpack Compose and CMP.", 412),
    SlackChannel("c-general", "general", "Company-wide announcements and work-based matters.", 1240),
    SlackChannel("c-kmp", "kotlin-multiplatform", "Kotlin Multiplatform discussions and RFCs.", 680),
    SlackChannel("c-android", "android", "Android platform chat.", 520),
    SlackChannel("c-design", "design", "Product and brand design critique.", 145),
    SlackChannel("c-random", "random", "Watercooler talk. Anything goes (sort of).", 980),
)

internal val currentChannel = slackChannels.first()

/**
 * Stable color pick for a user's avatar, derived from their id hash so the
 * same handle always renders the same color across the demo.
 */
internal fun SlackUser.avatarColor(): Color {
    val palette = SlackColors.AvatarPalette
    val idx = ((id.hashCode() % palette.size) + palette.size) % palette.size
    return palette[idx]
}

/**
 * Hand-written seed messages for the channel. Each uses its own remembered
 * `RichTextState` so the `RichText` renderer has a real state to read from
 * (the library displays from state, not from raw HTML/Markdown).
 */
@Composable
internal fun rememberSeededMessages(): List<SlackMessage> {
    val alice = slackWorkspaceUsers.first { it.id == "u-alice" }
    val bob = slackWorkspaceUsers.first { it.id == "u-bob" }
    val carol = slackWorkspaceUsers.first { it.id == "u-carol" }
    val david = slackWorkspaceUsers.first { it.id == "u-david" }
    val elena = slackWorkspaceUsers.first { it.id == "u-elena" }

    val welcome = rememberSeeded(
        markdown = "Hey team - shipping the **0.30** release of Compose Rich Editor today. " +
            "The highlight is the new [Notion-style slash menu](https://github.com/MohamedRejeb/compose-rich-editor) " +
            "plus the brand-new `headingStyle` paragraph field.",
    )
    val question = rememberSeeded(
        markdown = "Quick question - what's the cleanest way to toggle H1-H3 on selection? " +
            "Right now I'm calling `state.setHeadingStyle(HeadingStyle.H1)` and it feels right but " +
            "just making sure I'm not missing a dedicated toggle helper.",
    )
    val answer = rememberSeeded(
        markdown = "@mohamed @alice that's the supported path. `setHeadingStyle` replaces the current " +
            "heading on the selected paragraphs, and passing `HeadingStyle.Normal` clears it. " +
            "No separate toggle - that would just be sugar around the same call.",
    )
    val links = rememberSeeded(
        markdown = "Dropped the new docs page here: [compose-rich-text-editor.com/headings]" +
            "(https://compose-rich-text-editor.com/headings). " +
            "Covers the full round-trip story through HTML and Markdown.\n\n" +
            "Happy to take notes/feedback.",
    )
    val list = rememberSeeded(
        markdown = "Things I'd like to see in 0.31:\n" +
            "- Block-level quote with the left bar\n" +
            "- Real horizontal rule (not a unicode line)\n" +
            "- Checklist paragraphs\n" +
            "- Image block with caption\n",
    )
    val reactionMagnet = rememberSeeded(
        markdown = "Just tried the slash menu on mobile - buttery. :fire: Loving this roadmap, team.",
    )

    return listOf(
        SlackMessage(
            author = currentUser,
            timestamp = "Today at 9:14 AM",
            body = welcome,
            reactions = listOf(
                SlackReaction("🎉", 7, reactedByMe = true),
                SlackReaction("🚀", 4),
                SlackReaction("❤️", 2),
            ),
            thread = ThreadHint(
                replies = 12,
                lastReply = "Today at 10:24 AM",
                avatars = listOf(alice, bob, carol),
            ),
        ),
        SlackMessage(
            author = alice,
            timestamp = "Today at 10:02 AM",
            body = question,
        ),
        SlackMessage(
            author = bob,
            timestamp = "Today at 10:08 AM",
            body = answer,
            edited = true,
        ),
        SlackMessage(
            author = carol,
            timestamp = "Today at 10:17 AM",
            body = links,
            reactions = listOf(
                SlackReaction("👀", 3),
                SlackReaction("📝", 1, reactedByMe = true),
            ),
        ),
        SlackMessage(
            author = david,
            timestamp = "Today at 10:33 AM",
            body = list,
            thread = ThreadHint(
                replies = 5,
                lastReply = "Today at 11:02 AM",
                avatars = listOf(elena, currentUser),
            ),
        ),
        SlackMessage(
            author = elena,
            timestamp = "Today at 11:05 AM",
            body = reactionMagnet,
            reactions = listOf(
                SlackReaction("🔥", 6, reactedByMe = true),
                SlackReaction("💯", 3),
                SlackReaction("🙌", 2),
            ),
        ),
    )
}

@Composable
private fun rememberSeeded(
    markdown: String? = null,
    html: String? = null,
): RichTextState {
    val state = rememberRichTextState()
    LaunchedEffect(state) {
        if (state.annotatedString.text.isEmpty()) {
            when {
                markdown != null -> state.setMarkdown(markdown)
                html != null -> state.setHtml(html)
            }
        }
    }
    return state
}
