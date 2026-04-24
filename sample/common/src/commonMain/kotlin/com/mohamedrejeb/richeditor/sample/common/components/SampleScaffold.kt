package com.mohamedrejeb.richeditor.sample.common.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.richeditor.sample.common.ui.theme.LocalThemeController

/**
 * Consistent top-level wrapper for every sample feature screen.
 * Provides a standard back button, theme toggle action, optional extra actions, and a scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScaffold(
    title: String,
    navigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val theme = LocalThemeController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (navigateBack != null) {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    actions()
                    IconButton(onClick = theme.toggle) {
                        Icon(
                            imageVector = if (theme.darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (theme.darkTheme) "Switch to light theme" else "Switch to dark theme",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxSize(),
        content = content,
    )
}
