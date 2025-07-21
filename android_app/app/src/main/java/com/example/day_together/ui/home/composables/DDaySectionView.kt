package com.example.day_together.ui.home.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.TextPrimary

@Composable
fun DDaySectionView(
    dDayText: String,
    dDayTitle: String,
    cloudImageResList: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = dDayText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dDayTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary
                )
            )
        }



        Row {
            cloudImageResList.forEachIndexed { index, resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = "cloud",
                    modifier = Modifier
                        .size(100.dp)
                )
                if (index < cloudImageResList.size - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}