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

class FakeRepository {

    // 가짜 데이터베이스 및 로그인 상태 추가
    private val fakeUserDB = mutableMapOf<String, Pair<String, User>>() // "email" to <"password", User>
    private var isFakeLoggedIn = false

    init {
        // 초기 테스트 유저 추가
        val initialUser = User(uid = "daytogether", name = "테스트 유저", email = "daytogether@test.com", position = "테스터")
        fakeUserDB["daytogether@test.com"] = Pair("daytogether", initialUser)
    }

    private val fakeQuestions = listOf(
        Question(id = "q1", text = "우리 가족만의 특별한 루틴이 있나요?"),
        Question(id = "q2", text = "가장 기억에 남는 가족 여행은 어디였나요?"),
        Question(id = "q3", text = "서로에게 가장 고마웠던 순간은 언제인가요?")
    )
    private var questionIndex = 0

    suspend fun getUser(userId: String): User? {
        // 실제 uid로 유저 찾기
        return fakeUserDB.values.find { it.second.uid == userId }?.second
    }

    suspend fun getTodaysQuestion(): Question {
        delay(300L)
        val question = fakeQuestions[questionIndex]
        questionIndex = (questionIndex + 1) % fakeQuestions.size
        return question
    }

    suspend fun getFamilyQuote(): String {
        delay(200L)
        return "\"가족 사랑은 평화의 시작이다.\""
    }

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

    suspend fun getChatMessages(chatRoomId: String): List<ChatMessage> {
        delay(1500L)
        return listOf(
            ChatMessage(content = "첫 번째 테스트 메시지", sender = "상대방", timestamp = Date()),
            ChatMessage(content = "첫번째 테스트 메시지 확인, 두번째 테스트 메시지", sender = "테스트 유저", timestamp = Date()),
            ChatMessage(content = "UI 연결 테스트 중", sender = "상대방", timestamp = Date())
        )
    }

    // Auth 관련 가짜 함수
    fun getFakeLoginStatus(): Boolean {
        return isFakeLoggedIn
    }

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

    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        delay(1500L)
        if (fakeUserDB.containsKey(email)) {
            return AuthResult.Failure("이미 사용 중인 이메일입니다.")
        }
        val newUser = User(uid = "newUser_${System.currentTimeMillis()}", name = name, email = email, position = "새로운 유저")
        fakeUserDB[email] = Pair(password, newUser)
        return AuthResult.Success
    }

    fun logout() {
        isFakeLoggedIn = false
    }

    suspend fun resetPassword(email: String): AuthResult {
        delay(1000L)
        if (fakeUserDB.containsKey(email)) {
            return AuthResult.Success
        }
        return AuthResult.Failure("가입되지 않은 이메일입니다.")
    }

    // 아이디 찾기 함수 추가
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