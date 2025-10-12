package com.example.day_together.data.repository

import android.net.Uri
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
import com.example.day_together.data.dto.GoogleTokenRequest
import com.example.day_together.data.remote.ApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import retrofit2.HttpException

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine

// Firebase Task를 Coroutine으로 사용하기 위한 import
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.coroutines.resume

/**
 * 앱의 모든 데이터 통신을 책임지는 통합 Repository 클래스
 * 각 ViewModel이 필요로 하는 모든 기능 제공 & 실제 백엔드 로직(AuthManager 등)과 ViewModel 사이의 중개인 역할
 */
object AppRepository {

    // 실제 로직 담당 매니저들 선언
    private val authManager = AuthManager
    private val chatRoomManager = ChatRoomManager
    private val calendarManager = CalendarManager
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
     * 기존 -> 구글 ID 토큰으로 Firebase에 직접 로그인 요청
     * - 서버를 거치지 않고 FirebaseAuth.signInWithCredential로 로그인
     * - 서버 세션/권한이 필요 없는 경우에만 사용
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
     * 구글 ID 토큰으로 서버를 경유하여 로그인 요청
     * 클라이언트 → 서버: GoogleTokenRequest(id_token)
     * 서버: Google ID 토큰 검증 후 Firebase Custom Token 발급
     * 클라이언트: FirebaseAuth.signInWithCustomToken(customToken) 호출
     */
    suspend fun loginWithGoogleViaServer(idToken: String): AuthResult {
        return try {
            val request = GoogleTokenRequest(idToken = idToken)
            val response = ApiClient.service.googleLogin(request)
            val customToken = response.customToken

            if (customToken.isBlank()) {
                return AuthResult.Failure("서버로부터 유효한 토큰을 받지 못했습니다.")
            }

            // 서버에서 받은 커스텀 토큰으로 Firebase 최종 로그인
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
            AuthResult.Success
        } catch (e: Exception) {
            // 서버가 400 응답을 내려줄 경우 에러 메시지 파싱
            if (e is HttpException && e.code() == 400) {
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    val detailMessage = errorResponse.detail ?: "알 수 없는 400 오류입니다."
                    return AuthResult.Failure(detailMessage)
                } catch (jsonE: Exception) {
                    return AuthResult.Failure("서버 응답을 해석할 수 없습니다.")
                }
            }
            // 네트워크/기타 오류 처리
            Log.e("AppRepository", "구글 로그인(서버) 실패", e)
            AuthResult.Failure("서버와 통신할 수 없습니다. 네트워크 상태를 확인해주세요.")
        }
    }

    /**
     * 네이버 액세스 토큰으로 우리 서버에 로그인 요청을 보내고,
     * 받은 커스텀 토큰으로 Firebase에 최종 로그인하는 함수
     */
    suspend fun loginWithNaver(accessToken: String): AuthResult {
        return try {
            val request = NaverTokenRequest(accessToken = accessToken)
            val response = ApiClient.service.naverLogin(request)
            val customToken = response.customToken
            if (customToken.isBlank()) {
                return AuthResult.Failure("서버로부터 유효한 토큰을 받지 못했습니다.")
            }
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
            AuthResult.Success
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 400) {
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    val detailMessage = errorResponse.detail ?: "알 수 없는 400 오류입니다."
                    return AuthResult.Failure(detailMessage)
                } catch (jsonE: Exception) {
                    return AuthResult.Failure("서버 응답을 해석할 수 없습니다.")
                }
            }
            Log.e("AppRepository", "네이버 로그인 실패", e)
            AuthResult.Failure("서버와 통신할 수 없습니다. 네트워크 상태를 확인해주세요.")
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

    //내 채팅방ID 찾기
    suspend fun getMyChatRoomId(): String? = suspendCancellableCoroutine { cont ->
        val uid = authManager.getCurrentUserId() ?: return@suspendCancellableCoroutine cont.resume(null)
        db.collection("chatRooms")
            .whereArrayContains("members", uid)
            .get()
            .addOnSuccessListener { docs ->
                val chatRoomId = docs.firstOrNull()?.id
                cont.resume(chatRoomId)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * 수정된 사용자 정보 DB에 업데이트
     */
    suspend fun updateUser(updatedUser: User) {
        println("TODO: DB에 사용자 정보 업데이트: $updatedUser")
        delay(500)
    }

    /**
     * 비밀번호 변경
     */
    suspend fun changePassword(email: String, newPassword: String) {
        println("TODO: DB에 비밀번호 변경 요청: $email")
        delay(500)
    }

    /**
     * 비밀번호 재설정 요청
     */
    suspend fun resetPassword(email: String): AuthResult {
        println("TODO: 비밀번호 재설정 이메일 전송 요청: $email")
        delay(1000)
        return AuthResult.Success
    }

    /**
     * 아이디(이메일) 찾기
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

    // --- HomeViewModel 관련 함수들 ---
    suspend fun getTodaysQuestion(): Question {
        delay(300)
        return Question(id = "q1", text = "우리 가족만의 특별한 루틴이 있나요?")
    }

    suspend fun getFamilyQuote(): String {
        delay(200)
        return "\"가족 사랑은 평화의 시작이다.\""
    }

    // --- 캘린더 관련 함수들 ---

    /**
     * CalendarManager의 실시간 리스너를 ViewModel과 연결하는 함수.
     * List<CalendarEvent>를 ViewModel이 사용하기 좋은 Map<LocalDate, List<CalendarEvent>> 형태로 가공.
     */
    fun listenForCalendarEvents(
        chatRoomId: String,
        onEventsUpdated: (Map<LocalDate, List<CalendarEvent>>) -> Unit
    ): ListenerRegistration {
        return calendarManager.listenForEvents(chatRoomId) { events ->
            val eventsByDate = events.groupBy {
                it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            }
            onEventsUpdated(eventsByDate)
        }
    }

    // ViewModel이 Repository를 통해 이벤트를 추가할 수 있도록 함수를 연결
    suspend fun addOrUpdateCalendarEvent(chatRoomId: String, event: CalendarEvent){
        calendarManager.addEvent(chatRoomId, event)
    }

    /**
     * 채팅의 모든 사진 목록을 가져옴
     */
    suspend fun getImages(chatRoomId: String): List<PhotoItem> {
        delay(500)
        return suspendCancellableCoroutine { continuation ->
            db.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .whereNotEqualTo("imageUrl", "")
                .get()
                .addOnSuccessListener { snapshot ->
                    val photoItems = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(ChatMessage::class.java)
                        if (msg?.imageUrl.isNullOrBlank() || msg?.timestamp == null) return@mapNotNull null

                        // ChatMessage → PhotoItem 변환
                        PhotoItem(
                            id = doc.id,
                            imageUrl = msg.imageUrl ?: "",
                            date = msg.timestamp.toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                                .toString()
                        )
                    }

                    if (continuation.isActive) continuation.resume(photoItems)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
        }
    }

    suspend fun getMonthlyComments(yearMonth: YearMonth): List<MonthlyComment> {
        delay(400)
        return listOf()
    }

    suspend fun addMonthlyComment(yearMonth: YearMonth, comment: MonthlyComment) {
        delay(500)
        println("TODO: ${yearMonth}에 댓글 추가 - ${comment.text}")
    }

    // 채팅 관련 함수(MessageViewModel)
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

    suspend fun findUserChatRoomId(userId: String): String? {
        // userId가 비어있으면 아무것도 하지 않고 null을 반환
        if (userId.isBlank()) {
            Log.e("AppRepository", "findUserChatRoomId 호출 시 userId가 비어있습니다.")
            return null
        }

        return try {
            // 1. 사용자의 invitations 컬렉션에서 accepted 상태인 문서 찾음
            val invitations = db.collection("users").document(userId).collection("invitations")
                .whereEqualTo("status", "accepted").limit(1).get().await()
            val acceptedRoomId = invitations.documents.firstOrNull()?.id

            // 찾았다면 해당 채팅방 ID를 즉시 반환
            if (acceptedRoomId != null) {
                return acceptedRoomId
            }

            // 2. 초대 수락 기록이 없다면, chatRooms 컬렉션에서 사용자가 members 배열에 포함된 경우를 찾음
            val chatRooms = db.collection("chatRooms").whereArrayContains("members", userId).limit(1).get().await()
            chatRooms.documents.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("AppRepository", "findUserChatRoomId 실행 중 오류 발생", e)
            null
        }
    }


    suspend fun updateChatRoomName(chatRoomId: String, newName: String) {
        db.collection("chatRooms").document(chatRoomId)
            .update("chatRoomName", newName)
            .await()
    }

    suspend fun createNewChatRoom(inviterUserId: String): String? {
        return try {
            val newChatRoomRef = db.collection("chatRooms").document()
            val chatRoomId = newChatRoomRef.id
            val data = hashMapOf(
                "members" to listOf(inviterUserId),
                "createdAt" to Date()
            )
            newChatRoomRef.set(data).await()
            chatRoomId
        } catch (e: Exception) {
            null
        }
    }

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
                invitedUserId = listOf(invitedUserEmail)
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

    suspend fun getChatMessages(chatRoomId: String): List<ChatMessage> {
        return try {
            val snapshot = db.collection("chatRooms")
                .document(chatRoomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }




    /**
     * 새로운 채팅 메시지 전송
     */
    fun sendMessage(
        chatRoomId: String,
        text: String,
        sender: String,
        imageUrl: String? = null,
        type: String
    ) {
        // 아무 내용도 없으면 리턴
        if (sender.isBlank() || (text.isBlank() && imageUrl.isNullOrBlank())) return

        // 메시지 타입 자동 판별
        val type = if (!imageUrl.isNullOrBlank()) "image" else "text"

        val message = hashMapOf(
            "sender" to sender,
            "content" to text,
            "timestamp" to Date(),
            "imageUrl" to imageUrl,
            "type" to type
        )

        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
    }


    fun uploadImageToStorage(uri: Uri, onComplete: (String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    onComplete(uri.toString())
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    // SettingsViewModel
    fun getSettingsFlow(): Flow<UserSettings> {
        return flowOf(
            UserSettings(
                questionFrequency = "",
                questionTime = "",
                notificationEnabled = true,
                vibrationEnabled = false
            )
        )
    }

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