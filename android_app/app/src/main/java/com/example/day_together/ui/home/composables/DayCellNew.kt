package com.example.day_together.ui.home.composables

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.theme.AnniversaryBoardBackground
import com.example.day_together.ui.theme.TextPrimary
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun DayCellNew(
    dateData: WeeklyCalendarDay,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()

            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(modifier = Modifier.height(5.dp))


        val annotatedDateString = buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)) {
                append(dateData.date)
            }
            withStyle(style = SpanStyle(fontSize = 10.sp, color = TextPrimary.copy(alpha = 0.7f))) {
                append("(${dateData.dayOfWeek})")
            }
        }
        Text(
            text = annotatedDateString,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp)
        )


        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            thickness = 1.dp,
            color = TextPrimary.copy(alpha = 0.2f)
        )


        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            dateData.events.take(4).forEach { event ->
                Text(
                    text = event.description,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AnniversaryBoardBackground, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}



