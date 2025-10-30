package com.example.day_together.ui.home

// Compose 및 UI 관련
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

// Navigation 및 ViewModel 관련
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// 프로젝트 내부 다른 파일 및 클래스
import com.example.day_together.R
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.WheelCustomYearMonthPickerDialog
import com.example.day_together.ui.home.composables.ActualHomeScreenContent
import com.example.day_together.ui.home.composables.AddEventInputView
import com.example.day_together.ui.home.composables.DateEventsBottomSheet

// 자바/코틀린 기본 라이브러리
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.time.DayOfWeek as JavaDayOfWeek

/**
 * 앱의 메인 화면(홈)을 구성하는 Composable 함수
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appNavController: NavController,
    homeViewModel: HomeViewModel,
    invitedChatRoomId: MutableState<String?>,
    onAcceptInvitation: (String) -> Unit,
    onDismissInvitation: () -> Unit
) {
    val uiState by homeViewModel.uiState.collectAsState()

    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var isMonthlyView by remember { mutableStateOf(false) }
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    var dateForBorderOnly by remember { mutableStateOf<LocalDate?>(null) }
    var showAddEventSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var showCustomYearMonthPicker by remember { mutableStateOf(false) }
    var tempSelectedYearMonth by remember { mutableStateOf(currentYearMonth) }

    // 변수 이름 명확화 및 isPriority 상태 변수
    var currentEventTitleInput by remember { mutableStateOf("") }
    var currentEventIsPriority by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var eventToDeleteConfirmState by remember { mutableStateOf<CalendarEvent?>(null) }

    val today = LocalDate.now()
    val weeklyCalendarDataState: List<WeeklyCalendarDay> = remember(today, uiState.eventsByDate, isMonthlyView) {
        if (!isMonthlyView) {
            val firstDayOfRelevantWeek = today.with(JavaDayOfWeek.MONDAY)
            (0 until 7).map { offset ->
                val date = firstDayOfRelevantWeek.plusDays(offset.toLong())
                WeeklyCalendarDay(
                    date = date.dayOfMonth.toString().padStart(2,'0'),
                    dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    events = uiState.eventsByDate[date] ?: emptyList(),
                    isToday = date.isEqual(today)
                )
            }
        } else emptyList()
    }


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
                currentEventTitleInput = event.title
                // 오래된 데이터도 스위치가 켜져 보이도록 수정
                currentEventIsPriority = event.isPriority
                showAddEventSheet = true
                selectedDateForDetails = null
            },
            onDeleteEventRequest = { _, event ->
                eventToDeleteConfirmState = event
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
                    onAddNewEventClick = {
                        dateForNewEvent = selectedDateForDetails
                        eventToEdit = null
                        currentEventTitleInput = ""
                        currentEventIsPriority = false // 새로 추가 시 기본값 false
                        showAddEventSheet = true
                        selectedDateForDetails = null
                    },
                    onEditEvent = { eventToEditFromSheet ->
                        dateForNewEvent = selectedDateForDetails
                        eventToEdit = eventToEditFromSheet
                        currentEventTitleInput = eventToEditFromSheet.title
                        // 오래된 데이터도 스위치가 켜져 보이도록 수정
                        currentEventIsPriority = eventToEditFromSheet.isPriority
                        showAddEventSheet = true
                        selectedDateForDetails = null
                    },
                    onDeleteEventRequested = { eventToDelete ->
                        eventToDeleteConfirmState = eventToDelete
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }

        if (showAddEventSheet && dateForNewEvent != null) {
            val isInEditMode = eventToEdit != null
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AddEventInputView(
                    visible = true,
                    targetDate = dateForNewEvent!!,
                    eventTitle = currentEventTitleInput,
                    isEditing = isInEditMode,
                    isPriority = currentEventIsPriority,
                    onTitleChange = { newTitle ->
                        currentEventTitleInput = newTitle
                    },
                    onPriorityChange = { newPriority ->
                        currentEventIsPriority = newPriority
                    },
                    onSave = {
                        val titleToSave = currentEventTitleInput.trim()
                        if (titleToSave.isNotBlank()) {

                            // D-Day 로직 단순화
                            val newPriority = currentEventIsPriority
                            val newPrioritySetAt: Timestamp?

                            if (newPriority) {
                                // 스위치가 ON이면, 켠 시간을 지금으로 기록
                                newPrioritySetAt = Timestamp.now()
                            } else {
                                // 스위치가 OFF면, 켠 시간을 null로
                                newPrioritySetAt = null
                            }

                            val eventToSave = eventToEdit?.copy(
                                title = titleToSave,
                                isPriority = newPriority, // 현재 스위치 상태
                                prioritySetAt = newPrioritySetAt // 켠 시간 (혹은 null)
                            ) ?: CalendarEvent(
                                title = titleToSave,
                                startTime = Timestamp(Date.from(dateForNewEvent!!.atStartOfDay(ZoneId.systemDefault()).toInstant())),
                                creatorId = uiState.user?.uid ?: "",
                                creatorName = uiState.user?.name ?: "",
                                isPriority = newPriority,
                                prioritySetAt = newPrioritySetAt
                            )

                            // 스위치가 켜졌으면(true) setExclusiveDDay 호출, 꺼졌으면(false) addOrUpdateEvent 호출
                            if (newPriority) {
                                homeViewModel.setExclusiveDDay(eventToSave)
                            } else {
                                homeViewModel.addOrUpdateEvent(eventToSave)
                            }
                        }
                        showAddEventSheet = false
                        eventToEdit = null
                        dateForNewEvent = null
                        currentEventTitleInput = ""
                        currentEventIsPriority = false
                    },
                    onCancel = {
                        showAddEventSheet = false
                        eventToEdit = null
                        dateForNewEvent = null
                        currentEventTitleInput = ""
                        currentEventIsPriority = false
                    }
                )
            }
        }

        if (showCustomYearMonthPicker) {
            WheelCustomYearMonthPickerDialog(
                initialYearMonth = currentYearMonth,
                onDismissRequest = {
                    currentYearMonth = tempSelectedYearMonth
                    selectedDateForDetails = null
                    dateForBorderOnly = null
                    showCustomYearMonthPicker = false
                },
                onSelectionChanged = { selectedYearMonth ->
                    tempSelectedYearMonth = selectedYearMonth
                }
            )
        }

        if (showDeleteConfirmDialog && eventToDeleteConfirmState != null) {
            DeleteConfirmationDialog(
                onConfirm = {
                    homeViewModel.deleteEvent(eventToDeleteConfirmState!!)
                    if (selectedDateForDetails != null && uiState.eventsByDate[selectedDateForDetails!!].isNullOrEmpty()) {
                        selectedDateForDetails = null
                        dateForBorderOnly = null
                    }
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    eventToDeleteConfirmState = null
                }
            )
        }

        invitedChatRoomId.value?.let { chatRoomId ->
            InvitationDialog(
                onAccept = { onAcceptInvitation(chatRoomId) },
                onDismiss = onDismissInvitation
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

@Composable
fun InvitationDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("초대 도착") },
        text = { Text("가족 채팅방에 초대받았습니다. 입장하시겠습니까?") },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("입장하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}