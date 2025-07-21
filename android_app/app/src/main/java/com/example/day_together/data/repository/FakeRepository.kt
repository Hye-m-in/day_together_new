package com.example.day_together.data.repository

import com.example.day_together.data.model.Anniversary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.ChatMessage
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.ui.auth.AuthResult
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Date

/**
 * UI 테스트나 백엔드 API가 준비되지 않았을 때 사용하는 가짜 데이터 저장소 클래스
 * 실제 네트워크 통신 없이 미리 정의된 데이터를 반환하며, `delay`를 통해 비동기 동작을 흉내
 */
class FakeRepository {

    // MARK: - 가짜 데이터베이스 및 상태

    // 이메일을 키로 사용하는 가짜 유저 데이터베이스 (이메일 -> <비밀번호, 유저 정보>)
    private val fakeUserDB = mutableMapOf<String, Pair<String, User>>()
    // 가짜 로그인 상태를 저장하는 변수
    private var isFakeLoggedIn = false

    // 미리 정의된 오늘의 질문 목록
    private val fakeQuestions = listOf(
        Question(id = "q1", text = "우리 가족만의 특별한 루틴이 있나요?"),
        Question(id = "q2", text = "가장 기억에 남는 가족 여행은 어디였나요?"),
        Question(id = "q3", text = "서로에게 가장 고마웠던 순간은 언제인가요?")
    )
    // 다음 질문을 가져오기 위한 인덱스
    private var questionIndex = 0

    init {
        // 클래스 생성 시 초기 테스트 유저 추가
        val initialUser = User(uid = "daytogether", name = "테스트 유저", email = "daytogether@test.com", position = "테스터")
        fakeUserDB["daytogether@test.com"] = Pair("daytogether", initialUser)
    }

    // MARK: - 데이터 조회 함수

    /** 주어진 ID에 해당하는 사용자 정보를 반환 */
    suspend fun getUser(userId: String): User? {
        // 실제 uid로 유저를 찾음
        return fakeUserDB.values.find { it.second.uid == userId }?.second
    }

    /** 오늘의 질문 순환하며 반환 */
    suspend fun getTodaysQuestion(): Question {
        delay(300L) // 네트워크 딜레이 흉내
        val question = fakeQuestions[questionIndex]
        questionIndex = (questionIndex + 1) % fakeQuestions.size // 다음 질문을 위해 인덱스 순환
        return question
    }

    /** 오늘의 명언을 반환 */
    suspend fun getFamilyQuote(): String {
        delay(200L)
        return "\"가족 사랑은 평화의 시작이다.\""
    }

    /** 다가오는 기념일 정보 반환*/
    suspend fun getUpcomingAnniversary(): Anniversary {
        delay(400L)
        val today = LocalDate.now()
        val anniversaryDate = today.plusDays(3)
        val date = Date.from(anniversaryDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())

        return Anniversary(
            id = "anniv1",
            title = "엄마 생일",
            date = date.time,
            type = "BIRTHDAY"
        )
    }

    /** 날짜별 캘린더 이벤트 목록 반환 */
    suspend fun getCalendarEvents(): Map<LocalDate, List<CalendarEvent>> {
        delay(600L)
        val today = LocalDate.now()
        return mapOf(
            today.plusDays(1) to listOf(
                CalendarEvent(id = "1", description = "점심 약속", date = today.plusDays(1)),
                CalendarEvent(id = "2", description = "프로젝트 회의", date = today.plusDays(1))
            ),
            today to listOf(
                CalendarEvent(id = "3", description = "오늘 할 일!", date = today)
            )
        )
    }

    /** 특정 채팅방의 메시지 목록을 반환 */
    suspend fun getChatMessages(chatRoomId: String): List<ChatMessage> {
        delay(1500L) // 채팅은 로딩이 긴 것처럼 흉내
        return listOf(
            ChatMessage(content = "첫 번째 테스트 메시지", sender = "상대방", timestamp = Date()),
            ChatMessage(content = "첫번째 테스트 메시지 확인, 두번째 테스트 메시지", sender = "테스트 유저", timestamp = Date()),
            ChatMessage(content = "UI 연결 테스트 중", sender = "상대방", timestamp = Date())
        )
    }

    // MARK: - 인증(Auth) 관련 함수

    /** 현재 로그인 상태 반환 */
    fun getFakeLoginStatus(): Boolean {
        return isFakeLoggedIn
    }

    /** 이메일과 비밀번호로 로그인 처리 흉내*/
    suspend fun login(email: String, password: String): AuthResult {
        delay(1000L)
        val userEntry = fakeUserDB[email]
        return if (userEntry != null && userEntry.first == password) {
            isFakeLoggedIn = true
            AuthResult.Success
        } else {
            AuthResult.Failure("이메일 또는 비밀번호가 올바르지 않습니다.")
        }
    }

    /** 신규 사용자 회원가입 처리 흉내*/
    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        delay(1500L)
        if (fakeUserDB.containsKey(email)) {
            return AuthResult.Failure("이미 사용 중인 이메일입니다.")
        }
        val newUser = User(uid = "newUser_${System.currentTimeMillis()}", name = name, email = email, position = "새로운 유저")
        fakeUserDB[email] = Pair(password, newUser)
        return AuthResult.Success
    }

    /** 로그아웃 처리 흉내 */
    fun logout() {
        isFakeLoggedIn = false
    }

    /** 비밀번호 재설정 요청 처리 흉내 */
    suspend fun resetPassword(email: String): AuthResult {
        delay(1000L)
        return if (fakeUserDB.containsKey(email)) {
            // 실제로는 비밀번호 재설정 이메일을 보냄
            AuthResult.Success
        } else {
            AuthResult.Failure("가입되지 않은 이메일입니다.")
        }
    }

    /** 이름과 이메일로 아이디(이메일) 찾기 처리 흉내 */
    suspend fun findId(name: String, email: String): AuthResult {
        delay(1000L)
        val userEntry = fakeUserDB[email]
        return if (userEntry != null && userEntry.second.name == name) {
            AuthResult.Success
        } else {
            AuthResult.Failure("일치하는 사용자 정보가 없습니다.")
        }
    }
}