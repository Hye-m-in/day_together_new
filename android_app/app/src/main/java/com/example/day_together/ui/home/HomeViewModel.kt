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
 * UI 화면이 마음대로 데이터를 바꾸면 앱이 복잡해질수록 어디서 버그가 생기는지 찾기 매우 어려워짐
 */


class HomeViewModel : ViewModel() {

    private val repository = AppRepository
    // CalendarManager 직접 참조
    private val calendarManager = CalendarManager

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 변수 이름 변경 (가족/개인 공용)
    private var calendarEventsListener: ListenerRegistration? = null

    init {
        loadInitialData()
    }

    // 'public'으로 유지 (초대 수락 시 MainActivity에서 호출해야 함)
    fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }

        // [리스너가 남아있으면 제거 (개인 -> 가족 전환 시 중복 방지)
        calendarEventsListener?.remove()

        viewModelScope.launch {
            // 1. 기본 정보 로드 (사용자, 질문, 명언)
            val user = repository.getCurrentUser()
            val questionString = repository.getTodaysQuestion() // String? 반환
            // val quote = repository.getFamilyQuote()

            // String?을 Question? 객체로 변환
            val question = questionString?.let { Question(id = UUID.randomUUID().toString(), text = it) }

            // quote 삭제, question 객체로 업데이트
            _uiState.update { it.copy(user = user, aiQuestion = question, familyQuote = "") } // familyQuote는 빈 값으로

            if (user == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // 2. 채팅방 ID 탐색
            val chatRoomId = repository.findUserChatRoomId(user.uid)
            _uiState.update { it.copy(chatRoomId = chatRoomId) } // chatRoomId가 null이든 아니든 state에 저장

            // 3. 분기 처리
            if (chatRoomId == null) {
                // a. 채팅방 없음: 홈 화면 차단
                Log.d("HomeViewModel", "채팅방 없음. 홈 화면 사용이 차단됩니다.")
                // 개인 일정/생일 로드 로직 제거
                _uiState.update { it.copy(isLoading = false) } // 로딩만 멈춤

            } else {
                // 3b. 채팅방 있음: 가족 캘린더 모드
                Log.d("HomeViewModel", "채팅방($chatRoomId) 발견. [가족 캘린더] 모드로 시작.")
                // 3b-1. '가족 구성원 전체' 생일 불러오기
                val familyMembers = repository.getFamilyMembers(chatRoomId)
                val familyBirthdayEvents = createBirthdayEvents(familyMembers)
                _uiState.update { it.copy(birthdayEventsByDate = familyBirthdayEvents) }

                // 3b-2. '가족 일정'을 실시간 구독
                // repository -> calendarManager 직접 호출
                calendarEventsListener = calendarManager.listenForEvents(chatRoomId) { familyEvents -> // familyEvents: List<CalendarEvent>
                    // Type Mismatch 해결: List -> Map으로 변환
                    val familyEventsMap = familyEvents.groupBy {
                        it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    // 가족 일정 + 가족 생일 병합
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
            // String?을 Question? 객체로 변환
            val newQuestion = newQuestionString?.let { Question(id = UUID.randomUUID().toString(), text = it) }
            _uiState.value = _uiState.value.copy(aiQuestion = newQuestion)
        }
    }

    /**
     * UI에서 받은 이벤트를 가족 캘린더에 저장
     */
    fun addOrUpdateEvent(event: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid

        // If-Null foldable 경고 해결
        if (currentUserId == null || currentChatRoomId == null) return

        viewModelScope.launch {
            // 개인 캘린더 저장 로직 (else) 삭제
            // repository -> calendarManager 직접 호출
            calendarManager.addEvent(currentChatRoomId, event)
        }
    }

    /**
     * 이벤트를 '단독 D-Day'로 설정 (HomeScreen.kt에서 필요)
     */
    fun setExclusiveDDay(newEvent: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid
        // 채팅방 없으면(null) 즉시 리턴
        if (currentUserId == null || currentChatRoomId == null) return

        viewModelScope.launch {
            // 1. 현재 D-Day로 설정된 다른 모든 이벤트(newEvent 제외)를 가져옴
            val oldDDayEvents = _uiState.value.eventsByDate.values.flatten()
                .filter { it.id != newEvent.id && it.isPriority }

            // 2. 다른 모든 이벤트의 스위치를 끔 (isPriority=false, prioritySetAt=null)
            for (oldEvent in oldDDayEvents) {
                val updatedOldEvent = oldEvent.copy(
                    isPriority = false,
                    prioritySetAt = null
                )
                // 개인 캘린더 저장 로직 (else) 삭제
                // repository -> calendarManager 직접 호출
                calendarManager.addEvent(currentChatRoomId, updatedOldEvent)
            }

            // 3. 마지막으로 새로 D-Day로 설정한 이벤트 저장
            // 개인 캘린더 저장 로직 (else) 삭제
            // repository -> calendarManager 직접 호출
            calendarManager.addEvent(currentChatRoomId, newEvent)
        }
    }


    // UI에서 요청한 이벤트를 가족 캘린더에서 삭제

    fun deleteEvent(event: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid

        // If-Null foldable 경고 해결
        if (currentUserId == null || currentChatRoomId == null) return

        viewModelScope.launch {
            // 개인 캘린더 삭제 로직 (else) 삭제
            // repository -> calendarManager 직접 호출
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

        // 1. D-1 생일이 있는지 최우선으로 확인
        val d1Birthday = allFutureEvents.find { event ->
            val eventDate = event.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            event.type == "BIRTHDAY" && eventDate.isEqual(tomorrow)
        }

        if (d1Birthday != null) {
            // D-1 생일이 있으면 다른 D-Day 설정 무시하고 즉시 반환
            return Pair("D-1", d1Birthday.title)
        }

        // 2. D-1 생일이 없을 경우, 사용자가 설정한 D-Day 확인
        val priorityEvents = allFutureEvents
            .filter { it.isPriority && it.prioritySetAt != null }

        // 3. 가장 '최근에 켠' D-Day를 찾음 (날짜가 가까운 순이 아님)
        val latestPriorityEvent = priorityEvents
            .maxByOrNull { it.prioritySetAt!!.seconds }

        // 4. 최신 D-Day가 있으면 그것을 사용, 없으면 가장 가까운 일정 사용
        val closestEvent = latestPriorityEvent ?: allFutureEvents.minByOrNull { it.startTime.seconds }

        // If-Null foldable 경고 해결
        return closestEvent?.let {
            val eventDate = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val dDay = ChronoUnit.DAYS.between(today, eventDate)

            val dDayText = if (dDay == 0L) "D-Day" else "D-${dDay}"
            val dDayTitle = it.title
            Pair(dDayText, dDayTitle)
        } ?: Pair("D-Day", "다가오는 일정이 없어요")
    }

    /**
     * User 목록을 기반으로 올해의 생일 CalendarEvent 맵을 생성
     */
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


                    // 양력도 이미 지났으면 내년 생일로 계산
                    if (thisYearBirthday.isBefore(today.minusDays(1))) { // 오늘 이전이면
                        thisYearBirthday = thisYearBirthday.plusYears(1)
                    }

                    // title = "${member.name} 님의 생일" 삭제

                    // 7. CalendarEvent 객체 생성
                    val birthdayEvent = CalendarEvent(
                        id = "birthday_${member.uid}",
                        title = title,
                        description = "${birthDate.monthValue}월 ${birthDate.dayOfMonth}일", // [수정] description
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

        // 8. 생성된 모든 생일 이벤트를 날짜(LocalDate)별로 그룹화
        return events.groupBy {
            it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    /**
     * 두 개의 이벤트 맵(일반 일정, 생일 일정)을 하나로 병합
     */
    private fun mergeEventMaps(
        regularEvents: Map<LocalDate, List<CalendarEvent>>,
        birthdayEvents: Map<LocalDate, List<CalendarEvent>>
    ): Map<LocalDate, List<CalendarEvent>> {
        // 두 맵의 모든 고유한 날짜 키(LocalDate)를 합침
        val allKeys = regularEvents.keys + birthdayEvents.keys

        // 병합된 새 맵을 생성
        return allKeys.associateWith { date ->
            // 각 날짜(key)에 대해, 두 맵의 리스트(value)를 합침
            (regularEvents[date] ?: emptyList()) + (birthdayEvents[date] ?: emptyList())
        }
    }


    /**
     * ViewModel이 소멸될 때 Firestore 리스너를 반드시 제거하여 메모리 누수 방지
     */
    override fun onCleared() {
        super.onCleared()
        calendarEventsListener?.remove()
    }

    /**
     * AppRepository의 acceptInvitation을 호출하고,
     * 성공 시 반환된 chatRoomId를 콜백으로 전달합
     */
    fun acceptInvitation(invitationId: String, onResult: (chatRoomId: String?) -> Unit) {
        viewModelScope.launch {
            // "Variable declaration could be moved into 'when'" 경고 해결
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

    // migratePersonalEventsToFamilyRoom 함수 삭제
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
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),

    // 생일 이벤트를 별도로 저장할 맵
    val birthdayEventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()
)