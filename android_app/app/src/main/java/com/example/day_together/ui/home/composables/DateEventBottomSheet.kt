package com.example.day_together.ui.home.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateEventsBottomSheet(
    visible: Boolean,
    targetDate: LocalDate,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onAddNewEventClick: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEventRequested: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)", Locale.KOREAN)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(durationMillis = 250), initialOffsetY = { it }),
        exit = slideOutVertically(animationSpec = tween(durationMillis = 200), targetOffsetY = { it }),
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = screenHeight * 0.25f, max = screenHeight * 0.45f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, WeeklyCalendarBorderColor.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = TextPrimary.copy(alpha = 0.7f))
                    }
                }
                HorizontalDivider(color = TextPrimary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (events.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "등록된 일정이 없습니다.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(items = events, key = { _, event -> event.id }) { index, event ->
                                EventListItem(
                                    event = event,
                                    onEditClick = { onEditEvent(event) },
                                    onDeleteClick = { onDeleteEventRequested(event) }
                                )
                                if (index < events.lastIndex) {
                                    HorizontalDivider(color = TextPrimary.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddNewEventClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextPrimary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "새 일정 만들기 아이콘")
                    Spacer(Modifier.width(8.dp))
                    Text("새 일정 만들기", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}