package com.example.day_together.ui.home.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.AnniversaryBoardBackground
import com.example.day_together.ui.theme.TextPrimary


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnniversaryBoard(text: String, modifier: Modifier = Modifier) {
    val isPreview = LocalInspectionMode.current


    val repetitions = 3
    val separator = "     "

    val wideDisplayText = remember(text, repetitions, separator) {
        if (text.isEmpty()) {
            " "
        } else {

            List(repetitions) { text }.joinToString(separator)
        }
    }

    Text(
        text = wideDisplayText,
        modifier = modifier
            .fillMaxWidth()
            .background(AnniversaryBoardBackground)
            .padding(vertical = 10.dp)
            .basicMarquee(
                iterations = if (isPreview) 5 else Int.MAX_VALUE

            ),
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        ),
        maxLines = 1,
        softWrap = false
    )
}