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

/**
 * 앱의 메인 화면(홈)을 구성하는 Composable 함수.
 * 캘린더, D-Day, AI 질문 등 핵심 기능을 포함합니다.
 * @param appNavController 앱 전체의 화면 전환을 담당하는 NavController
 * @param homeViewModel HomeScreen의 상태와 로직을 관리하는 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appNavController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    // ViewModel로부터 UI 상태를 구독하여, 상태 변경 시 자동으로 UI가 업데이트되도록 함
    val uiState by homeViewModel.uiState.collectAsState()

    // --- UI 자체의 상태를 관리하는 변수들 ---
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var isMonthlyView by remember { mutableStateOf(false) }
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    var dateForBorderOnly by remember { mutableStateOf<LocalDate?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var showCustomYearMonthPicker by remember { mutableStateOf(false) }

    // 다이얼로그의 임시 선택 값을 저장할 상태 변수
    var tempSelectedYearMonth by remember { mutableStateOf(currentYearMonth) }

    // 일정 추가/수정 관련 상태 변수
    var currentEventDescriptionInput by remember { mutableStateOf("") }
    var currentEventIsPriority by remember { mutableStateOf(false) }

    // 삭제 확인 다이얼로그 관련 상태 변수
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var eventToDeleteConfirmState by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateOfEventToDeleteConfirmState by remember { mutableStateOf<LocalDate?>(null) }

    // 오늘 날짜를 기준으로 주간 달력 데이터를 계산
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

    // 배경에 표시될 구름 이미지 리소스 목록
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

    // 화면에 그릴 내용을 Box 안에 배치
    Box(modifier = Modifier.fillMaxSize()) {
        ActualHomeScreenContent(
            upcomingAnniversaryText = uiState.dDayTitle,
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
            onMonthlyCalendarHeaderIconClick = {
                if(isMonthlyView) {
                    // 다이얼로그 열 때 임시 상태 동기화
                    tempSelectedYearMonth = currentYearMonth
                    showCustomYearMonthPicker = true
                }
            },
            onRefreshQuestionClicked = homeViewModel::refreshQuestion,
            onMonthlyTodayButtonClick = {
                val todayDate = LocalDate.now()
                currentYearMonth = YearMonth.from(todayDate)
                dateForBorderOnly = todayDate
                selectedDateForDetails = null
                showAddEventSheet = false
            },
            onEditEventRequest = { date, event ->
                dateForNewEvent = date
                eventToEdit = event
                currentEventDescriptionInput = event.description
                currentEventIsPriority = event.isPriority
                showAddEventSheet = true
                selectedDateForDetails = null
            },
            onDeleteEventRequest = { date, event ->
                eventToDeleteConfirmState = event
                dateOfEventToDeleteConfirmState = date
                showDeleteConfirmDialog = true
            }
        )

        // 특정 날짜가 선택되었고, 일정 추가 모드가 아닐 때 BottomSheet를 표시
        if (selectedDateForDetails != null && !showAddEventSheet) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                DateEventsBottomSheet(
                    visible = true,
                    targetDate = selectedDateForDetails!!,
                    events = uiState.eventsByDate[selectedDateForDetails!!] ?: emptyList(),
                    onDismiss = { selectedDateForDetails = null },
                    onAddNewEventClick = {
                        dateForNewEvent = selectedDateForDetails
                        eventToEdit = null
                        currentEventDescriptionInput = ""
                        currentEventIsPriority = false
                        showAddEventSheet = true
                        selectedDateForDetails = null
                    },
                    onEditEvent = { eventToEditFromSheet ->
                        dateForNewEvent = selectedDateForDetails
                        eventToEdit = eventToEditFromSheet
                        currentEventDescriptionInput = eventToEditFromSheet.description
                        currentEventIsPriority = eventToEditFromSheet.isPriority
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

        // 일정 추가/수정 모드일 때 입력창 BottomSheet를 표시
        if (showAddEventSheet && dateForNewEvent != null) {
            val isInEditMode = eventToEdit != null
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AddEventInputView(
                    visible = true,
                    targetDate = dateForNewEvent!!,
                    eventDescription = currentEventDescriptionInput,
                    isEditing = isInEditMode,
                    isPriority = currentEventIsPriority,
                    onDescriptionChange = { newDescription ->
                        currentEventDescriptionInput = newDescription
                    },
                    onPriorityChange = { newPriority ->
                        currentEventIsPriority = newPriority
                    },
                    onSave = {
                        val descriptionToSave = currentEventDescriptionInput.trim()
                        if (descriptionToSave.isNotBlank()) {
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

        // 커스텀 년/월 선택 다이얼로그 표시
        if (showCustomYearMonthPicker) {
            // 다이얼로그 호출 로직
            WheelCustomYearMonthPickerDialog(
                initialYearMonth = currentYearMonth,
                // 다이얼로그가 닫힐 때(외부 클릭, 뒤로가기) 최종 값을 확정
                onDismissRequest = {
                    currentYearMonth = tempSelectedYearMonth // 임시 값을 최종 값으로 반영
                    selectedDateForDetails = null
                    dateForBorderOnly = null
                    showCustomYearMonthPicker = false
                },
                // 스크롤이 멈출 때마다 임시 상태만 업데이트
                onSelectionChanged = { selectedYearMonth ->
                    tempSelectedYearMonth = selectedYearMonth
                }
            )

        }

        // 삭제 확인 다이얼로그 표시
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

/**
 * 일정 삭제 시 확인을 받기 위한 간단한 AlertDialog Composable
 */
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