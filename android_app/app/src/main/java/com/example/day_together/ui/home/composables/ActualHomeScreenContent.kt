package com.example.day_together.ui.home.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.theme.ScreenBackground
import com.example.day_together.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.remember

val LocalDate.yearMonth: YearMonth
    get() = YearMonth.of(this.year, this.month)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActualHomeScreenContent(
    upcomingAnniversaryText: String,
    dDayText: String,
    dDayTitle: String,
    randomCloudResIds: List<Int>,
    currentYearMonth: YearMonth,
    isMonthlyView: Boolean,
    selectedDateForDetails: LocalDate?,
    dateForBorderOnly: LocalDate?,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    weeklyCalendarData: List<WeeklyCalendarDay>,
    isQuestionAnsweredByAll: Boolean,
    aiQuestion: String,
    familyQuote: String,
    showAddEventInputScreen: Boolean,
    isBottomBarVisible: Boolean,
    onMonthChange: (YearMonth) -> Unit,
    onDateClick: (LocalDate?) -> Unit,
    onToggleCalendarView: () -> Unit,
    onMonthlyCalendarHeaderTitleClick: () -> Unit,
    onMonthlyCalendarHeaderIconClick: () -> Unit,
    onRefreshQuestionClicked: () -> Unit,
    onMonthlyTodayButtonClick: () -> Unit,
    onEditEventRequest: (LocalDate, CalendarEvent) -> Unit,
    onDeleteEventRequest: (LocalDate, CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayYearMonth = if (isMonthlyView) currentYearMonth else YearMonth.now()
    val displayYearMonthFormatted = remember(displayYearMonth) {
        DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN).format(displayYearMonth)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
        ) {
            AnniversaryBoard(text = upcomingAnniversaryText)
            Spacer(modifier = Modifier.height(24.dp))
            DDaySectionView(
                dDayText = dDayText,
                dDayTitle = dDayTitle,
                cloudImageResList = randomCloudResIds,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isMonthlyView) {
                MonthlyCalendarView(
                    currentMonth = currentYearMonth,
                    onMonthChange = onMonthChange,
                    onDateClick = onDateClick,
                    eventsByDate = eventsByDate,
                    selectedDateForDetails = selectedDateForDetails,
                    dateForBorderOnly = dateForBorderOnly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    onEditEventRequest = onEditEventRequest,
                    onDeleteEventRequest = onDeleteEventRequest,
                    onTitleClick = onMonthlyCalendarHeaderTitleClick,
                    onCalendarIconClick = onMonthlyCalendarHeaderIconClick,
                    onTodayHeaderButtonClick = onMonthlyTodayButtonClick
                )
            } else {
                Column(
                    modifier = Modifier

                        .padding(horizontal = 20.dp)
                ) {
                    Text(

                        text = displayYearMonthFormatted,
                        color = TextPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 21.sp),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .clickable { onToggleCalendarView() }
                            .padding(bottom = 20.dp) // 연월과 주간 캘린더 사이 간격
                    )
                    WeeklyCalendarView(
                        weeklyCalendarData = weeklyCalendarData,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!isMonthlyView) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // '오늘의 질문' 섹션의 상단 간격 조절
                    Spacer(modifier = Modifier.height(60.dp))
                    TodayQuestionHeaderWithAlert(isAnsweredByAll = isQuestionAnsweredByAll)
                    Spacer(modifier = Modifier.height(12.dp))
                    TodayQuestionContentCard(questionText = aiQuestion)
                    Spacer(modifier = Modifier.height(18.dp))
                    RefreshQuestionButton(
                        isAnsweredByAll = isQuestionAnsweredByAll,
                        onRefreshQuestionClicked = onRefreshQuestionClicked,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    // '명언 문구' 섹션의 상단 간격 조절
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }

            val showQuote = !isMonthlyView && !showAddEventInputScreen && selectedDateForDetails == null

            if (showQuote) {
                QuoteView(
                    quote = familyQuote,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
                )
            } else {
                if (isMonthlyView) {
                    Spacer(modifier = Modifier.height(20.dp))
                } else {

                    Spacer(modifier = Modifier.height(20.dp + (MaterialTheme.typography.bodyMedium.fontSize.value.dp * 2)))
                }
            }


            if (!isMonthlyView) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}