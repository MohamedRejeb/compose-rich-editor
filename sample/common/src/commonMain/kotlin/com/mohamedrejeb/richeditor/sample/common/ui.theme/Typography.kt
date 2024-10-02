package com.mohamedrejeb.richeditor.sample.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_Bold
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_BoldItalic
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_Italic
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_Medium
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_MediumItalic
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_Regular
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_SemiBold
import com.mohamedrejeb.richeditor.common.generated.resources.Raleway_SemiBoldItalic
import com.mohamedrejeb.richeditor.common.generated.resources.Res
import org.jetbrains.compose.resources.Font

val Raleway
    @Composable
    get() = FontFamily(
        listOf(
            Font(
                Res.font.Raleway_Regular,
                weight = FontWeight.Normal,
                style = FontStyle.Normal,
            ),
            Font(
                Res.font.Raleway_Italic,
                weight = FontWeight.Normal,
                style = FontStyle.Italic,
            ),
            Font(
                Res.font.Raleway_Medium,
                weight = FontWeight.Medium,
                style = FontStyle.Normal,
            ),
            Font(
                Res.font.Raleway_MediumItalic,
                weight = FontWeight.Medium,
                style = FontStyle.Italic,
            ),
            Font(
                Res.font.Raleway_SemiBold,
                weight = FontWeight.SemiBold,
                style = FontStyle.Normal,
            ),
            Font(
                Res.font.Raleway_SemiBoldItalic,
                weight = FontWeight.SemiBold,
                style = FontStyle.Italic,
            ),
            Font(
                Res.font.Raleway_Bold,
                weight = FontWeight.Bold,
                style = FontStyle.Normal,
            ),
            Font(
                Res.font.Raleway_BoldItalic,
                weight = FontWeight.Bold,
                style = FontStyle.Italic,
            ),
        )
    )

val Typography
    @Composable
    get() = Typography(
        /* Other default text styles to override
        button = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.W500,
            fontSize = 14.sp
        ),
        caption = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp
        )
        */
    ).let {
        it.copy(
            displayLarge = it.displayLarge.copy(fontFamily = Raleway),
            displayMedium = it.displayMedium.copy(fontFamily = Raleway),
            displaySmall = it.displaySmall.copy(fontFamily = Raleway),
            headlineLarge = it.headlineLarge.copy(fontFamily = Raleway),
            headlineMedium = it.headlineMedium.copy(fontFamily = Raleway),
            headlineSmall = it.headlineSmall.copy(fontFamily = Raleway),
            titleLarge = it.titleLarge.copy(fontFamily = Raleway),
            titleMedium = it.titleMedium.copy(fontFamily = Raleway),
            titleSmall = it.titleSmall.copy(fontFamily = Raleway),
            bodyLarge = it.bodyLarge.copy(fontFamily = Raleway),
            bodyMedium = it.bodyMedium.copy(fontFamily = Raleway),
            bodySmall = it.bodySmall.copy(fontFamily = Raleway),
            labelLarge = it.labelLarge.copy(fontFamily = Raleway),
            labelMedium = it.labelMedium.copy(fontFamily = Raleway),
            labelSmall = it.labelSmall.copy(fontFamily = Raleway),
        )
    }