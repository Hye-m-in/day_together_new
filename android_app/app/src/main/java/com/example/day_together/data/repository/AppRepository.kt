package com.example.day_together.data.repository

import com.example.day_together.AuthManager
import com.example.day_together.CalendarManager
import com.example.day_together.ChatMessage
import com.example.day_together.ChatRoomManager
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.ui.gallery.MonthlyComment
import com.example.day_together.ui.gallery.PhotoItem
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import kotlin.coroutines.resume

/**
 * 앱의 모든 데이터 통신을 책임지는 통합 Repository 클래스
 * 각 ViewModel이 필요로 하는 모든 기능 제공 &  실제 백엔드 로직(AuthManager 등)과 ViewModel 사이의 중개인 역할
 */
class AppRepository {

    // 실제 로직 담당 매니저들 선언
    private val authManager = AuthManager

    private val chatRoomManager = ChatRoomManager
    private val calendarManager = CalendarManager()


    /**
     * 이메일과 비밀번호로 로그인 요청
     */
    suspend fun login(email: String, password: String): AuthResult {
        return suspendCancellableCoroutine { continuation ->
            authManager.loginUser(email, password) { success, errorMessage ->
                if (continuation.isActive) {
                    if (success) continuation.resume(AuthResult.Success)
                    else continuation.resume(AuthResult.Failure(errorMessage ?: "알 수 없는 오류"))
                }
            }
        }
    }

    /**
     * 사용자 정보로 회원가입 요청
     */
    suspend fun signUp(name: String, email: String, password: String): AuthResult {
        val defaultPosition = "가족" // 회원가입 시 기본 역할
        return suspendCancellableCoroutine { continuation ->
            authManager.registerUser(name, email, password, defaultPosition) { success, errorMessage ->
                if (continuation.isActive) {
                    if (success) continuation.resume(AuthResult.Success)
                    else continuation.resume(AuthResult.Failure(errorMessage ?: "회원가입 실패"))
                }
            }
        }
    }

    /**
     * 로그아웃을 처리
     */
    fun logout() {
        authManager.logoutUser()
    }

    /**
     * 현재 로그인된 사용자의 프로필 정보를 가져옴
     * TODO: 백엔드에 현재 사용자 정보를 Firestore에서 가져오는 기능 구현 및 연결 필요
     */
    suspend fun getCurrentUser(): User? {
        delay(200) // 가짜 딜레이
        val uid = authManager.getCurrentUserId()
        return if (uid != null) {
            User(uid = uid, name = "테스트 유저", email = "test@example.com", position = "아들")
        } else {
            null
        }
    }

    /**
     * 수정된 사용자 정보 DB에 업데이트
     * TODO: 백엔드에 사용자 정보를 Firestore에 업데이트하는 기능 구현 및 연결 필요
     */
    suspend fun updateUser(updatedUser: User) {
        println("TODO: DB에 사용자 정보 업데이트: $updatedUser")
        delay(500)
    }

    /**
     * 비밀번호를 변경
     * TODO: 백엔드에 비밀번호 변경 기능 구현 및 연결 필요
     */
    suspend fun changePassword(email: String, newPassword: String) {
        println("TODO: DB에 비밀번호 변경 요청: $email")
        delay(500)
    }

    /**
     * 비밀번호 재설정 요청
     * TODO: 백엔드에 비밀번호 재설정 이메일 전송 기능 구현 및 연결 필요
     */
    suspend fun resetPassword(email: String): AuthResult {
        println("TODO: 비밀번호 재설정 이메일 전송 요청: $email")
        delay(1000)
        return AuthResult.Success
    }

    /**
     * 아이디(이메일) 찾기
     * TODO: 백엔드에 이름/생년월일 등으로 이메일을 찾아주는 기능 구현 및 연결 필요
     */
    suspend fun findId(name: String, email: String): AuthResult {
        println("TODO: 아이디 찾기 요청: $name, $email")
        delay(1000)
        return AuthResult.Success
    }

    suspend fun getUser(userId: String): User? {
        println("TODO: 특정 사용자 정보 가져오기: $userId")
        return User(uid = userId, name = "가족 구성원", email = "family@example.com")
    }


    // HomeViewModel

