package com.alexvt.assistant.uitheme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.alexvt.assistant.platform.Font

object Fonts {
    @Composable
    fun jetbrainsMono() = FontFamily(
        Font(
            "Roboto Mono",
            "robotomono_regular",
            FontWeight.Normal,
            FontStyle.Normal
        ),
        Font(
            "Roboto Mono",
            "robotomono_italic",
            FontWeight.Normal,
            FontStyle.Italic
        ),

        Font(
            "Roboto Mono",
            "robotomono_semibold",
            FontWeight.Bold,
            FontStyle.Normal
        ),
        Font(
            "Roboto Mono",
            "robotomono_semibolditalic",
            FontWeight.Bold,
            FontStyle.Italic
        ),

        Font(
            "Roboto Mono",
            "robotomono_bold",
            FontWeight.ExtraBold,
            FontStyle.Normal
        ),
        Font(
            "Roboto Mono",
            "robotomono_bolditalic",
            FontWeight.ExtraBold,
            FontStyle.Italic
        ),

        Font(
            "Roboto Mono",
            "robotomono_medium",
            FontWeight.Medium,
            FontStyle.Normal
        ),
        Font(
            "Roboto Mono",
            "robotomono_mediumitalic",
            FontWeight.Medium,
            FontStyle.Italic
        )
    )
}
