package com.example.day_together.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
import com.example.day_together.ui.home.composables.TodayQuestionHeaderWithAlert
import com.example.day_together.ui.home.composables.RefreshQuestionButton
import com.example.day_together.ui.message.MessageScreen
import com.example.day_together.ui.gallery.GalleryScreen
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.day_together.R
import com.example.day_together.ui.navigation.BottomNavItem
import com.example.day_together.ui.settings.SettingsScreen
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.NavIconSelected
import com.example.day_together.ui.theme.NavIconUnselected
import com.example.day_together.ui.theme.TextPrimary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.WeeklyCalendarDay
import com.example.day_together.ui.home.composables.ActualHomeScreenContent
import com.example.day_together.ui.home.composables.AddEventInputView
import com.example.day_together.ui.home.composables.DateEventsBottomSheet
import com.example.day_together.ui.message.ChatInfoScreen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import java.time.DayOfWeek as JavaDayOfWeek
import com.example.day_together.ui.WheelCustomYearMonthPickerDialog
import kotlin.random.Random

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "일정 삭제",
                fontWeight = FontWeight.Medium
            )
        },
        text = { Text("이 일정을 삭제하시겠습니까?") },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(appNavController: NavController) {
    val mainNavController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Message,
        BottomNavItem.Gallery,
        BottomNavItem.Settings
    )

    var upcomingAnniversaryText by remember { mutableStateOf("D-3 엄마 생일!") }
    var dDayTextState by remember { mutableStateOf("D-3") }
    var dDayTitleState by remember { mutableStateOf("엄마 생일") }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var isMonthlyView by remember { mutableStateOf(false) }
    var selectedDateForDetails by remember { mutableStateOf<LocalDate?>(null) }
    var dateForBorderOnly by remember { mutableStateOf<LocalDate?>(null) }
    val today = LocalDate.now()
    val eventsByDateState = remember { mutableStateMapOf<LocalDate, List<CalendarEvent>>() }
    val weeklyCalendarDataState = remember(today, eventsByDateState, isMonthlyView) {
        if(!isMonthlyView) {
            val firstDayOfRelevantWeek = today.with(JavaDayOfWeek.MONDAY)
            (0 until 7).map { dayOffset ->
                val date = firstDayOfRelevantWeek.plusDays(dayOffset.toLong())
                WeeklyCalendarDay(
                    date = date.dayOfMonth.toString(),
                    dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    isToday = date.isEqual(today),
                    events = eventsByDateState[date] ?: emptyList()
                )
            }
        } else {
            emptyList()
        }
    }
    var isQuestionAnsweredByAllState by remember { mutableStateOf(false) }
    var aiQuestionState by remember { mutableStateOf("우리 가족만의 특별한 루틴이 있나요?") }
    var familyQuoteState by remember { mutableStateOf("\"가족 사랑은 평화의 시작이다.\"") }

    val allCloudDrawables = remember {
        listOf(
            R.drawable.ic_cloud1,
            R.drawable.ic_cloud2,
            R.drawable.ic_cloud3,
            R.drawable.ic_cloud4,
            R.drawable.ic_cloud5,
            R.drawable.ic_cloud6
        )
    }
    val randomCloudResIds by remember {
        mutableStateOf(
            if (allCloudDrawables.size >= 2) {
                allCloudDrawables.shuffled(Random(System.currentTimeMillis())).take(2)
            } else {
                allCloudDrawables
            }
        )
    }


    var showAddEventSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateForNewEvent by remember { mutableStateOf<LocalDate?>(null) }
    var showCustomYearMonthPicker by remember { mutableStateOf(false) }
    var currentEventDescriptionInput by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var eventToDeleteConfirmState by remember { mutableStateOf<CalendarEvent?>(null) }
    var dateOfEventToDeleteConfirmState by remember { mutableStateOf<LocalDate?>(null) }


    LaunchedEffect(Unit) {
        val sampleDate1 = LocalDate.now().plusDays(1)
        eventsByDateState[sampleDate1] = listOf(
            CalendarEvent(id = "1", description = "점심 약속 🍔", date = sampleDate1),
            CalendarEvent(id = "2", description = "프로젝트 회의 💻", date = sampleDate1)
        )
        val sampleDate2 = LocalDate.now()
        eventsByDateState[sampleDate2] = listOf(
            CalendarEvent(id = "3", description = "오늘 할 일!", date = sampleDate2)
        )
    }

    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != BottomNavItem.Message.route && currentRoute != "chat_info_screen_route") {
                Column {
                    Divider(color = TextPrimary.copy(alpha = 0.2f), thickness = 0.5.dp)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val currentDestination = navBackStackEntry?.destination
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { Icon(imageVector = ImageVector.vectorResource(id = screen.iconResId), contentDescription = screen.label, tint = if (isSelected) NavIconSelected else NavIconUnselected) },
                                selected = isSelected,
                                onClick = {
                                    mainNavController.navigate(screen.route) {
                                        popUpTo(mainNavController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = mainNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ActualHomeScreenContent(
                        upcomingAnniversaryText = upcomingAnniversaryText,
                        dDayText = dDayTextState,
                        dDayTitle = dDayTitleState,
                        randomCloudResIds = randomCloudResIds,
                        currentYearMonth = currentYearMonth,
                        isMonthlyView = isMonthlyView,
                        selectedDateForDetails = selectedDateForDetails,
                        dateForBorderOnly = dateForBorderOnly,
                        eventsByDate = eventsByDateState,
                        weeklyCalendarData = weeklyCalendarDataState,
                        isQuestionAnsweredByAll = isQuestionAnsweredByAllState,
                        aiQuestion = aiQuestionState,
                        familyQuote = familyQuoteState,
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
                                showCustomYearMonthPicker = true
                            }
                        },
                        onRefreshQuestionClicked = { /* TODO: ViewModel */ },
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
                                events = eventsByDateState[selectedDateForDetails!!] ?: emptyList(),
                                onDismiss = {
                                    selectedDateForDetails = null
                                },
                                onAddNewEventClick = {
                                    dateForNewEvent = selectedDateForDetails
                                    eventToEdit = null
                                    currentEventDescriptionInput = ""
                                    showAddEventSheet = true
                                    selectedDateForDetails = null
                                },
                                onEditEvent = { eventToEditFromSheet ->
                                    dateForNewEvent = selectedDateForDetails
                                    eventToEdit = eventToEditFromSheet
                                    currentEventDescriptionInput = eventToEditFromSheet.description
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
                            AddEventInputView(
                                visible = true,
                                targetDate = dateForNewEvent!!,
                                eventDescription = currentEventDescriptionInput,
                                isEditing = isInEditMode,
                                onDescriptionChange = { newDescription ->
                                    currentEventDescriptionInput = newDescription
                                },
                                onSave = {
                                    val descriptionToSave = currentEventDescriptionInput.trim()
                                    if (descriptionToSave.isNotBlank()) {
                                        if (isInEditMode && eventToEdit != null) {
                                            val updatedEvent = eventToEdit!!.copy(description = descriptionToSave)
                                            val currentEvents = eventsByDateState[dateForNewEvent!!]?.toMutableList() ?: mutableListOf()
                                            val index = currentEvents.indexOfFirst { it.id == updatedEvent.id }
                                            if (index != -1) {
                                                currentEvents[index] = updatedEvent
                                                eventsByDateState[dateForNewEvent!!] = currentEvents
                                            }
                                        } else {
                                            val newEvent = CalendarEvent(
                                                id = UUID.randomUUID().toString(),
                                                description = descriptionToSave,
                                                date = dateForNewEvent!!
                                            )
                                            val currentEvents = eventsByDateState[dateForNewEvent!!]?.toMutableList() ?: mutableListOf()
                                            currentEvents.add(newEvent)
                                            eventsByDateState[dateForNewEvent!!] = currentEvents
                                        }
                                    }
                                    showAddEventSheet = false
                                    eventToEdit = null
                                    dateForNewEvent = null
                                    currentEventDescriptionInput = ""
                                },
                                onCancel = {
                                    showAddEventSheet = false
                                    eventToEdit = null
                                    dateForNewEvent = null
                                    currentEventDescriptionInput = ""
                                }
                            )
                        }
                    }

                    if (showCustomYearMonthPicker) {
                        WheelCustomYearMonthPickerDialog(
                            initialYearMonth = currentYearMonth,
                            onDismissRequest = {
                                showCustomYearMonthPicker = false
                            },
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
                                val date = dateOfEventToDeleteConfirmState!!
                                val event = eventToDeleteConfirmState!!
                                eventsByDateState[date] = eventsByDateState[date]?.filterNot { it.id == event.id } ?: emptyList()

                                if (eventsByDateState[date].isNullOrEmpty() && selectedDateForDetails == date) {
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
            composable(BottomNavItem.Message.route) {
                MessageScreen(navController = mainNavController)
            }
            composable(BottomNavItem.Gallery.route) {
                GalleryScreen(navController = mainNavController)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(navController = appNavController)
            }
            composable("chat_info_screen_route") {
                ChatInfoScreen(navController = mainNavController)
            }
        }
    }
}

@Preview(showBackground = true, name = "전체 홈 화면 (Scaffold 포함)", widthDp = 390, heightDp = 844)
@Composable
fun FullHomeScreenPreview() {
    Day_togetherTheme {
        HomeScreen(appNavController = rememberNavController())
    }
}

@Preview(name = "HomeScreen - 오늘의 질문 (모두 답변 안 함)", showBackground = true, backgroundColor = 0xFFF5F0E8)
@Composable
fun HomeScreenTodaysQuestionNotAnsweredPreview() {
    Day_togetherTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            TodayQuestionHeaderWithAlert(isAnsweredByAll = false)
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "AI 질문 예시입니다.",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(18.dp))

            RefreshQuestionButton(
                isAnsweredByAll = false,
                onRefreshQuestionClicked = {}
            )
        }
    }
}

@Preview(name = "HomeScreen - 오늘의 질문 (모두 답변 완료)", showBackground = true, backgroundColor = 0xFFF5F0E8)
@Composable
fun HomeScreenTodaysQuestionAnsweredPreview() {
    Day_togetherTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TodayQuestionHeaderWithAlert(isAnsweredByAll = true)
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Text(
                    "AI 질문 예시입니다. (모두 답변)",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            RefreshQuestionButton(
                isAnsweredByAll = true,
                onRefreshQuestionClicked = {}
            )
        }
    }
}