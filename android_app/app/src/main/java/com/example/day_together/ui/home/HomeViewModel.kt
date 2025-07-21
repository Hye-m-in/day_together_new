package com.example.day_together.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.model.Anniversary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.data.repository.FakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

class HomeViewModel : ViewModel() {

    private val repository = FakeRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val user = repository.getUser("any_id")
            val question = repository.getTodaysQuestion()
            val quote = repository.getFamilyQuote()
            val events = repository.getCalendarEvents()

            // 디데이 정보 계산
            val dDayInfo = calculateDDayInfo(events)

            _uiState.value = _uiState.value.copy(
                user = user,
                aiQuestion = question,
                familyQuote = quote,
                eventsByDate = events,
                dDayText = dDayInfo.first,
                dDayTitle = dDayInfo.second,
                isLoading = false
            )
        }
    }

    fun refreshQuestion() {
        viewModelScope.launch {
            val newQuestion = repository.getTodaysQuestion()
            _uiState.value = _uiState.value.copy(aiQuestion = newQuestion)
        }
    }

    fun addOrUpdateEvent(event: CalendarEvent, date: LocalDate) {
        val currentEvents = _uiState.value.eventsByDate.toMutableMap()
        val eventsForDate = currentEvents.getOrDefault(date, emptyList()).toMutableList()
        val existingEventIndex = eventsForDate.indexOfFirst { it.id == event.id }

        if (existingEventIndex != -1) {
            eventsForDate[existingEventIndex] = event
        } else {
            val eventWithId = if(event.id.isBlank()) event.copy(id = UUID.randomUUID().toString()) else event
            eventsForDate.add(eventWithId)
        }

        currentEvents[date] = eventsForDate

        // 디데이 정보 다시 계산
        val dDayInfo = calculateDDayInfo(currentEvents)

        // 일정 목록, 디데이 정보를 한 번에 업데이트
        _uiState.value = _uiState.value.copy(
            eventsByDate = currentEvents,
            dDayText = dDayInfo.first,
            dDayTitle = dDayInfo.second
        )
    }

    fun deleteEvent(event: CalendarEvent, date: LocalDate) {
        val currentEvents = _uiState.value.eventsByDate.toMutableMap()
        val eventsForDate = currentEvents.getOrDefault(date, emptyList()).toMutableList()
        eventsForDate.removeAll { it.id == event.id }

        if (eventsForDate.isEmpty()) {
            currentEvents.remove(date)
        } else {
            currentEvents[date] = eventsForDate
        }

        // 디데이 정보를 다시 계산
        val dDayInfo = calculateDDayInfo(currentEvents)

        // 일정 목록과 디데이 정보를 한 번에 업데이트
        _uiState.value = _uiState.value.copy(
            eventsByDate = currentEvents,
            dDayText = dDayInfo.first,
            dDayTitle = dDayInfo.second
        )
    }

    //D-Day 정보를 계산하여 Pair(dDayText, dDayTitle) 형태로 반환하는 함수

    private fun calculateDDayInfo(events: Map<LocalDate, List<CalendarEvent>>): Pair<String, String> {
        val today = LocalDate.now()
        val allFutureEvents = events
            .flatMap { it.value }
            .filter { it.date.isAfter(today) || it.date.isEqual(today) }

        val priorityEvent = allFutureEvents
            .filter { it.isPriority }
            .minByOrNull { it.date }

        val closestEvent = priorityEvent ?: allFutureEvents.minByOrNull { it.date }

        return if (closestEvent != null) {
            val dDay = ChronoUnit.DAYS.between(today, closestEvent.date)
            val dDayText = "D-${dDay}"
            val dDayTitle = closestEvent.description
            Pair(dDayText, dDayTitle)
        } else {
            Pair("D-Day", "다가오는 일정이 없어요")
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val aiQuestion: Question? = null,
    val isQuestionAnsweredByAll: Boolean = true,
    val familyQuote: String = "",
    val upcomingAnniversary: Anniversary? = null,
    val dDayText: String = "",
    val dDayTitle: String = "",
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()
)