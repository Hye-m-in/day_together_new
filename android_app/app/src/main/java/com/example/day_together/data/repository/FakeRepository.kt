package com.example.day_together.data.repository

import com.example.day_together.data.model.Anniversary
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.ChatMessage
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.ui.gallery.MonthlyComment
import com.example.day_together.ui.gallery.PhotoItem
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.time.LocalDate
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 인증 결과를 나타내는 Sealed Class
 * 성공(Success) 또는 실패(Failure) 상태를 가지며, 실패 시 에러 메시지를 포함
 */
sealed class AuthResult {
    object Success : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

/**
 * 사용자 설정 정보를 담는 데이터 클래스
 */
data class UserSettings(
    val questionFrequency: String,
    val questionTime: String,
    val notificationEnabled: Boolean,
    val vibrationEnabled: Boolean
)


/**
 * UI 테스트나 백엔드 API가 준비되지 않았을 때 사용하는 가짜 데이터 저장소 클래스
 * 실제 네트워크 통신 없이 미리 정의된 데이터를 반환하며, `delay`를 통해 비동기 동작 흉내
 */
class FakeRepository {

    // MARK: - 가짜 데이터베이스 및 상태
    // 이메일을 키로 사용하는 가짜 유저 데이터베이스 (이메일 -> <비밀번호, 유저 정보>)
    private val fakeUserDB = mutableMapOf<String, Pair<String, User>>()

    // 현재 로그인된 사용자를 저장하기 위한 변수
    private var currentUser: User? = null

    private var isFakeLoggedIn = false

    private val fakeQuestions = listOf(
        Question(id = "q1", text = "우리 가족만의 특별한 루틴이 있나요?"),
        Question(id = "q2", text = "가장 기억에 남는 가족 여행은 어디였나요?"),
        Question(id = "q3", text = "서로에게 가장 고마웠던 순간은 언제인가요?")
    )
    private var questionIndex = 0

    init {
        val initialUser = User(
            uid = "daytogether",
            name = "테스트 유저",
            email = "daytogether@test.com",
            position = "테스터",
            profileImageUrl = ""
        )
        fakeUserDB["daytogether@test.com"] = Pair("daytogether", initialUser)
    }

    // MARK: 데이터 조회 함수

    /** ★★★ [추가] 현재 로그인된 사용자 정보를 반환하는 함수 ★★★ */
    suspend fun getCurrentUser(): User? {
        delay(100L) // 실제 네트워크 통신을 흉내 내기 위한 딜레이
        return currentUser
    }

    suspend fun getUser(userId: String): User? {
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

    fun getFakeLoginStatus(): Boolean {
        return isFakeLoggedIn
    }

    /** 이메일과 비밀번호로 로그인 처리 흉내 */
    suspend fun login(email: String, password: String): AuthResult {
        delay(1000L)
        val userEntry = fakeUserDB[email]
        return if (userEntry != null && userEntry.first == password) {
            isFakeLoggedIn = true
            // ★★★ [수정] 로그인 성공 시, 현재 사용자 정보를 currentUser 변수에 저장 ★★★
            currentUser = userEntry.second
            AuthResult.Success
        } else {
            AuthResult.Failure("이메일 또는 비밀번호가 올바르지 않습니다.")
        }
    }

    /** 신규 사용자 회원가입 처리 흉내 */
    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        delay(1500L)
        if (fakeUserDB.containsKey(email)) {
            return AuthResult.Failure("이미 사용 중인 이메일입니다.")
        }
        val newUser = User(uid = "newUser_${System.currentTimeMillis()}", name = name, email = email, position = "새로운 유저")
        fakeUserDB[email] = Pair(password, newUser)
        // ★★★ [수정] 회원가입 성공 시, 새로 만든 사용자 정보를 currentUser 변수에 저장 ★★★
        currentUser = newUser
        return AuthResult.Success
    }

