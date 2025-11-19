package com.example.day_together.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.CalendarManager
import com.example.day_together.data.model.Anniversary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.AuthResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

/**
 * 데이터를 한 곳(ViewModel)에서만 통제함으로써 코드가 꼬이는 것을 막음
 */
class HomeViewModel : ViewModel() {

    private val repository = AppRepository
    private val calendarManager = CalendarManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var calendarEventsListener: ListenerRegistration? = null

    init {
        loadInitialData()
    }

    /**
     * 초기 데이터 로드
     * 1. 사용자 정보 로드
     * 2. 채팅방 유무 확인 -> 없으면 로드 중단 (MainActivity에서 잠금 화면 표시)
     * 3. 채팅방 있으면 가족 멤버 및 캘린더 이벤트 구독
     */
    fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }

        calendarEventsListener?.remove()

        viewModelScope.launch {
            // 1. 기본 정보 로드
            val user = repository.getCurrentUser()
            val questionString = repository.getTodaysQuestion()

            val question = questionString?.let { Question(id = UUID.randomUUID().toString(), text = it) }

            _uiState.update { it.copy(user = user, aiQuestion = question, familyQuote = "") }

            if (user == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // 2. 채팅방 ID 탐색 및 상태 업데이트
            val chatRoomId = repository.findUserChatRoomId(user.uid)
            _uiState.update { it.copy(chatRoomId = chatRoomId) }

            // 3. 분기 처리
            if (chatRoomId == null) {
                // a. 채팅방 없음: 홈 화면 차단 상태 유지 (MainActivity에서 처리)
                Log.d("HomeViewModel", "채팅방 없음. 홈 화면 데이터 로드 중단.")
                _uiState.update { it.copy(isLoading = false) }
            } else {
                // b. 채팅방 있음: 가족 캘린더 모드 시작
                Log.d("HomeViewModel", "채팅방($chatRoomId) 발견. 가족 데이터 로드 시작.")

                // b-1. 가족 구성원 생일 이벤트 생성
                val familyMembers = repository.getFamilyMembers(chatRoomId)
                val familyBirthdayEvents = createBirthdayEvents(familyMembers)
                _uiState.update { it.copy(birthdayEventsByDate = familyBirthdayEvents) }

                // b-2. 가족 일정 실시간 구독
                calendarEventsListener = calendarManager.listenForEvents(chatRoomId) { familyEvents ->
                    val familyEventsMap = familyEvents.groupBy {
                        it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    }

                    val mergedEvents = mergeEventMaps(familyEventsMap, familyBirthdayEvents)
                    val dDayInfo = calculateDDayInfo(mergedEvents)

                    _uiState.update {
                        it.copy(
                            eventsByDate = mergedEvents,
                            dDayText = dDayInfo.first,
                            dDayTitle = dDayInfo.second,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun refreshQuestion() {
        viewModelScope.launch {
            val newQuestionString = repository.getTodaysQuestion()
            val newQuestion = newQuestionString?.let { Question(id = UUID.randomUUID().toString(), text = it) }
            _uiState.value = _uiState.value.copy(aiQuestion = newQuestion)
        }
    }

    fun addOrUpdateEvent(event: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid
        if (currentUserId == null || currentChatRoomId == null) return

        viewModelScope.launch {
            calendarManager.addEvent(currentChatRoomId, event)
        }
    }

    fun setExclusiveDDay(newEvent: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid
        if (currentUserId == null || currentChatRoomId == null) return

        viewModelScope.launch {
            val oldDDayEvents = _uiState.value.eventsByDate.values.flatten()
                .filter { it.id != newEvent.id && it.isPriority }

            for (oldEvent in oldDDayEvents) {
                val updatedOldEvent = oldEvent.copy(
                    isPriority = false,
                    prioritySetAt = null
                )
                calendarManager.addEvent(currentChatRoomId, updatedOldEvent)
            }

            calendarManager.addEvent(currentChatRoomId, newEvent)
        }
    }


    fun deleteEvent(event: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid
        if (currentUserId == null || currentChatRoomId == null) return

        viewModelScope.launch {
            calendarManager.deleteEvent(currentChatRoomId, event.id)
        }
    }

    private fun calculateDDayInfo(events: Map<LocalDate, List<CalendarEvent>>): Pair<String, String> {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val allFutureEvents = events.values.flatten().filter {
            val eventDate = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            eventDate.isAfter(today) || eventDate.isEqual(today)
        }

        // 1. D-1 생일 우선 확인
        val d1Birthday = allFutureEvents.find { event ->
            val eventDate = event.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            event.type == "BIRTHDAY" && eventDate.isEqual(tomorrow)
        }

        if (d1Birthday != null) {
            return Pair("D-1", d1Birthday.title)
        }

        // 2. 설정된 D-Day 확인
        val priorityEvents = allFutureEvents.filter { it.isPriority && it.prioritySetAt != null }
        val latestPriorityEvent = priorityEvents.maxByOrNull { it.prioritySetAt!!.seconds }
        val closestEvent = latestPriorityEvent ?: allFutureEvents.minByOrNull { it.startTime.seconds }

        return closestEvent?.let {
            val eventDate = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val dDay = ChronoUnit.DAYS.between(today, eventDate)

            val dDayText = if (dDay == 0L) "D-Day" else "D-${dDay}"
            val dDayTitle = it.title
            Pair(dDayText, dDayTitle)
        } ?: Pair("D-Day", "다가오는 일정이 없어요")
    }

    private fun createBirthdayEvents(members: List<User>): Map<LocalDate, List<CalendarEvent>> {
        val events = mutableListOf<CalendarEvent>()
        val today = LocalDate.now()
        val birthDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        for (member in members) {
            val birthDateStr = member.birthDate
            try {
                if (birthDateStr != null && birthDateStr.length == 8) {
                    val birthDate = LocalDate.parse(birthDateStr, birthDateFormatter)
                    val title: String = "${member.name} 님의 생일"
                    var thisYearBirthday: LocalDate = birthDate.withYear(today.year)

                    if (thisYearBirthday.isBefore(today.minusDays(1))) {
                        thisYearBirthday = thisYearBirthday.plusYears(1)
                    }

                    val birthdayEvent = CalendarEvent(
                        id = "birthday_${member.uid}",
                        title = title,
                        description = "${birthDate.monthValue}월 ${birthDate.dayOfMonth}일",
                        startTime = Timestamp(Date.from(thisYearBirthday.atStartOfDay(ZoneId.systemDefault()).toInstant())),
                        endTime = null,
                        creatorId = member.uid,
                        creatorName = member.name,
                        type = "BIRTHDAY",
                        isPriority = false
                    )
                    events.add(birthdayEvent)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "${member.name}님의 생일($birthDateStr) 변환 중 오류", e)
            }
        }

        return events.groupBy {
            it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    private fun mergeEventMaps(
        regularEvents: Map<LocalDate, List<CalendarEvent>>,
        birthdayEvents: Map<LocalDate, List<CalendarEvent>>
    ): Map<LocalDate, List<CalendarEvent>> {
        val allKeys = regularEvents.keys + birthdayEvents.keys
        return allKeys.associateWith { date ->
            (regularEvents[date] ?: emptyList()) + (birthdayEvents[date] ?: emptyList())
        }
    }

    override fun onCleared() {
        super.onCleared()
        calendarEventsListener?.remove()
    }

    /**
     * 초대 수락 (AppRepository 호출)
     * MainActivity에서 호출되며, 성공 시 chatRoomId를 반환
     */
    fun acceptInvitation(invitationId: String, onResult: (chatRoomId: String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.acceptInvitation(invitationId)) {
                is AuthResult.Success -> {
                    onResult(result.chatRoomId)
                }
                is AuthResult.Failure -> {
                    Log.e("HomeViewModel", result.message)
                    onResult(null)
                }
            }
        }
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val chatRoomId: String? = null,
    val aiQuestion: Question? = null,
    val isQuestionAnsweredByAll: Boolean = true,
    val familyQuote: String = "",
    val upcomingAnniversary: Anniversary? = null,
    val dDayText: String = "",
    val dDayTitle: String = "",
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val birthdayEventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()
)