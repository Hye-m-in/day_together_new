package com.example.day_together.ui.home.composables

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.ui.theme.TextPrimary
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.painterResource
import com.example.day_together.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.ui.theme.*
import java.time.LocalDate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.clipToBounds
import java.util.UUID
@Composable
internal fun MonthlyCalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTitleClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onTodayHeaderButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN)
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onCalendarIconClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_year_month_picker),
                contentDescription = "날짜 선택",
                tint = TextPrimary,
                modifier = Modifier.size(25.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_custom_arrow_left),
                    contentDescription = "이전 달",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = currentMonth.format(formatter),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 21.sp
                ),
                color = TextPrimary,
                modifier = Modifier
                    .clickable { onTitleClick() }
                    .padding(horizontal = 4.dp)
            )
            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_custom_arrow_right),
                    contentDescription = "다음 달",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        TextButton(onClick = onTodayHeaderButtonClick, modifier = Modifier.height(36.dp)) {
            Text("오늘", color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

data class MonthlyCalendarCellData(
    val date: LocalDate?,
    val isCurrentMonth: Boolean,
    var events: List<CalendarEvent> = emptyList()
)

private fun getDaysForMonthlyCalendarGrid(
    yearMonth: YearMonth,
    allEvents: Map<LocalDate, List<CalendarEvent>>
): List<MonthlyCalendarCellData?> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val daysToPrepend = firstDayOfMonth.dayOfWeek.value % 7
    val calendarDays = mutableListOf<MonthlyCalendarCellData?>()

    if (daysToPrepend > 0) {
        val prevMonth = yearMonth.minusMonths(1)
        val daysInPrevMonth = prevMonth.lengthOfMonth()
        for (i in 0 until daysToPrepend) {
            val date = prevMonth.atDay(daysInPrevMonth - daysToPrepend + 1 + i)
            calendarDays.add(MonthlyCalendarCellData(date, false, allEvents[date] ?: emptyList()))
        }
    }

    for (day in 1..daysInMonth) {
        val date = yearMonth.atDay(day)
        calendarDays.add(MonthlyCalendarCellData(date, true, allEvents[date] ?: emptyList()))
    }

    val totalCellsToDisplay = 35
    val currentCellCount = calendarDays.size
    if (currentCellCount < totalCellsToDisplay) {
        val nextMonth = yearMonth.plusMonths(1)
        for (day in 1..(totalCellsToDisplay - currentCellCount)) {
            val date = nextMonth.atDay(day)
            calendarDays.add(MonthlyCalendarCellData(date, false, allEvents[date] ?: emptyList()))
        }
    }
    return calendarDays
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthlyCalendarView(
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDateClick: (LocalDate?) -> Unit,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    selectedDateForDetails: LocalDate?,
    dateForBorderOnly: LocalDate?,
    modifier: Modifier = Modifier,
    onEditEventRequest: (LocalDate, CalendarEvent) -> Unit,
    onDeleteEventRequest: (LocalDate, CalendarEvent) -> Unit,
    onTitleClick: () -> Unit,
    onCalendarIconClick: () -> Unit,
    onTodayHeaderButtonClick: () -> Unit
) {
    val today = LocalDate.now()
    val daysInGrid: List<MonthlyCalendarCellData?> = remember(currentMonth, eventsByDate.toMap()) {
        getDaysForMonthlyCalendarGrid(currentMonth, eventsByDate)
    }

    BoxWithConstraints(modifier = modifier) {
        val totalAvailableHeight = maxHeight
        Column(modifier = Modifier.fillMaxSize()) {
            var headerSectionHeight by remember { mutableStateOf(0.dp) }
            val density = LocalDensity.current

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        headerSectionHeight = with(density) { coordinates.size.height.toDp() }
                    }
            ) {
                MonthlyCalendarHeader(
                    currentMonth = currentMonth,
                    onPreviousMonth = { onMonthChange(currentMonth.minusMonths(1)) },
                    onNextMonth = { onMonthChange(currentMonth.plusMonths(1)) },
                    onTitleClick = onTitleClick,
                    onCalendarIconClick = onCalendarIconClick,
                    onTodayHeaderButtonClick = onTodayHeaderButtonClick,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                DayOfWeekHeaderMonthly(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
            }

            val gridAvailableHeight = totalAvailableHeight - headerSectionHeight
            val numberOfRows = 5
            val verticalSpacingBetweenRows = 4.dp
            val totalSpacingHeight = verticalSpacingBetweenRows * (numberOfRows - 1).coerceAtLeast(0)

            val dynamicCellHeight = if (numberOfRows > 0 && gridAvailableHeight > totalSpacingHeight) {
                (gridAvailableHeight - totalSpacingHeight) / numberOfRows
            } else {
                60.dp.coerceAtLeast(1.dp)
            }

            if (gridAvailableHeight > 0.dp && dynamicCellHeight > 0.dp && headerSectionHeight > 0.dp) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridAvailableHeight),
                    horizontalArrangement = Arrangement.Absolute.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(verticalSpacingBetweenRows)
                ) {
                    items(
                        items = daysInGrid,
                        key = { dayInfoNullable -> dayInfoNullable?.date?.toEpochDay() ?: UUID.randomUUID() }
                    ) { dayInfo ->
                        MonthlyDayCell(
                            date = dayInfo?.date,
                            isCurrentMonth = dayInfo?.isCurrentMonth ?: false,
                            isToday = dayInfo?.date == today,
                            showPopupHighlight = dayInfo?.date != null && dayInfo.date == selectedDateForDetails,
                            showBorderOnlyHighlight = dayInfo?.date != null && dayInfo.date == dateForBorderOnly,
                            events = dayInfo?.events ?: emptyList(),
                            onClick = {
                                if (dayInfo?.date != null && dayInfo.isCurrentMonth) {
                                    onDateClick(dayInfo.date)
                                }
                            },
                            cellHeight = dynamicCellHeight
                        )
                    }
                }
            } else if (headerSectionHeight > 0.dp) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayOfWeekHeaderMonthly(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
            Text(
                text = day,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthlyDayCell(
    date: LocalDate?,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    showPopupHighlight: Boolean,
    showBorderOnlyHighlight: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
    cellHeight: Dp,
    modifier: Modifier = Modifier
) {
    val textColor = if (!isCurrentMonth) OtherMonthDayText else TextPrimary

    var actualBorderColor: Color = Color.Transparent
    var actualBorderWidth: Dp = 0.dp
    var actualBackgroundColor: Color = if (isToday && isCurrentMonth) TodayMonthlyBackground else Color.Transparent

    if (showBorderOnlyHighlight) {
        actualBorderColor = SelectedMonthlyBorder
        actualBorderWidth = 1.5.dp
    } else if (showPopupHighlight) {
        actualBorderColor = SelectedMonthlyBorder
        actualBorderWidth = 1.5.dp
        actualBackgroundColor = SelectedMonthlyBorder.copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .height(cellHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(actualBackgroundColor)
            .border(BorderStroke(actualBorderWidth, actualBorderColor), RoundedCornerShape(4.dp))
            .clickable(enabled = date != null && isCurrentMonth) { onClick() }
            .padding(horizontal = 3.dp, vertical = 4.dp),
        contentAlignment = Alignment.TopStart
    ) {
        if (date != null) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isToday && isCurrentMonth) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = 12.sp
                        ),
                        color = if (isToday && isCurrentMonth) TextPrimary else textColor
                    )
                    if (isCurrentMonth && events.size > 4) {
                        Text(
                            text = "+${events.size - 4}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (isCurrentMonth) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .clipToBounds(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val maxEventsToShow = if (cellHeight < 50.dp) 1 else if (cellHeight < 70.dp) 2 else 4
                        events.take(maxEventsToShow).forEach { event ->
                            Text(
                                text = event.description,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 2.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsDialog(
    date: LocalDate,
    events: List<CalendarEvent>,
    onDismiss: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)", Locale.KOREAN)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                date.format(formatter),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            if (events.isEmpty()) {
                Text("등록된 일정이 없습니다.", modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    // LazyColumn의 items 함수를 List를 직접 받는 형태로 수정
                    items(
                        items = events,
                        key = { event -> event.id }
                    ) { event: CalendarEvent -> // event 타입을 명시적으로 CalendarEvent로 지정
                        var showMenu by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(event.description, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Box {
                                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "더보기", tint = TextPrimary)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("수정") },
                                        onClick = {
                                            onEditEvent(event)
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("삭제") },
                                        onClick = {
                                            onDeleteEvent(event)
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = TextPrimary.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

@Preview(showBackground = true, name = "월간 캘린더 뷰 (전체)", widthDp = 390)
@Composable
fun MonthlyCalendarViewFullPreview() {
    Day_togetherTheme {
        val today = LocalDate.now()
        val dummyEvents = mapOf(
            today.plusDays(1) to listOf(
                CalendarEvent(description = "회의", date = today.plusDays(1)),
                CalendarEvent(description = "점심 약속", date = today.plusDays(1)),
                CalendarEvent(description = "저녁 식사", date = today.plusDays(1)),
                CalendarEvent(description = "추가 일정", date = today.plusDays(1))
            ),
            today.plusDays(2) to listOf(CalendarEvent(description = "발표 준비 데드라인", date = today.plusDays(2))),
            today.plusDays(3) to listOf(
                CalendarEvent(description = "스터디", date = today.plusDays(3)),
                CalendarEvent(description = "운동", date = today.plusDays(3)),
                CalendarEvent(description = "장보기 목록 작성", date = today.plusDays(3)),
                CalendarEvent(description = "친구와 저녁 약속", date = today.plusDays(3)),
                CalendarEvent(description = "도서관 책 반납", date = today.plusDays(3))
            ),
            today to listOf(
                CalendarEvent(description = "오늘의 할일 1", date = today),
                CalendarEvent(description = "오늘의 할일 2", date = today),
                CalendarEvent(description = "오늘의 할일 3", date = today),
                CalendarEvent(description = "오늘의 할일 4 (길게 써보기 테스트으으으으으으으으으)", date = today),
                CalendarEvent(description = "오늘의 할일 5", date = today)
            ),
            today.minusDays(2) to listOf(CalendarEvent(description = "지난 일정", date = today.minusDays(2)))
        )

        var currentMonthPreview by remember { mutableStateOf(YearMonth.now()) }
        var selectedDateForDetailsPreview by remember { mutableStateOf<LocalDate?>(null) }
        var dateForBorderOnlyPreview by remember { mutableStateOf<LocalDate?>(null) }

        Column(Modifier.background(Color.White)) {
            MonthlyCalendarView(
                currentMonth = currentMonthPreview,
                onMonthChange = { newMonth ->
                    currentMonthPreview = newMonth
                    selectedDateForDetailsPreview = null
                    dateForBorderOnlyPreview = null
                },
                onDateClick = { clickedDate ->
                    if (clickedDate == null) return@MonthlyCalendarView
                    if (clickedDate == today) {
                        if (dateForBorderOnlyPreview == today) {
                            selectedDateForDetailsPreview = today
                            dateForBorderOnlyPreview = null
                        } else {
                            selectedDateForDetailsPreview = null
                            dateForBorderOnlyPreview = today
                        }
                    } else {
                        selectedDateForDetailsPreview = clickedDate
                        dateForBorderOnlyPreview = null
                    }
                },
                eventsByDate = dummyEvents,
                selectedDateForDetails = selectedDateForDetailsPreview,
                dateForBorderOnly = dateForBorderOnlyPreview,
                onEditEventRequest = { _, _ -> },
                onDeleteEventRequest = { _, _ -> },
                onTitleClick = { },
                onCalendarIconClick = { },
                onTodayHeaderButtonClick = {
                    currentMonthPreview = YearMonth.now()
                    selectedDateForDetailsPreview = null
                    dateForBorderOnlyPreview = LocalDate.now()
                }
            )
            Text("Selected for Details (Popup): ${selectedDateForDetailsPreview?.toString() ?: "None"}")
            Text("Border Only For (Today first click): ${dateForBorderOnlyPreview?.toString() ?: "None"}")
        }
    }
}