    /**
     * 오늘의 질문을 가져옴
     * TODO: 백엔드에 오늘의 질문을 가져오는 기능 구현 및 연결 필요
     */
    suspend fun getTodaysQuestion(): Question {
        delay(300)
        return Question(id = "q1", text = "우리 가족만의 특별한 루틴이 있나요?")
    }

    /**
     * 가족 명언을 가져옴
     * TODO: 백엔드에 명언을 가져오는 기능 구현 및 연결 필요
     */
    suspend fun getFamilyQuote(): String {
        delay(200)
        return "\"가족 사랑은 평화의 시작이다.\""
    }

    /**
     * 캘린더의 모든 이벤트를 가져옴
     * TODO: CalendarManager에 캘린더 이벤트를 가져오는 함수(예: getEvents)를 추가하고 연결해야 함
     */
    suspend fun getCalendarEvents(): Map<LocalDate, List<CalendarEvent>> {
        delay(600)
        val today = LocalDate.now()
        // 현재 CalendarManager에는 이벤트 추가 기능만 있으므로, 가져오기 기능은 임시 데이터 반환
        return mapOf(
            today.plusDays(3) to listOf(
                CalendarEvent(id = "1", description = "엄마 생일", date = today.plusDays(3), isPriority = true)
            ),
            today.plusDays(10) to listOf(
                CalendarEvent(id = "2", description = "가족 여행", date = today.plusDays(10))
            )
        )
    }

    // GalleryViewModel)


    /**
     * 갤러리의 모든 사진 목록을 가져옴
     * TODO: Firebase Storage 등에서 실제 사진 목록을 가져오도록 구현 필요
     */
    suspend fun getGalleryPhotos(): List<PhotoItem> {
        delay(800)
        return listOf(
            PhotoItem("p1", "https://picsum.photos/seed/202501/200/300", LocalDate.now().minusMonths(2).toString()),
            PhotoItem("p2", "https://picsum.photos/seed/202503/200/300", LocalDate.now().minusMonths(1).toString()),
            PhotoItem("p3", "https://picsum.photos/seed/202504/200/300", LocalDate.now().toString())
        )
    }

    /**
     * 특정 월의 댓글 목록을 가져옴(임시 데이터 생성)
     * TODO: Firestore에서 실제 댓글 목록을 가져오도록 구현 필요
     */
    suspend fun getMonthlyComments(yearMonth: YearMonth): List<MonthlyComment> {
        delay(400)
        return listOf(
        )
    }

    /**
     * 새로운 댓글 추가
     * TODO: Firestore에 실제 댓글을 저장하도록 구현 필요
     */
    suspend fun addMonthlyComment(yearMonth: YearMonth, comment: MonthlyComment) {
        delay(500)
        println("TODO: ${yearMonth}에 댓글 추가 - ${comment.text}")
    }


    // MessageViewModel

    /**
     * 특정 채팅방의 메시지 목록 가져옴
     */
    suspend fun getChatMessages(chatRoomId: String): List<ChatMessage> {
        return suspendCancellableCoroutine { continuation ->
            chatRoomManager.db.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }
                    if (continuation.isActive) continuation.resume(messages)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resume(emptyList()) // 실패 시 빈 리스트 반환
                }
        }
    }

    /**
     * 새로운 채팅 메시지 전송
     */
    fun sendMessage(chatRoomId: String, text: String, sender: String) {
        if (text.isBlank() || sender.isBlank()) return

        val message = hashMapOf(
            "sender" to sender,
            "content" to text,
            "timestamp" to Date()
        )
        chatRoomManager.db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
    }



    // SettingsViewModel


    /**
     * 현재 설정 값을 Flow로 제공
     * TODO: Firestore 등에서 실제 사용자 설정 값을 가져와 Flow로 제공하도록 구현 필요
     */
    fun getSettingsFlow(): Flow<UserSettings> {
        // 임시로 가짜 데이터 담은 Flow 반환
        return flowOf(
            UserSettings(
                questionFrequency = "주3회",
                questionTime = "오후",
                notificationEnabled = true,
                vibrationEnabled = false
            )
        )
    }

    /**
     * 변경된 설정을 저장
     * TODO: Firestore 등 실제 DB에 설정을 저장하도록 구현 필요
     */
    suspend fun saveSettings(newSettings: UserSettings) {
        delay(200)
        println("TODO: DB에 설정 저장 - $newSettings")
    }


}




/**
 * 인증 결과를 나타내는 Sealed Class
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