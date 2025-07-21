package com.example.day_together.ui.home.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.theme.TextPrimary

val WeeklyCalendarBorderColor = Color(0xFF533A28)

@Composable
fun WeeklyCalendarView(
    weeklyCalendarData: List<WeeklyCalendarDay>,
    modifier: Modifier = Modifier
) {
    val weeklyCalendarShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .border(
                width = 0.5.dp,
                color = WeeklyCalendarBorderColor,
                shape = weeklyCalendarShape

            )

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            weeklyCalendarData.forEachIndexed { index, dayData ->
                DayCellNew(
                    dateData = dayData,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                if (index < weeklyCalendarData.size - 1) {
                    VerticalDivider(
                        color = TextPrimary.copy(alpha = 0.15f),
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}