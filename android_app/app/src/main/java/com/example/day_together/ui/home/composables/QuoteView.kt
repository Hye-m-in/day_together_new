package com.example.day_together.ui.home.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.day_together.ui.theme.TextPrimary

@Composable
fun QuoteView(quote: String, modifier: Modifier = Modifier) {
    Text(
        text = quote,
        color = TextPrimary.copy(alpha = 0.7f),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()

    )
}