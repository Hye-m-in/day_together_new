package com.example.day_together.ui.home


import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.model.Anniversary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.data.repository.AppRepository
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

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 리스너 변수 이름 변경 (가족/개인 공용)
    private var calendarEventsListener: ListenerRegistration? = null

    init {
        loadInitialData()
    }

    // 'public'으로 유지 (초대 수락 시 MainActivity에서 호출해야 함)
    fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }

        // 리스너가 남아있으면 제거 (개인 -> 가족 전환 시 중복 방지)
        calendarEventsListener?.remove()

        viewModelScope.launch {
            // 1. 기본 정보 로드 (사용자, 질문, 명언)
            val user = repository.getCurrentUser()
            val question = repository.getTodaysQuestion()
            val quote = repository.getFamilyQuote()
            _uiState.update { it.copy(user = user, aiQuestion = question, familyQuote = quote) }

            if (user == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // 2. 채팅방 ID 탐색
            val chatRoomId = repository.findUserChatRoomId(user.uid)
            _uiState.update { it.copy(chatRoomId = chatRoomId) } // chatRoomId가 null이든 아니든 state에 저장

            // 3. 분기 처리
            if (chatRoomId == null) {
                // 3a. 채팅방 없음: 개인 캘린더 모드
                Log.d("HomeViewModel", "채팅방 없음. [개인 캘린더] 모드로 시작.")
                // 3a-1. 내 생일만 불러옴
                val myBirthdayEvents = createBirthdayEvents(listOf(user))
                _uiState.update { it.copy(birthdayEventsByDate = myBirthdayEvents) }

                // 3a-2. 개인 일정 실시간 구독
                calendarEventsListener = repository.listenForPersonalEvents(user.uid) { personalEvents ->
                    // 개인 일정 + 내 생일 병합
                    val mergedEvents = mergeEventMaps(personalEvents, myBirthdayEvents)
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

            } else {
                // 3b. 채팅방 있음: 가족 캘린더 모드
                Log.d("HomeViewModel", "채팅방($chatRoomId) 발견. [가족 캘린더] 모드로 시작.")
                // 3b-1. '가족 구성원 전체' 생일 불러오기
                val familyMembers = repository.getFamilyMembers(chatRoomId)
                val familyBirthdayEvents = createBirthdayEvents(familyMembers)
                _uiState.update { it.copy(birthdayEventsByDate = familyBirthdayEvents) }

                // 3b-2. 가족 일정 실시간 구독
                calendarEventsListener = repository.listenForCalendarEvents(chatRoomId) { familyEvents ->
                    // 가족 일정 + 가족 생일 병합
                    val mergedEvents = mergeEventMaps(familyEvents, familyBirthdayEvents)
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
            val newQuestion = repository.getTodaysQuestion()
            _uiState.value = _uiState.value.copy(aiQuestion = newQuestion)
        }
    }

    /**
     * UI에서 받은 이벤트를 상황(개인/가족)에 맞게 저장
     */
    fun addOrUpdateEvent(event: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid

        if (currentUserId == null) return // 사용자가 없으면 아무것도 안 함

        viewModelScope.launch {
            if (currentChatRoomId != null) {
                // 가족 캘린더에 저장
                repository.addOrUpdateCalendarEvent(currentChatRoomId, event)
            } else {
                // 개인 캘린더에 저장
                repository.addOrUpdatePersonalEvent(currentUserId, event)
            }
        }
    }

    /**
     * D-Day 스위치 로직 (가족 전용)
     * (개인 캘린더 모드에서는 D-Day 스위치를 '켜는' 로직이 약간 다를 수 있으나,우선 가족 캘린더와 동일하게 배타적(exclusive)으로 동작하도록 구현)
     */
    fun setExclusiveDDay(newEvent: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid
        if (currentUserId == null) return

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
                // 상황에 맞게 저장
                if (currentChatRoomId != null) {
                    repository.addOrUpdateCalendarEvent(currentChatRoomId, updatedOldEvent)
                } else {
                    repository.addOrUpdatePersonalEvent(currentUserId, updatedOldEvent)
                }
            }

            // 3. 마지막으로 새로 D-Day로 설정한 이벤트 저장, 상황에 맞게 저장
            if (currentChatRoomId != null) {
                repository.addOrUpdateCalendarEvent(currentChatRoomId, newEvent)
            } else {
                repository.addOrUpdatePersonalEvent(currentUserId, newEvent)
            }
        }
    }

    /**
     * UI에서 요청한 이벤트를 상황(개인/가족)에 맞게 삭제
     */
    fun deleteEvent(event: CalendarEvent) {
        val currentChatRoomId = _uiState.value.chatRoomId
        val currentUserId = _uiState.value.user?.uid

        if (currentUserId == null) return

        viewModelScope.launch {
            if (currentChatRoomId != null) {
                // 가족 캘린더에서 삭제
                repository.deleteCalendarEvent(currentChatRoomId, event.id)
            } else {
                // 개인 캘린더에서 삭제
                repository.deletePersonalEvent(currentUserId, event.id)
            }
        }
    }

    /**
     * 개인 일정을 가족 캘린더로 이전하는 함수
     */
    fun migratePersonalEventsToFamilyRoom(newChatRoomId: String) {
        val currentUserId = _uiState.value.user?.uid
        if (currentUserId == null) return

        viewModelScope.launch {
            val success = repository.migratePersonalEventsToFamilyRoom(currentUserId, newChatRoomId)
            if (success) {
                // 이전이 성공하면, HomeViewModel의 데이터를 [가족 캘린더] 모드로 새로고침
                loadInitialData()
            }
        }
    }


    // D-Day 계산 로직
    private fun calculateDDayInfo(events: Map<LocalDate, List<CalendarEvent>>): Pair<String, String> {
        val today = LocalDate.now()
        val allFutureEvents = events.values.flatten().filter {
            val eventDate = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            eventDate.isAfter(today) || eventDate.isEqual(today)
        }

        // 1. D-Day 스위치가 켜진(isPriority=true) 미래 이벤트 목록을 찾음
        val priorityEvents = allFutureEvents.filter { it.isPriority }

        // 2. 그 중에서 가장 최근에 스위치를 켠(=prioritySetAt이 가장 큰) 이벤트를 찾음
        val latestPriorityEvent = priorityEvents
            .filter { it.prioritySetAt != null }
            .maxByOrNull { it.prioritySetAt!!.seconds }

        // 3. 가장 최근에 켠 D-Day가 있으면 그것을 사용하고(latestPriorityEvent), 없으면 모든 미래 일정 중 가장 가까운 일정을 사용
        val closestEvent = latestPriorityEvent ?: allFutureEvents.minByOrNull { it.startTime.seconds }

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
     * User 목록을 기반으로 [올해]의 생일 CalendarEvent 맵 생성
     */
    // 음력 날짜 보정 로직 포함
    private fun createBirthdayEvents(members: List<User>): Map<LocalDate, List<CalendarEvent>> {
        val events = mutableListOf<CalendarEvent>()
        val today = LocalDate.now()
        // 회원가입 시 "YYYYMMDD" 형식으로 저장했으므로 해당 포맷 사용
        val birthDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        for (member in members) {
            val birthDateStr = member.birthDate
            try {
                // 생년월일 정보가 있고, 8자리일 경우에만 처리
                if (birthDateStr != null && birthDateStr.length == 8) {

                    val isLunar = member.isLunar == true
                    // 문자열을 LocalDate로 파싱 (원래 생일)
                    val birthDate = LocalDate.parse(birthDateStr, birthDateFormatter)

                    var thisYearBirthday: LocalDate
                    val title: String
                    val lunarMonth = birthDate.monthValue - 1 // 음력 월 (0-based)
                    var lunarDay = birthDate.dayOfMonth       // 음력 일 (1-based)

                    if (isLunar) {
                        // 음력 생일 계산
                        val lunarCal = ChineseCalendar()
                        lunarCal.clear() // 필드 초기화

                        // 기준이 되는 태양력 연도를 설정 (올해)
                        lunarCal.set(Calendar.YEAR, today.year)
                        // 찾고 싶은 음력 월 설정
                        lunarCal.set(ChineseCalendar.MONTH, lunarMonth)

                        // 이 달의 마지막 날짜 확인
                        val maxDay = lunarCal.getActualMaximum(ChineseCalendar.DAY_OF_MONTH)

                        // 만약 저장된 날짜(예: 30)가 이 달의 마지막 날(예: 29)보다 크다면, 마지막 날(29)로 보정
                        if (lunarDay > maxDay) {
                            lunarDay = maxDay
                        }
                        lunarCal.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)

                        // 위 음력 날짜에 해당하는 태양력 날짜를 가져옴
                        lunarCal.get(Calendar.YEAR) // 강제 재계산
                        thisYearBirthday = lunarCal.time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

                        // 만약 계산된 날짜가 오늘보다 너무 이르면 (예: 올해 음력 생일이 이미 지났음)
                        // 내년 음력 생일로 계산(약 30일 이전이라면 내년으로)
                        if (thisYearBirthday.isBefore(today.minusDays(30))) {
                            lunarCal.clear()
                            lunarCal.set(Calendar.YEAR, today.year + 1) // 내년으로
                            lunarCal.set(ChineseCalendar.MONTH, lunarMonth)

                            // 내년의 마지막 날짜도 다시 확인
                            val nextYearMaxDay = lunarCal.getActualMaximum(ChineseCalendar.DAY_OF_MONTH)
                            lunarDay = birthDate.dayOfMonth // 원본 날짜로 리셋
                            if (lunarDay > nextYearMaxDay) {
                                lunarDay = nextYearMaxDay
                            }
                            lunarCal.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)

                            lunarCal.get(Calendar.YEAR) // 강제 재계산
                            thisYearBirthday = lunarCal.time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                        }

                        title = "${member.name} 님의 생일(음력)"

                    } else {
                        // 양력 생일 계산
                        thisYearBirthday = birthDate.withYear(today.year)

                        // 양력도 이미 지났으면 내년 생일로 계산
                        if (thisYearBirthday.isBefore(today.minusDays(1))) { // 오늘 이전이면
                            thisYearBirthday = thisYearBirthday.plusYears(1)
                        }

                        title = "${member.name} 님의 생일(양력)"
                    }

                    // CalendarEvent 객체 생성
                    val birthdayEvent = CalendarEvent(
                        id = "birthday_${member.uid}", // 고유하고 안정적인 ID 부여
                        title = title, // (음력/양력)이 포함된 제목
                        description = if (isLunar) "음력 ${lunarMonth+1}월 ${lunarDay}일" else "양력 ${birthDate.monthValue}월 ${birthDate.dayOfMonth}일", // 상세 설명
                        // LocalDate를 Timestamp로 변환
                        startTime = Timestamp(Date.from(thisYearBirthday.atStartOfDay(ZoneId.systemDefault()).toInstant())),
                        endTime = null, // 하루 종일 이벤트
                        creatorId = "SYSTEM_BIRTHDAY", // 시스템이 생성
                        creatorName = "가족 캘린더",
                        type = "BIRTHDAY", // Anniversary 모델의 type 활용
                        isPriority = false // 생일을 D-Day 기본값으로는 설정하지 않음
                    )
                    events.add(birthdayEvent)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "${member.name}님의 생일($birthDateStr) 변환 중 오류", e)
            }
        }

        // 생성된 모든 생일 이벤트를 날짜(LocalDate)별로 그룹화
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

        // 병합된 새 맵 생성
        return allKeys.associateWith { date ->
            // 각 날짜(key)에 대해, 두 맵의 리스트(value)를 합침 -> 해당 날짜에 이벤트가 없으면 null이므로 emptyList()로 처리
            (regularEvents[date] ?: emptyList()) + (birthdayEvents[date] ?: emptyList())
        }
    }


    /**
     * ViewModel이 소멸될 때 Firestore 리스너를 반드시 제거하여 메모리 누수 방지
     */
    override fun onCleared() {
        super.onCleared()
        // 변수명 변경
        calendarEventsListener?.remove()
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
    val eventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap(),

    // 생일 이벤트를 별도로 저장할 맵
    val birthdayEventsByDate: Map<LocalDate, List<CalendarEvent>> = emptyMap()
)