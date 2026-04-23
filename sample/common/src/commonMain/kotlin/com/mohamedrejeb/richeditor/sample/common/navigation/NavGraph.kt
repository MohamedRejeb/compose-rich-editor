package com.mohamedrejeb.richeditor.sample.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohamedrejeb.richeditor.sample.common.home.HomeScreen
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorContent
import com.mohamedrejeb.richeditor.sample.common.markdowneditor.MarkdownEditorContent
import com.mohamedrejeb.richeditor.sample.common.mentions.MentionsSampleScreen
import com.mohamedrejeb.richeditor.sample.common.richeditor.RichEditorScreen
import com.mohamedrejeb.richeditor.sample.common.slack.SlackDemoScreen

private const val HOME_ROUTE = "home"
private const val RICH_EDITOR_ROUTE = "richEditor"
private const val HTML_EDITOR_ROUTE = "htmlEditor"
private const val MARKDOWN_EDITOR_ROUTE = "markdownEditor"
private const val SLACK_ROUTE = "slack"
private const val MENTIONS_ROUTE = "mentions"

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE
    ) {
        composable(HOME_ROUTE) {
            HomeScreen(
                navigateToRichEditor = { navController.navigate(RICH_EDITOR_ROUTE) },
                navigateToHtmlEditor = { navController.navigate(HTML_EDITOR_ROUTE) },
                navigateToMarkdownEditor = { navController.navigate(MARKDOWN_EDITOR_ROUTE) },
                navigateToSlack = { navController.navigate(SLACK_ROUTE) },
                navigateToMentions = { navController.navigate(MENTIONS_ROUTE) },
            )
        }

        composable(RICH_EDITOR_ROUTE) {
            RichEditorScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(HTML_EDITOR_ROUTE) {
            HtmlEditorContent(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(MARKDOWN_EDITOR_ROUTE) {
            MarkdownEditorContent(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(SLACK_ROUTE) {
            SlackDemoScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(MENTIONS_ROUTE) {
            MentionsSampleScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
    }
}