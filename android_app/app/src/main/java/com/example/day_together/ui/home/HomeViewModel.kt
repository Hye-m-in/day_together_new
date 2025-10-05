package com.example.day_together.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.model.Anniversary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.data.repository.AppRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID


/**
 * 데이터를 한 곳(ViewModel)에서만 통제함으로써 코드가 꼬이는 것을 막음
 * UI 화면이 마음대로 데이터를 바꾸면 앱이 복잡해질수록 어디서 버그가 생기는지 찾기 매우 어려워짐
 */


class HomeViewModel : ViewModel() {

    private val repository = AppRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 실시간 리스너를 관리하기 위한 변수
    private var calendarListener: ListenerRegistration? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // 사용자 정보, 질문, 명언 등은 기존과 동일하게 불러옴
            val user = repository.getCurrentUser()
            val question = repository.getTodaysQuestion()
            val quote = repository.getFamilyQuote()
            _uiState.update { it.copy(user = user, aiQuestion = question, familyQuote = quote) }

            // 채팅방 ID를 찾음
            val chatRoomId = user?.uid?.let { repository.findUserChatRoomId(it) }

            // 채팅방이 존재하면, 해당 채팅방의 캘린더 이벤트를 실시간으로 구독
            if (chatRoomId != null) {
                _uiState.update { it.copy(chatRoomId = chatRoomId) } // UI State에 chatRoomId 저장

                // AppRepository에 추가한 실시간 리스너 함수 호출
                calendarListener = repository.listenForCalendarEvents(chatRoomId) { eventsByDate ->
                    // 캘린더 데이터가 변경될 때마다 D-Day 다시 계산
                    val dDayInfo = calculateDDayInfo(eventsByDate)
                    // 최신 데이터로 UI 상태 업데이트
                    _uiState.update {
                        it.copy(
                            eventsByDate = eventsByDate,
                            dDayText = dDayInfo.first,
                            dDayTitle = dDayInfo.second,
                            isLoading = false // 데이터 로드가 완료되었으므로 로딩 상태 해제
                        )
                    }
                }
            } else {
                // 채팅방이 없으면 로딩 상태만 해제
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refreshQuestion() {
        viewModelScope.launch {
            val newQuestion = repository.getTodaysQuestion()
            _uiState.value = _uiState.value.copy(aiQuestion = newQuestion)
        }
    }

    /**
     * UI에서 받은 이벤트를 Firestore에 저장/수정하도록 Repository에 요청
     */
    fun addOrUpdateEvent(event: CalendarEvent) {
        // UI State에 저장된 chatRoomId를 사용
        val chatRoomId = _uiState.value.chatRoomId ?: return

        viewModelScope.launch {
            repository.addOrUpdateCalendarEvent(chatRoomId, event)
            // 데이터 저장은 위 함수에서 처리되고, UI 업데이트는 실시간 리스너가 자동으로 처리함
        }
    }

    /**
     * UI에서 요청한 이벤트를 Firestore에서 삭제하도록 Repository에 요청
     */
    fun deleteEvent(event: CalendarEvent) {
        val chatRoomId = _uiState.value.chatRoomId ?: return

        viewModelScope.launch {
            repository.deleteCalendarEvent(chatRoomId, event.id)
            // 데이터 삭제는 위 함수에서 처리되고, UI 업데이트는 실시간 리스너가 자동으로 처리함
        }
    }

    

    private fun calculateDDayInfo(events: Map<LocalDate, List<CalendarEvent>>): Pair<String, String> {
        val today = LocalDate.now()
        val allFutureEvents = events.values.flatten().filter {
            val eventDate = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            eventDate.isAfter(today) || eventDate.isEqual(today)
        }

        // isPriority가 true인 이벤트를 먼저 찾음
        val priorityEvent = allFutureEvents
            .filter { it.isPriority }
            .minByOrNull { it.startTime.seconds }

        // 우선순위 이벤트가 없으면, 전체 미래 일정 중 가장 가까운 것 찾음
        val closestEvent = priorityEvent ?: allFutureEvents.minByOrNull { it.startTime.seconds }

        return if (closestEvent != null) {
            val eventDate = closestEvent.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val dDay = ChronoUnit.DAYS.between(today, eventDate)

            val dDayText = if (dDay == 0L) "D-Day" else "D-${dDay}"
            val dDayTitle = closestEvent.title
            Pair(dDayText, dDayTitle)
        } else {
            Pair("D-Day", "다가오는 일정이 없어요")
        }
    }

    /**
     * ViewModel이 소멸될 때 Firestore 리스너를 반드시 제거하여 메모리 누수 방지
     */
    override fun onCleared() {
        super.onCleared()
        calendarListener?.remove()
    }
}

// HomeUiState에 chatRoomId를 추가하여 ViewModel 내부에서 쉽게 접근할 수 있도록 함
data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val chatRoomId: String? = null, // 채팅방 ID 추가
    val aiQuestion: Question? = null,
    val isQuestionAnsweredByAll: Boolean = true,
    val familyQuote: String = "",
    val upcomingAnniversary: Anniversary? = null,
    val dDayText: String = "",
    val dDayTitle: String = "",
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()
)