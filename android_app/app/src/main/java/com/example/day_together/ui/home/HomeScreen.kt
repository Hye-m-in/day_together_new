package com.example.day_together.ui.home

// Compose 및 UI 관련
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// 프로젝트 내부 다른 파일 및 클래스
import com.example.day_together.R
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.WheelCustomYearMonthPickerDialog
import com.example.day_together.ui.dialogs.InvitationDialog
import com.example.day_together.ui.home.composables.ActualHomeScreenContent
import com.example.day_together.ui.home.composables.AddEventInputView
import com.example.day_together.ui.home.composables.DateEventsBottomSheet

// 자바/코틀린 기본 라이브러리
import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.YearMonth

import java.time.format.TextStyle

import java.util.Locale
import java.time.DayOfWeek as JavaDayOfWeek

/**
 * 앱의 메인 화면(홈)을 구성하는 Composable 함수
 */
// OptIn 삭제
@Composable
fun HomeScreen(
    // appNavController 파라미터 삭제
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

        // 1. 로딩, 2. (캘린더 + 차단) 순으로
        if (uiState.isLoading) {
            // 1. 로딩 중
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

        } else {
            // 2. 로딩 완료: 캘린더 UI를 먼저 그림
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
                    if (event.type != "BIRTHDAY") {
                        dateForNewEvent = date
                        eventToEdit = event
                        currentEventTitleInput = event.title
                        currentEventIsPriority = event.isPriority
                        showAddEventSheet = true
                        selectedDateForDetails = null
                    }
                },
                onDeleteEventRequest = { _, event ->
                    if (event.type != "BIRTHDAY") {
                        eventToDeleteConfirmState = event
                        showDeleteConfirmDialog = true
                    }
                }
            )

            // 3. 채팅방이 없을 경우에만 반투명 오버레이를 그림
            if (uiState.chatRoomId == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)) // 반투명 배경
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "가족채팅방 생성 후 이용 가능합니다.",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                }
            } else {
                // 4. 채팅방이 있을 때만 하단 시트/다이얼로그 등이 동작하도록 함
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
                                if (eventToEditFromSheet.type != "BIRTHDAY") {
                                    dateForNewEvent = selectedDateForDetails
                                    eventToEdit = eventToEditFromSheet
                                    currentEventTitleInput = eventToEditFromSheet.title
                                    currentEventIsPriority = eventToEditFromSheet.isPriority
                                    showAddEventSheet = true
                                    selectedDateForDetails = null
                                }
                            },
                            onDeleteEventRequested = { eventToDelete ->
                                if (eventToDelete.type != "BIRTHDAY") {
                                    eventToDeleteConfirmState = eventToDelete
                                    showDeleteConfirmDialog = true
                                }
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
                                if (titleToSave.isNotBlank() && dateForNewEvent != null) {

                                    val newPriority = currentEventIsPriority
                                    val newPrioritySetAt: Timestamp? = if (newPriority) {
                                        Timestamp.now()
                                    } else {
                                        null
                                    }

                                    val eventToSave = eventToEdit?.copy(
                                        title = titleToSave,
                                        date = dateForNewEvent!!.toString(),   // 수정 모드에서 date 유지/갱신
                                        isPriority = newPriority,
                                        prioritySetAt = newPrioritySetAt
                                    ) ?: CalendarEvent(
                                        title = titleToSave,
                                        date = dateForNewEvent!!.toString(),   // 새 일정 생성 시 필수
                                        creatorId = uiState.user?.uid ?: "",
                                        creatorName = uiState.user?.name ?: "",
                                        type = "EVENT",
                                        isPriority = newPriority,
                                        prioritySetAt = newPrioritySetAt
                                    )

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
            }
        }

        // 초대장 다이얼로그 (로딩이 끝났으면 항상 표시)
        if (!uiState.isLoading && invitedChatRoomId.value != null) {
            InvitationDialog(
                onAccept = {
                    val invitationId = invitedChatRoomId.value!!
                    // MainActivity의 onAcceptInvitation 람다 호출
                    onAcceptInvitation(invitationId)
                },
                onDismiss = {
                    onDismissInvitation() // MainActivity의 onDismissInvitation 람다 호출
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