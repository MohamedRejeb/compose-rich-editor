package com.mohamedrejeb.richeditor.sample.common.github

internal data class GitHubUser(
    val id: String,
    val handle: String,
    val displayName: String,
    val avatarColor: Long,
)

internal data class GitHubIssueRef(
    val id: String,
    val number: Int,
    val title: String,
)

internal data class GitHubComment(
    val author: GitHubUser,
    val timeAgo: String,
    val html: String,
)

internal val sampleUsers: List<GitHubUser> = listOf(
    GitHubUser("u-mohamed", "@mohamedrejeb", "Mohamed Rejeb", 0xFF8957E5),
    GitHubUser("u-alice", "@alice", "Alice Johnson", 0xFFD2A8FF),
    GitHubUser("u-bob", "@bob", "Bob Smith", 0xFFA5D6FF),
    GitHubUser("u-carol", "@carol", "Carol Diaz", 0xFFFFA657),
    GitHubUser("u-david", "@david", "David Lee", 0xFF7EE787),
)

internal val sampleIssueRefs: List<GitHubIssueRef> = listOf(
    GitHubIssueRef("issue-423", 423, "Inline images overflow container width"),
    GitHubIssueRef("issue-593", 593, "Markdown underline round-trip"),
    GitHubIssueRef("issue-404", 404, "Crash when MaxLines hit on ordered list"),
    GitHubIssueRef("issue-512", 512, "Add ListPrefixAlignment.Start"),
    GitHubIssueRef("pr-617", 617, "feat: add ImageLoader extensibility"),
)

internal const val SAMPLE_ISSUE_NUMBER = 624
internal const val SAMPLE_ISSUE_TITLE = "Add Compose Rich Editor demo for GitHub"
internal val sampleIssueAuthor: GitHubUser = sampleUsers[1]

/** Repository path used when opening sample issue URLs. Purely cosmetic - these are not real issues. */
internal const val GITHUB_SAMPLE_REPO_URL: String = "MohamedRejeb/compose-richeditor"

internal val sampleIssueBodyHtml: String = """
<p>The Real Examples hub still lists GitHub as <i>Coming Soon</i>. Let's promote it
by shipping a working composer that mirrors the issue UX, including
<code>code spans</code>, fenced blocks and references to other issues like
<a href="https://github.com/MohamedRejeb/Compose-Rich-Editor/issues/423">#423</a>.</p>
<p>Acceptance:</p>
<ul>
    <li>Threaded comment list rendered through <b>RichText</b>.</li>
    <li>Composer with the standard GitHub toolbar.</li>
    <li><code>@</code> mentions and <code>#</code> issue references with suggestion popups.</li>
    <li>Submitting a comment appends to the thread.</li>
</ul>
""".trimIndent()

internal val sampleComments: List<GitHubComment> = listOf(
    GitHubComment(
        author = sampleUsers[2],
        timeAgo = "5 hours ago",
        html = """
        <p>Strong +1 from me. The library can already render every piece needed,
        we just need to compose them in one screen. I'd reuse the trigger system
        from <code>MentionsSampleScreen</code> for both <code>@</code> and
        <code>#</code> popups.</p>
        """.trimIndent(),
    ),
    GitHubComment(
        author = sampleUsers[0],
        timeAgo = "2 hours ago",
        html = """
        <p>Drafted the composer, going with GitHub Dark for the visual identity.
        Code blocks render via <code>toggleCode()</code>, code spans via
        <code>toggleCodeSpan()</code>. Linking
        <a href="https://github.com/MohamedRejeb/Compose-Rich-Editor/issues/593">#593</a>
        for context on markdown round-trips.</p>
        <ol>
            <li>Composer toolbar wired up.</li>
            <li>Trigger popups for users and refs.</li>
            <li>Submitted comments append to the thread.</li>
        </ol>
        """.trimIndent(),
    ),
)
