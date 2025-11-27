package com.example.day_together.ui.home

import com.example.day_together.AuthManager
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

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


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


    fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }

        calendarEventsListener?.remove()

        viewModelScope.launch {
            val user = repository.getCurrentUser()
            val currentUserId = AuthManager.getCurrentUserId()

            if (user == null || currentUserId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // 일단 유저 정보 먼저 저장
            _uiState.update { it.copy(user = user) }

            // 채팅방 ID 탐색
            val chatRoomId = repository.findUserChatRoomId(currentUserId)
            _uiState.update { it.copy(chatRoomId = chatRoomId) }

            if (chatRoomId == null) {
                _uiState.update { it.copy(isLoading = false) }
            } else {
                // 채팅방 ID가 있을 때 -> 질문 가져오기 & 가족 데이터 로드
                val question = repository.getTodaysQuestion(chatRoomId)
                _uiState.update { it.copy(aiQuestion = question) }

                loadFamilyData(chatRoomId, user)
            }
        }
    }



    // 코드 중복 방지를 위해 분리한 함수
    private suspend fun loadFamilyData(chatRoomId: String, user: User) {
        // b-1. 가족 구성원 불러오기
        val familyMembersFromRepo = repository.getFamilyMembers(chatRoomId)
        val familyMembers = if (familyMembersFromRepo.isEmpty()) listOfNotNull(user) else familyMembersFromRepo

        val familyBirthdayEvents = createBirthdayEvents(familyMembers)
        _uiState.update { it.copy(birthdayEventsByDate = familyBirthdayEvents) }

        // b-2. 가족 일정 구독
        calendarEventsListener = calendarManager.listenForEvents(chatRoomId) { familyEvents ->

            // date가 비어있는 event는 파싱하기 전에 필터링
            val validEvents = familyEvents.filter { event ->
                if (event.date.isNullOrBlank()) {
                    Log.w("HomeViewModel", "무시된 이벤트: date가 비어있습니다 (id=${event.id}, title=${event.title})")
                    false
                } else true
            }

            // 안전하게 변환
            val familyEventsMap = validEvents.groupBy { event ->
                LocalDate.parse(event.date)   // "2025-11-27" -> LocalDate
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
            event ->
            val eventDate = LocalDate.parse(event.date)   // "2025-11-27" → LocalDate
            eventDate.isAfter(today) || eventDate.isEqual(today)
        }

        // 1. D-1 생일 우선 확인
        val d1Birthday = allFutureEvents.find { event ->
            event.type == "BIRTHDAY" && LocalDate.parse(event.date).isEqual(tomorrow)
        }

        if (d1Birthday != null) {
            return Pair("D-1", d1Birthday.title)
        }

        // 2-1. 설정된 D-Day 확인 : 가장 최근에 d-day를 켠 일정 우선
        val priorityEvents = allFutureEvents.filter { it.isPriority && it.prioritySetAt != null }
        val latestPriorityEvent = priorityEvents.maxByOrNull { it.prioritySetAt!!.seconds }
        // 2-2. 없으면, 날짜가 가장 가까운 일정 선택
        val closestEvent = latestPriorityEvent ?: allFutureEvents.minByOrNull { event ->
            LocalDate.parse(event.date).toEpochDay()
        }

        return closestEvent?.let { event ->
            val eventDate = LocalDate.parse(event.date)
            val dDay = ChronoUnit.DAYS.between(today, eventDate)

            val dDayText = if (dDay == 0L) "D-Day" else "D-${dDay}"
            val dDayTitle = event.title
            Pair(dDayText, dDayTitle)
        } ?: Pair("D-Day", "다가오는 일정이 없어요")
    }

    private fun createBirthdayEvents(members: List<User>): Map<LocalDate, List<CalendarEvent>> {
        val today = LocalDate.now()
        val birthDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")




        val birthdayList = members.mapNotNull { member ->
            val birthDateStr = member.birthDate ?: return@mapNotNull null

            try {
                if (birthDateStr.length != 8) return@mapNotNull null

                val birthDate = LocalDate.parse(birthDateStr, birthDateFormatter)
                val title = "${member.name} 님의 생일"

                var birthdayThisYear = birthDate.withYear(today.year)
                if (birthdayThisYear.isBefore(today.minusDays(1))) {
                    birthdayThisYear = birthdayThisYear.plusYears(1)
                }

                CalendarEvent(
                    id = "birthday_${member.uid}",
                    title = title,
                    description = "${birthDate.monthValue}월 ${birthDate.dayOfMonth}일",
                    date = birthdayThisYear.toString(),
                    creatorId = member.uid,
                    creatorName = member.name,
                    type = "BIRTHDAY",
                    isPriority = false
                )

            } catch (e: Exception) {
                Log.e("HomeViewModel", "생일 변환 오류 - ${member.name} : $birthDateStr", e)
                null
            }
        }

        return birthdayList.groupBy { event ->
            LocalDate.parse(event.date)
        }
    }


    private fun mergeEventMaps(
        regularEvents: Map<LocalDate, List<CalendarEvent>>,
        birthdayEvents: Map<LocalDate, List<CalendarEvent>>
    ): Map<LocalDate, List<CalendarEvent>> {
        val allKeys = regularEvents.keys + birthdayEvents.keys
        return allKeys.associateWith { date ->
            val mergedList = (regularEvents[date] ?: emptyList()) + (birthdayEvents[date] ?: emptyList())

            mergedList.distinctBy { it.id } // ID가 같은 중복 일정 제거
        }
    }

    override fun onCleared() {
        super.onCleared()
        calendarEventsListener?.remove()
    }

    // 초대 수락 -> String? (채팅방 ID)를 직접 반환
    fun acceptInvitation(invitationId: String, onResult: (chatRoomId: String?) -> Unit) {
        viewModelScope.launch {
            // 1. Repository에서 채팅방 ID를 직접 받아옴 (실패 시 null)
            val chatRoomId = repository.acceptInvitation(invitationId)

            if (chatRoomId != null) {
                // 2. 성공 시: ViewModel 상태(uiState)를 즉시 업데이트
                _uiState.update { it.copy(chatRoomId = chatRoomId) }

                // 3. 현재 로그인한 유저의 생일을 Firestore events에 저장
                val currentUserId = _uiState.value.user?.uid ?: AuthManager.getCurrentUserId()
                if (!currentUserId.isNullOrBlank()) {
                    CalendarManager.registerBirthday(chatRoomId, currentUserId)
                }

                // 4. MainActivity로 ID 전달 (다이얼로그 닫기 및 이동용)
                Log.d("HomeViewModel", "초대 수락 성공. 방 ID: $chatRoomId")
                onResult(chatRoomId)
            } else {
                // 5. 실패 시: null 전달
                Log.e("HomeViewModel", "초대 수락 실패 (Repository returned null)")
                onResult(null)
            }
        }
    }


    // 초대 거절 함수
    fun rejectInvitation(
        invitationId: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.rejectInvitation(invitationId)

            if (result is AuthResult.Success) {
                // 특별히 UI 상태 건드릴 건 없고, 그냥 콜백으로 다이얼로그 닫아주면 됨
                onComplete()
            } else if (result is AuthResult.Failure) {
                // 필요하면 토스트 띄우거나 로그 찍는 용도로 에러 처리
                Log.e("HomeViewModel", "초대 거절 실패: ${result.message}")
                onComplete() // 실패했어도 일단 다이얼로그는 닫고 싶다면 유지
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
)}