    /** 로그아웃 처리 흉내 */
    fun logout() {
        isFakeLoggedIn = false
        // ★★★ [수정] 로그아웃 시, 저장된 현재 사용자 정보를 초기화 ★★★
        currentUser = null
    }

    suspend fun resetPassword(email: String): AuthResult {
        delay(1000L)
        return if (fakeUserDB.containsKey(email)) {
            AuthResult.Success
        } else {
            AuthResult.Failure("가입되지 않은 이메일입니다.")
        }
    }

    suspend fun findId(name: String, email: String): AuthResult {
        delay(1000L)
        val userEntry = fakeUserDB[email]
        return if (userEntry != null && userEntry.second.name == name) {
            AuthResult.Success
        } else {
            AuthResult.Failure("일치하는 사용자 정보가 없습니다.")
        }
    }

    suspend fun getGalleryPhotos(): List<PhotoItem> {
        delay(500L)
        return listOf(
            PhotoItem("s1", "https://picsum.photos/seed/202401/200/300", YearMonth.of(2024, 1).atDay(10).toString()),
            PhotoItem("s2", "https://picsum.photos/seed/202403/200/300", YearMonth.of(2024, 3).atDay(5).toString()),
            PhotoItem("s3", "https://picsum.photos/seed/current_prev/200/300", YearMonth.now().minusMonths(1).atDay(15).toString()),
            PhotoItem("s4", "https://picsum.photos/seed/current/200/300", YearMonth.now().atDay(1).toString()),
            PhotoItem("s5", "https://picsum.photos/seed/current_plus1/200/300", YearMonth.now().plusMonths(1).atDay(20).toString())
        )
    }

    suspend fun getMonthlyComments(yearMonth: YearMonth): List<MonthlyComment> {
        delay(300L)
        return listOf(
            MonthlyComment(author = "엄마", text = "행복했던 ${yearMonth.monthValue}월!", timestamp = "2시간 전"),
            MonthlyComment(author = "오빠", text = "이번 달이 벌써 끝나간다니.. 시간 너무 빠르다", timestamp = "2시간 전"),
            MonthlyComment(author = "아빠", text = "앞으로도 행복한 일만 가득하길~", timestamp = "1시간 전")
        )
    }

    suspend fun addMonthlyComment(yearMonth: YearMonth, comment: MonthlyComment) {
        delay(200L)
        println("${yearMonth}월 댓글 작성: ${comment.text}")
    }

    private val fakeSettings = UserSettings(
        questionFrequency = "주3회",
        questionTime = "오후",
        notificationEnabled = true,
        vibrationEnabled = false
    )

    private val _settingsFlow = MutableStateFlow(fakeSettings)

    fun getSettingsFlow() = _settingsFlow.asStateFlow()

    suspend fun saveSettings(newSettings: UserSettings) {
        delay(200L)
        println("설정 저장 완료: $newSettings")
        _settingsFlow.value = newSettings
    }

    /** 수정된 사용자 정보를 받아 가짜 DB에 업데이트 */
    suspend fun updateUser(updatedUser: User) {
        delay(300L)
        val userEntry = fakeUserDB[updatedUser.email]
        if (userEntry != null) {
            fakeUserDB[updatedUser.email] = userEntry.first to updatedUser
            // 정보 수정 시, 현재 로그인된 사용자 정보도 함께 업데이트
            if (currentUser?.uid == updatedUser.uid) {
                currentUser = updatedUser
            }
            println("사용자 정보 업데이트 성공: 역할=${updatedUser.position}, 이미지=${updatedUser.profileImageUrl}")
        }
    }

    suspend fun changePassword(email: String, newPassword: String) {
        delay(500L)
        val userEntry = fakeUserDB[email]
        if (userEntry != null) {
            fakeUserDB[email] = newPassword to userEntry.second
            println("$email 사용자의 비밀번호 변경 성공")
        }
    }
}
