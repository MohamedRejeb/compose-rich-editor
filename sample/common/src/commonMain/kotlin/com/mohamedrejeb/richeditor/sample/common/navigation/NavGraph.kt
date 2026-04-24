package com.mohamedrejeb.richeditor.sample.common.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohamedrejeb.richeditor.sample.common.examples.RealExamplesScreen
import com.mohamedrejeb.richeditor.sample.common.home.HomeScreen
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorContent
import com.mohamedrejeb.richeditor.sample.common.images.ImagesSampleScreen
import com.mohamedrejeb.richeditor.sample.common.links.LinksSampleScreen
import com.mohamedrejeb.richeditor.sample.common.listsconfig.ListsConfigSampleScreen
import com.mohamedrejeb.richeditor.sample.common.markdowneditor.MarkdownEditorContent
import com.mohamedrejeb.richeditor.sample.common.mentions.MentionsSampleScreen
import com.mohamedrejeb.richeditor.sample.common.richeditor.RichEditorScreen
import com.mohamedrejeb.richeditor.sample.common.slack.SlackDemoScreen
import com.mohamedrejeb.richeditor.sample.common.undoredo.UndoRedoSampleScreen

private const val HOME_ROUTE = "home"
private const val RICH_EDITOR_ROUTE = "richEditor"
private const val HTML_EDITOR_ROUTE = "htmlEditor"
private const val MARKDOWN_EDITOR_ROUTE = "markdownEditor"
private const val SLACK_ROUTE = "slack"
private const val MENTIONS_ROUTE = "mentions"
private const val UNDO_REDO_ROUTE = "undoRedo"
private const val LISTS_CONFIG_ROUTE = "listsConfig"
private const val REAL_EXAMPLES_ROUTE = "realExamples"
private const val LINKS_ROUTE = "links"
private const val IMAGES_ROUTE = "images"

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
                navigateToUndoRedo = { navController.navigate(UNDO_REDO_ROUTE) },
                navigateToListsConfig = { navController.navigate(LISTS_CONFIG_ROUTE) },
                navigateToRealExamples = { navController.navigate(REAL_EXAMPLES_ROUTE) },
                navigateToLinks = { navController.navigate(LINKS_ROUTE) },
                navigateToImages = { navController.navigate(IMAGES_ROUTE) },
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

        composable(UNDO_REDO_ROUTE) {
            UndoRedoSampleScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(LISTS_CONFIG_ROUTE) {
            ListsConfigSampleScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(REAL_EXAMPLES_ROUTE) {
            RealExamplesScreen(
                navigateBack = { navController.popBackStack() },
                navigateToSlack = { navController.navigate(SLACK_ROUTE) },
            )
        }

        composable(LINKS_ROUTE) {
            LinksSampleScreen(
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(IMAGES_ROUTE) {
            ImagesSampleScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
    }
}
