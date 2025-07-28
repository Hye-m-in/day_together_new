package com.example.day_together.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.WheelCustomYearMonthPickerDialog
import com.example.day_together.ui.home.composables.ActualHomeScreenContent
import com.example.day_together.ui.home.composables.AddEventInputView
import com.example.day_together.ui.home.composables.DateEventsBottomSheet
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.time.DayOfWeek as JavaDayOfWeek
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day_together.R
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appNavController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    // ViewModel로 UI 상태 구독
    val uiState by homeViewModel.uiState.collectAsState()

    // UI 자체 상태 관리 변수
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var isMonthlyView by remember { mutableStateOf(false) }
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    var dateForBorderOnly by remember { mutableStateOf<LocalDate?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var showCustomYearMonthPicker by remember { mutableStateOf(false) }

    // 일정 추가/수정 상태 변수
    var currentEventDescriptionInput by remember { mutableStateOf("") }
    var currentEventIsPriority by remember { mutableStateOf(false) } // d-day 설정 스위치 상태

    // 삭제 확인 다이얼로그 상태 변수
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var eventToDeleteConfirmState by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateOfEventToDeleteConfirmState by remember { mutableStateOf<LocalDate?>(null) }

    // ViewModel 데이터 기반 계산되는 변수
    val today = LocalDate.now()
    val weeklyCalendarDataState = remember(today, uiState.eventsByDate, isMonthlyView) {
        if(!isMonthlyView) {
            val firstDayOfRelevantWeek = today.with(JavaDayOfWeek.MONDAY)
            (0 until 7).map { dayOffset ->
                val date = firstDayOfRelevantWeek.plusDays(dayOffset.toLong())
                WeeklyCalendarDay(
                    date = date.dayOfMonth.toString(),
                    dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    isToday = date.isEqual(today),
                    events = uiState.eventsByDate[date] ?: emptyList()
                )
            }
        } else {
            emptyList()
        }
    }

    // 기타 UI 변수
    val allCloudDrawables = remember {
        listOf(
            R.drawable.ic_cloud1, R.drawable.ic_cloud2, R.drawable.ic_cloud3,
            R.drawable.ic_cloud4, R.drawable.ic_cloud5, R.drawable.ic_cloud6
        )
    }
    val randomCloudResIds by remember {
        mutableStateOf(
            if (allCloudDrawables.size >= 2) allCloudDrawables.shuffled().take(2)
            else allCloudDrawables
        )
    }

    // 화면에 그릴 내용
    Box(modifier = Modifier.fillMaxSize()) {
        ActualHomeScreenContent(
            upcomingAnniversaryText = uiState.dDayTitle, // ViewModel에서 계산된 디데이 제목 사용
            dDayText = uiState.dDayText,
            dDayTitle = uiState.dDayTitle,
            randomCloudResIds = randomCloudResIds,
            currentYearMonth = currentYearMonth,
            isMonthlyView = isMonthlyView,
            selectedDateForDetails = selectedDateForDetails,
            dateForBorderOnly = dateForBorderOnly,
            eventsByDate = uiState.eventsByDate,
            weeklyCalendarData = weeklyCalendarDataState,
            isQuestionAnsweredByAll = uiState.isQuestionAnsweredByAll,
            aiQuestion = uiState.aiQuestion?.text ?: "로딩 중",
            familyQuote = uiState.familyQuote,
            showAddEventInputScreen = showAddEventSheet,
            isBottomBarVisible = !showAddEventSheet && selectedDateForDetails == null,
            onMonthChange = { newMonth -> currentYearMonth = newMonth },
            onDateClick = { dateClicked ->
                if (dateClicked != null) {
                    selectedDateForDetails = dateClicked
                    dateForBorderOnly = dateClicked
                    showAddEventSheet = false
                    eventToEdit = null
                    dateForNewEvent = null
                } else {
                    selectedDateForDetails = null
                }
            },
            onToggleCalendarView = { isMonthlyView = !isMonthlyView },
            onMonthlyCalendarHeaderTitleClick = { isMonthlyView = false },
            onMonthlyCalendarHeaderIconClick = { if(isMonthlyView) showCustomYearMonthPicker = true },
            onRefreshQuestionClicked = homeViewModel::refreshQuestion,
            onMonthlyTodayButtonClick = {
                val todayDate = LocalDate.now()
                currentYearMonth = YearMonth.from(todayDate)
                dateForBorderOnly = todayDate
                selectedDateForDetails = null
                showAddEventSheet = false
            },
            // 수정버튼 클릭하면 해당 이벤트 isPriority 값도 상태에 저장
            onEditEventRequest = { date, event ->
                dateForNewEvent = date
                eventToEdit = event
                currentEventDescriptionInput = event.description
                currentEventIsPriority = event.isPriority // isPriority 상태 설정
                showAddEventSheet = true
                selectedDateForDetails = null
            },
            onDeleteEventRequest = { date, event ->
                eventToDeleteConfirmState = event
                dateOfEventToDeleteConfirmState = date
                showDeleteConfirmDialog = true
            }
        )

        if (selectedDateForDetails != null && !showAddEventSheet) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                DateEventsBottomSheet(
                    visible = true,
                    targetDate = selectedDateForDetails!!,
                    events = uiState.eventsByDate[selectedDateForDetails!!] ?: emptyList(),
                    onDismiss = { selectedDateForDetails = null },
                    // 새 일정 추가 시, isPriority 상태 false로 초기화
                    onAddNewEventClick = {
                        dateForNewEvent = selectedDateForDetails
                        eventToEdit = null
                        currentEventDescriptionInput = ""
                        currentEventIsPriority = false // isPriority 상태 초기화
                        showAddEventSheet = true
                        selectedDateForDetails = null
                    },
                    // 일정 수정 시, 해당 이벤트의 isPriority 값으로 상태 설정
                    onEditEvent = { eventToEditFromSheet ->
                        dateForNewEvent = selectedDateForDetails
                        eventToEdit = eventToEditFromSheet
                        currentEventDescriptionInput = eventToEditFromSheet.description
                        currentEventIsPriority = eventToEditFromSheet.isPriority // isPriority 상태 설정
                        showAddEventSheet = true
                        selectedDateForDetails = null
                    },
                    onDeleteEventRequested = { eventToDelete ->
                        eventToDeleteConfirmState = eventToDelete
                        dateOfEventToDeleteConfirmState = selectedDateForDetails
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }

        if (showAddEventSheet && dateForNewEvent != null) {
            val isInEditMode = eventToEdit != null
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                // AddEventInputView에 isPriority 상태와 변경 함수를 전달
                AddEventInputView(
                    visible = true,
                    targetDate = dateForNewEvent!!,
                    eventDescription = currentEventDescriptionInput,
                    isEditing = isInEditMode,
                    isPriority = currentEventIsPriority, // isPriority 상태 전달
                    onDescriptionChange = { newDescription ->
                        currentEventDescriptionInput = newDescription
                    },
                    onPriorityChange = { newPriority -> // 스위치 값 변경 시 상태 업데이트
                        currentEventIsPriority = newPriority
                    },
                    onSave = {
                        val descriptionToSave = currentEventDescriptionInput.trim()
                        if (descriptionToSave.isNotBlank()) {
                            // 저장 시, isPriority 값을 포함하여 Event 객체 생성
                            val eventToSave = eventToEdit?.copy(
                                description = descriptionToSave,
                                isPriority = currentEventIsPriority
                            ) ?: CalendarEvent(
                                description = descriptionToSave,
                                date = dateForNewEvent!!,
                                isPriority = currentEventIsPriority
                            )
                            homeViewModel.addOrUpdateEvent(eventToSave, dateForNewEvent!!)
                        }
                        showAddEventSheet = false
                        eventToEdit = null
                        dateForNewEvent = null
                        currentEventDescriptionInput = ""
                        currentEventIsPriority = false
                    },
                    onCancel = {
                        showAddEventSheet = false
                        eventToEdit = null
                        dateForNewEvent = null
                        currentEventDescriptionInput = ""
                        currentEventIsPriority = false
                    }
                )
            }
        }

        if (showCustomYearMonthPicker) {
            WheelCustomYearMonthPickerDialog(
                initialYearMonth = currentYearMonth,
                onDismissRequest = { showCustomYearMonthPicker = false },
                onConfirm = { selectedYearMonth ->
                    currentYearMonth = selectedYearMonth
                    selectedDateForDetails = null
                    dateForBorderOnly = null
                }
            )
        }

        if (showDeleteConfirmDialog && eventToDeleteConfirmState != null && dateOfEventToDeleteConfirmState != null) {
            DeleteConfirmationDialog(
                onConfirm = {
                    homeViewModel.deleteEvent(eventToDeleteConfirmState!!, dateOfEventToDeleteConfirmState!!)
                    if (uiState.eventsByDate[dateOfEventToDeleteConfirmState!!].isNullOrEmpty() && selectedDateForDetails == dateOfEventToDeleteConfirmState!!) {
                        selectedDateForDetails = null
                        dateForBorderOnly = null
                    }
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    eventToDeleteConfirmState = null
                    dateOfEventToDeleteConfirmState = null
                }
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("일정 삭제", fontWeight = FontWeight.Medium) },
        text = { Text("이 일정을 삭제하시겠습니까?") },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text("삭제") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}