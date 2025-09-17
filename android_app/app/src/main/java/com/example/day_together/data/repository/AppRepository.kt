package com.example.day_together.data.repository

import android.util.Log
import com.example.day_together.AuthManager
import com.example.day_together.CalendarManager
import com.example.day_together.ui.message.ChatMessage
import com.example.day_together.ChatRoomManager
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.data.model.Question
import com.example.day_together.data.model.User
import com.example.day_together.ui.gallery.MonthlyComment
import com.example.day_together.ui.gallery.PhotoItem

// Retrofit, DTO, Firebase, Gson 관련 import
import com.example.day_together.data.dto.ErrorResponse
import com.example.day_together.data.dto.NaverTokenRequest
import com.example.day_together.data.remote.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import retrofit2.HttpException


import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine

// Firebase Task를 Coroutine으로 사용하기 위한 import
import kotlinx.coroutines.tasks.await

import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import kotlin.coroutines.resume

/**
 * 앱의 모든 데이터 통신을 책임지는 통합 Repository 클래스
 * 각 ViewModel이 필요로 하는 모든 기능 제공 &  실제 백엔드 로직(AuthManager 등)과 ViewModel 사이의 중개인 역할
 */
object AppRepository {

    // 실제 로직 담당 매니저들 선언
    private val authManager = AuthManager
    private val chatRoomManager = ChatRoomManager
    private val calendarManager = CalendarManager()
    private val db = chatRoomManager.db // 편의를 위해 db 인스턴스 가져오기


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
     * 구글 ID 토큰으로 Firebase에 로그인 요청하는 함수 추가
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return suspendCancellableCoroutine { continuation ->
            authManager.signInWithGoogleCredential(idToken) { success, errorMessage ->
                if (continuation.isActive) {
                    if (success) continuation.resume(AuthResult.Success)
                    else continuation.resume(AuthResult.Failure(errorMessage ?: "구글 로그인 실패"))
                }
            }
        }
    }


    /**
     * 네이버 액세스 토큰으로 우리 서버에 로그인 요청을 보내고,
     * 받은 커스텀 토큰으로 Firebase에 최종 로그인하는 함수
     */
    suspend fun loginWithNaver(accessToken: String): AuthResult {
        return try {
            // (내용 동일)
            val request = NaverTokenRequest(accessToken = accessToken)
            val response = ApiClient.service.naverLogin(request)
            val customToken = response.customToken
            if (customToken.isBlank()) {
                return AuthResult.Failure("서버로부터 유효한 토큰을 받지 못했습니다.")
            }
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
            AuthResult.Success
        } catch (e: Exception) {

            // 1. Retrofit의 HTTP 오류인지, 그리고 상태 코드가 400인지 확인
            if (e is HttpException && e.code() == 400) {
                // 2. 서버가 보낸 JSON 형식의 에러 본문을 문자열로 변환
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    // 3. Gson을 사용해 에러 문자열을 ErrorResponse 객체로 파싱
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    // 4. 파싱된 객체에서 구체적인 에러 메시지를 추출하여 반환
                    val detailMessage = errorResponse.detail ?: "알 수 없는 400 오류입니다."
                    return AuthResult.Failure(detailMessage)
                } catch (jsonE: Exception) {
                    // 에러 본문 파싱 실패 시 (예: 서버가 JSON이 아닌 다른 형식으로 보냈을 때)
                    return AuthResult.Failure("서버 응답을 해석할 수 없습니다.")
                }
            }
            // 400 오류가 아닌 다른 모든 종류의 오류 처리 (예: 인터넷 연결 끊김 등)
            Log.e("AppRepository", "네이버 로그인 실패", e)
            return AuthResult.Failure("서버와 통신할 수 없습니다. 네트워크 상태를 확인해주세요.")
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
     * 로그아웃 처리
     */
    fun logout() {
        authManager.logoutUser()
    }

    /**
     * 현재 로그인된 사용자의 프로필 정보를 가져옴
     * TODO: 백엔드에 현재 사용자 정보를 Firestore에서 가져오는 기능 구현 및 연결 필요
     */
    suspend fun getCurrentUser(): User? {
        val uid = authManager.getCurrentUserId()
        return suspendCancellableCoroutine { continuation ->
            if (uid != null) {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (continuation.isActive) {
                            val name = document.getString("name") ?: "Unknown"
                            val email = document.getString("email") ?: "Unknown"
                            val position = document.getString("position") ?: "가족"
                            continuation.resume(User(uid = uid, name = name, email = email, position = position))
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
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
     * 비밀번호 변경
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
//            today.plusDays(3) to listOf(
//                CalendarEvent(id = "1", description = "엄마 생일", date = today.plusDays(3), isPriority = true)
//            ),
//            today.plusDays(10) to listOf(
//                CalendarEvent(id = "2", description = "가족 여행", date = today.plusDays(10))
//            )
        )
    }

    // GalleryViewModel

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
     * ChatActivity 로직 이전: 현재 로그인된 사용자의 이름을 가져옴
     */
    suspend fun getCurrentUserName(): String {
        return suspendCancellableCoroutine { continuation ->
            val uid = authManager.getCurrentUserId()
            if (uid == null) {
                if (continuation.isActive) continuation.resume("Unknown")
                return@suspendCancellableCoroutine
            }
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (continuation.isActive) {
                        continuation.resume(document.getString("name") ?: "Unknown")
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume("Unknown")
                }
        }
    }

    /**
     * ChatActivity 로직 이전: 사용자가 참여한 채팅방 ID를 찾음
     */
    suspend fun findUserChatRoomId(userId: String): String? {
        return suspendCancellableCoroutine { continuation ->
            // 1. 내가 초대받아 수락한 채팅방 찾기
            db.collection("users").document(userId).collection("invitations")
                .whereEqualTo("status", "accepted").limit(1).get()
                .addOnSuccessListener { documents ->
                    val acceptedRoomId = documents.firstOrNull()?.id
                    if (acceptedRoomId != null) {
                        if (continuation.isActive) continuation.resume(acceptedRoomId)
                        return@addOnSuccessListener
                    }

                    // 2. 내가 만든 채팅방 찾기
                    db.collection("chatRooms").whereArrayContains("members", userId).limit(1).get()
                        .addOnSuccessListener { chatRooms ->
                            val ownRoomId = chatRooms.firstOrNull()?.id
                            if (continuation.isActive) continuation.resume(ownRoomId)
                        }
                        .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                }
                .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
        }
    }

    /**
     * ChatActivity 로직 이전: 새로운 채팅방 생성함
     */
    suspend fun createNewChatRoom(inviterUserId: String): String? {
        return suspendCancellableCoroutine { continuation ->
            val newChatRoomRef = db.collection("chatRooms").document()
            val chatRoomId = newChatRoomRef.id
            val data = hashMapOf(
                "members" to listOf(inviterUserId),
                "createdAt" to Date()
            )
            newChatRoomRef.set(data)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(chatRoomId)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
    }

    /**
     * ChatActivity 로직 이전: 실시간으로 메시지 수신
     */
    fun listenForMessages(chatRoomId: String, onMessagesUpdated: (List<ChatMessage>) -> Unit): ListenerRegistration {
        return db.collection("chatRooms").document(chatRoomId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Repository", "Listen failed.", error)
                    onMessagesUpdated(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    onMessagesUpdated(messages)
                }
            }
    }

    /**
     * 새로운 멤버를 채팅방에 초대
     */
    suspend fun inviteMember(chatRoomId: String, inviterUserId: String, invitedUserEmail: String): AuthResult {
        return suspendCancellableCoroutine { continuation ->
            chatRoomManager.inviteMembers(
                chatRoomId = chatRoomId,
                inviterUserId = inviterUserId,
                invitedUserId = listOf(invitedUserEmail) // 이메일은 리스트 형태로 전달
            ) { success, error ->
                if (continuation.isActive) {
                    if (success) {
                        continuation.resume(AuthResult.Success)
                    } else {
                        continuation.resume(AuthResult.Failure(error ?: "초대 실패"))
                    }
                }
            }
        }
    }

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
                .addOnFailureListener {
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
                questionFrequency = "",
                questionTime = "",
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