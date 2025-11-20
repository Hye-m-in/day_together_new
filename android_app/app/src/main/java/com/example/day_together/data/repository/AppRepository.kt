package com.example.day_together.data.repository

import android.net.Uri
import android.util.Log
import com.example.day_together.AuthManager
import com.example.day_together.ui.message.ChatMessage
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

import java.time.YearMonth

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

    // ChatRoomManager.db는 private이므로 접근 불가. Repository에서 직접 Firestore 인스턴스를 생성
    private val db = FirebaseFirestore.getInstance()

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
     * 구글 ID 토큰으로 서버를 경유하여 로그인 요청
     * 클라이언트 → 서버: GoogleTokenRequest(id_token)
     * 서버: Google ID 토큰 검증 후 Firebase Custom Token 발급
     * 클라이언트: FirebaseAuth.signInWithCustomToken(customToken) 호출
     */
    suspend fun loginWithGoogleViaServer(idToken: String): AuthResult {
        return try {
            val request = GoogleTokenRequest(idToken = idToken)

            // ApiClient.service -> ApiClient.authService 로 변경
            val response = ApiClient.authService.googleLogin(request)
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

            // ApiClient.service -> ApiClient.authService 로 변경
            val response = ApiClient.authService.naverLogin(request)
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

    // 사용자 정보로 회원가입 요청
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        birthDate: String,
        position: String,
        profileImage: String
    ): AuthResult {

        return suspendCancellableCoroutine { continuation ->
            authManager.registerUser(
                name,
                email,
                password,
                birthDate,
                position,
                profileImage
            ) { success, errorMessage ->
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
                            // User 객체로 바로 변환
                            val user = document.toObject(User::class.java)
                            if (user != null) {
                                continuation.resume(user)
                            } else {
                                // 변환 실패 시 수동 매핑
                                val name = document.getString("name") ?: "Unknown"
                                val email = document.getString("email") ?: "Unknown"
                                val position = document.getString("position") ?: "가족" //todo
                                continuation.resume(User(uid = uid, name = name, email = email, position = position))
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } else {
                if (continuation.isActive) continuation.resume(null)
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



    // HomeViewModel 관련 함수
    suspend fun getTodaysQuestion(): Question {
        delay(300)
        return Question(id = "q1", text = "우리 가족만의 특별한 루틴이 있나요?")
    }

    suspend fun getFamilyQuote(): String {
        delay(200)
        return "\"가족 사랑은 평화의 시작이다.\""
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


    suspend fun findUserChatRoomId(userId: String): String? {
        if (userId.isBlank()) {
            Log.e("AppRepository", "findUserChatRoomId 호출 시 userId가 비어있습니다.")
            return null
        }

        return try {
            // 사용자가 멤버로 포함된 채팅방 한 개만 찾기
            val chatRooms = db.collection("chatRooms")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .await()

            chatRooms.documents.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("AppRepository", "findUserChatRoomId 실행 중 오류 발생", e)
            null
        }
    }

    // 채팅방에 속한 가족 구성원 목록 가져오기
    suspend fun getFamilyMembers(chatRoomId: String): List<User> {
        return try {
            // 채팅방 문서에서 members 배열(UID 리스트) 가져오기
            val chatRoomSnap = db.collection("chatRooms")
                .document(chatRoomId)
                .get()
                .await()

            val memberIds = chatRoomSnap.get("members") as? List<*>
            val uidList = memberIds
                ?.filterIsInstance<String>()
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            if (uidList.isEmpty()) {
                Log.w("AppRepository", "getFamilyMembers: members가 비어 있음 (chatRoomId=$chatRoomId)")
                return emptyList()
            }

            // 각 UID에 대해 users/{uid} 문서를 개별 조회
            val members = mutableListOf<User>()
            for (uid in uidList) {
                val userDoc = db.collection("users")
                    .document(uid)
                    .get()
                    .await()

                val user = userDoc.toObject(User::class.java)
                if (user != null) {
                    // member_id(=uid 필드)가 비어 있으면 문서 ID로 보정
                    if (user.uid.isBlank()) {
                        user.uid = uid
                    }
                    members.add(user)
                }
            }

            members
        } catch (e: Exception) {
            Log.e("AppRepository", "getFamilyMembers 실패 (chatRoomId=$chatRoomId)", e)
            emptyList()
        }
    }

    // 채팅방 생성일 가져오기
    suspend fun getChatRoomCreationDate(chatRoomId: String): Date? {
        return try {
            val doc = db.collection("chatRooms")
                .document(chatRoomId)
                .get()
                .await()

            doc.getDate("createdAt")
        } catch (e: Exception) {
            Log.e("AppRepository", "getChatRoomCreationDate 실패 (chatRoomId=$chatRoomId)", e)
            null
        }
    }


    /**
     * 새 채팅방 생성 시 기존 데이터 구조와 동일하게 필드 저장
     * - chatRoomName: 기본 이름
     * - members: 방 멤버
     * - invitedUsers: 아직 합류 전인 초대 대상자 리스트 (초기에는 빈 리스트)
     * - createdAt: 생성 시간
     */
    suspend fun createNewChatRoom(inviterUserId: String): String? {
        return try {
            val newChatRoomRef = db.collection("chatRooms").document()
            val chatRoomId = newChatRoomRef.id

            val data = hashMapOf(
                "chatRoomId" to chatRoomId,
                "chatRoomName" to "우리 가족 채팅방",      // 기본 방 이름
                "members" to listOf(inviterUserId),        // 방 만든 사람만 먼저 멤버로
                "invitedUsers" to listOf<String>(),        // 초대된 사람들 uid 리스트 (초기엔 비어있음)
                "createdAt" to Date()
            )

            newChatRoomRef.set(data).await()
            chatRoomId
        } catch (e: Exception) {
            Log.e("AppRepository", "createNewChatRoom 실패", e)
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
                    try {
                        // [안전장치] 데이터 형식 불일치 시 크래시 방지
                        val messages = snapshot.toObjects(ChatMessage::class.java)
                        onMessagesUpdated(messages)
                    } catch (e: Exception) {
                        Log.e("Repository", "메시지 데이터 파싱 오류", e)
                        onMessagesUpdated(emptyList())
                    }
                }
            }
    }

    // 채팅방 이름 변경
    suspend fun updateChatRoomName(chatRoomId: String, newName: String) {
        try {
            db.collection("chatRooms")
                .document(chatRoomId)
                .update("chatRoomName", newName)
                .await()
        } catch (e: Exception) {
            Log.e("AppRepository", "updateChatRoomName 실패", e)
            throw e
        }
    }

    // 새로운 멤버를 채팅방에 초대 -> 초대자가 이미 채팅방에 있다면, 새 방을 만들지 않고 기존 방에 초대
     suspend fun createInvitation(inviterUserId: String, invitedUserEmail: String): AuthResult {
        return try {
            val invitedSnapshot = db.collection("users")
                .whereEqualTo("email", invitedUserEmail)
                .limit(1)
                .get()
                .await()

            val invitedUserId = invitedSnapshot.documents.firstOrNull()?.id
                ?: return AuthResult.Failure("해당 이메일의 사용자를 찾을 수 없습니다.")

            // 1. 초대자가 이미 속해있는 가족 채팅방이 있는지 확인
            val inviterDoc = db.collection("users").document(inviterUserId).get().await()
            var currentChatRoomId = inviterDoc.getString("invitedChatRoomId")

            if (currentChatRoomId.isNullOrBlank()) {
                val existingRooms = db.collection("chatRooms")
                    .whereArrayContains("members", inviterUserId)
                    .limit(1)
                    .get()
                    .await()
                currentChatRoomId = existingRooms.documents.firstOrNull()?.id
            }

            // 2. 기존 방이 있으면 그 ID 사용, 없으면 새로 생성
            val chatRoomId = currentChatRoomId ?: createNewChatRoom(inviterUserId)

            if (chatRoomId == null) {
                return AuthResult.Failure("채팅방 연결에 실패했습니다.")
            }

            // 초대받은 유저를 invitedUsers 배열에 기록
            db.collection("chatRooms").document(chatRoomId)
                .update("invitedUsers", FieldValue.arrayUnion(invitedUserId))
                .await()

            // 3. 초대장 발송 (users/{invitedUserId}/invitations)
            val invitationId = UUID.randomUUID().toString()
            val invitationData = hashMapOf(
                "inviterId" to inviterUserId,
                "chatRoomId" to chatRoomId,
                "status" to "pending",
                "createdAt" to Date()
            )

            db.collection("users").document(invitedUserId)
                .collection("invitations")
                .document(invitationId)
                .set(invitationData)
                .await()

            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AppRepository", "inviteMember 실패", e)
            AuthResult.Failure("초대 전송 중 오류가 발생했습니다.")
        }
    }


    // AppRepository.kt

    // 반환 타입을 AuthResult -> String? 으로 변경 (성공 시 chatRoomId, 실패 시 null)
    suspend fun acceptInvitation(invitationId: String): String? {
        val inviteeId = authManager.getCurrentUserId()
        if (inviteeId == null) return null // 실패 시 null 반환

        return try {
            val invitationRef = db.collection("users")
                .document(inviteeId)
                .collection("invitations")
                .document(invitationId)

            val invitationSnap = invitationRef.get().await()
            if (!invitationSnap.exists()) return null

            val inviterId = invitationSnap.getString("inviterId") ?: return null
            var chatRoomId = invitationSnap.getString("chatRoomId")

            // 방이 없으면 생성
            if (chatRoomId.isNullOrBlank()) {
                chatRoomId = createNewChatRoom(inviterId)
                if (chatRoomId == null) return null
            }

            // [1] 채팅방 멤버 추가
            db.collection("chatRooms").document(chatRoomId)
                .update("members", FieldValue.arrayUnion(inviterId, inviteeId))
                .await()

            // [2] 초대장 상태 업데이트
            invitationRef.update(
                mapOf(
                    "status" to "accepted",
                    "acceptedAt" to Date(),
                    "chatRoomId" to chatRoomId
                )
            ).await()

            // [3] 유저 정보 갱신
            db.collection("users").document(inviterId).update("invitedChatRoomId", chatRoomId).await()
            db.collection("users").document(inviteeId).update("invitedChatRoomId", chatRoomId).await()

            // 성공 시 채팅방 ID 반환
            chatRoomId
        } catch (e: Exception) {
            Log.e("AppRepository", "acceptInvitation 실패", e)
            null // 실패 시 null 반환
        }
    }


    // 초대 거절
    suspend fun rejectInvitation(invitationId: String): AuthResult {
        val inviteeId = authManager.getCurrentUserId()
            ?: return AuthResult.Failure("로그인이 필요합니다.")

        return try {
            val invitationRef = db.collection("users")
                .document(inviteeId)
                .collection("invitations")
                .document(invitationId)

            val invitationSnap = invitationRef.get().await()
            if (!invitationSnap.exists()) {
                return AuthResult.Failure("초대 정보를 찾을 수 없습니다.")
            }

            // status를 'rejected'로 변경
            invitationRef.update(
                mapOf(
                    "status" to "rejected",
                    "rejectedAt" to Date()
                )
            ).await()

            AuthResult.Success
        } catch (e: Exception) {
            Log.e("AppRepository", "rejectInvitation 실패", e)
            AuthResult.Failure("초대 거절 중 오류가 발생했습니다.")
        }
    }




    // 새로운 채팅 메시지 전송

    fun sendMessage(
        chatRoomId: String,
        text: String,
        sender: String,
        imageUrl: String? = null,
        type: String
    ) {

        // 시작 로그
        Log.d("AppRepository", "sendMessage 시작 - chatRoomId=$chatRoomId, sender=$sender, text='$text', imageUrl=$imageUrl")


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
