package com.example.day_together.data.repository

import android.net.Uri
import android.util.Log
import com.example.day_together.AuthManager
import com.example.day_together.ui.message.ChatMessage
import com.example.day_together.data.model.User
import com.example.day_together.ui.gallery.MonthlyComment
import com.example.day_together.ui.gallery.PhotoItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine

import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlin.coroutines.resume

// Flow 관련 import
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.delay

/**
 * 앱의 모든 데이터 소스(Firebase Auth, Firestore, Storage)와의 통신을 담당하는 싱글톤 객체
 * 모든 비동기 작업은 코루틴(suspend 함수)을 통해 처리
 */
object AppRepository {

    // AuthManager에 인증 관련 로직을 위임
    private val authManager = AuthManager

    /**
     * 이메일과 비밀번호로 로그인 시도
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @return AuthResult 로그인 성공 또는 실패 결과
     */
    suspend fun login(email: String, password: String): AuthResult {
        return authManager.loginUser(email, password)
    }

    /**
     * Google ID 토큰을 서버로 전송하여 로그인 시도
     * @param idToken Google 로그인 시 발급받은 ID 토큰
     * @return AuthResult 로그인 성공 또는 실패 결과
     */
    suspend fun loginWithGoogleViaServer(idToken: String): AuthResult {
        return authManager.loginWithGoogleViaServer(idToken)
    }

    /**
     * Naver Access Token을 서버로 전송하여 로그인 시도
     * @param accessToken Naver 로그인 시 발급받은 Access Token
     * @return AuthResult 로그인 성공 또는 실패 결과
     */
    suspend fun loginWithNaver(accessToken: String): AuthResult {
        return authManager.loginWithNaver(accessToken)
    }

    /**
     * 신규 사용자를 등록
     * @param name 사용자 이름
     * @param email 사용자 이메일
     * @param password 사용자 비밀번호
     * @param position 사용자의 가족 내 위치 (예: "아빠", "엄마")
     * @param birthDate 사용자 생년월일
     * @return AuthResult 회원가입 성공 또는 실패 결과
     */
    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        position: String,
        birthDate: String,
    ): AuthResult {
        return authManager.registerUser(name, email, password, position, birthDate)
    }

    /**
     * 현재 사용자를 로그아웃
     */
    fun logout() {
        authManager.logoutUser()
    }

    /**
     * 현재 로그인된 사용자의 UID를 기반으로 Firestore에서 User 객체를 가져옴
     * @return 로그인된 사용자의 User 객체, 로그인되지 않았거나 실패 시 null
     */
    suspend fun getCurrentUser(): User? {
        val uid = authManager.getCurrentUserId()
        // 콜백 기반의 Firebase SDK를 코루틴으로 감싸기 위해 suspendCancellableCoroutine 사용
        return suspendCancellableCoroutine { continuation ->
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        // 코루틴이 아직 활성 상태일 때만 결과를 처리
                        if (continuation.isActive) {
                            val user = document.toObject(User::class.java)
                            continuation.resume(user) // 코루틴 재개 (성공)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null) // 코루틴 재개 (실패)
                    }
            } else {
                if (continuation.isActive) continuation.resume(null) // UID가 없으면 null 반환
            }
        }
    }

    /**
     * 현재 사용자가 속한 채팅방의 ID를 가져옴
     * @return 채팅방 ID (String), 실패 또는 채팅방이 없는 경우 null
     */
    suspend fun getMyChatRoomId(): String? = suspendCancellableCoroutine { cont ->
        val uid = authManager.getCurrentUserId() ?: return@suspendCancellableCoroutine cont.resume(null)
        FirebaseFirestore.getInstance().collection("chatRooms")
            .whereArrayContains("members", uid) // 'members' 배열에 내 UID가 포함된 문서를 찾음
            .get()
            .addOnSuccessListener { docs ->
                val chatRoomId = docs.firstOrNull()?.id // 첫 번째 매칭되는 채팅방의 ID
                cont.resume(chatRoomId)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Firestore의 'users' 컬렉션에 사용자 정보를 업데이트(덮어쓰기)
     * @param updatedUser 업데이트할 User 객체 (uid 필드가 반드시 포함되어야 함)
     */
    suspend fun updateUser(updatedUser: User) {
        val uid = updatedUser.uid
        if (uid.isBlank()) {
            Log.e("AppRepository", "updateUser: UID가 비어있습니다.")
            return
        }
        try {
            // .await()를 사용하여 Firestore 작업이 완료될 때까지 코루틴을 일시 중단
            FirebaseFirestore.getInstance().collection("users").document(uid).set(updatedUser).await()
            Log.d("AppRepository", "사용자 정보 업데이트 성공: $uid")
        } catch (e: Exception) {
            Log.e("AppRepository", "사용자 정보 업데이트 실패", e)
        }
    }

    /**
     * 현재 사용자의 비밀번호를 변경합니다.
     * @param oldPassword 기존 비밀번호
     * @param newPassword 새 비밀번호
     * @return AuthResult 변경 성공 또는 실패 결과
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult {
        return authManager.changePassword(oldPassword, newPassword)
    }

    /**
     * 비밀번호 재설정 이메일 발송
     * @param email 비밀번호를 재설정할 계정의 이메일
     * @return AuthResult 이메일 발송 성공 또는 실패 결과
     */
    suspend fun resetPassword(email: String): AuthResult {
        return authManager.resetPassword(email)
    }

    /**
     * 'daily_questions' 컬렉션에서 오늘 날짜에 해당하는 질문을 가져옴
     * @return 오늘의 질문 문자열, 없거나 실패 시 null
     */
    suspend fun getTodaysQuestion(): String? {
        val uid = authManager.getCurrentUserId()
        if (uid == null) {
            Log.w("AppRepository", "로그인한 사용자가 없어 오늘의 질문을 가져올 수 없습니다.")
            return null
        }

        // KST (Asia/Seoul) 기준 오늘 날짜
        val today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString()

        return try {
            val documents = FirebaseFirestore.getInstance().collection("daily_questions")
                .whereEqualTo("uid", uid) // 해당 사용자에게 할당된
                .whereEqualTo("target_date", today) // 오늘 날짜의
                .limit(1) // 질문 하나
                .get()
                .await() // 작업 대기

            if (!documents.isEmpty) {
                documents.documents[0].getString("question")
            } else {
                null // 오늘 질문이 없음
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "오늘의 질문 로드 실패", e)
            null
        }
    }

    /**
     * 특정 채팅방에 속한 모든 가족 구성원(멤버)의 User 정보를 가져옴
     * @param chatRoomId 정보를 가져올 채팅방 ID
     * @return List<User> 가족 구성원 목록, 실패 시 빈 리스트
     */
    suspend fun getFamilyMembers(chatRoomId: String): List<User> {
        if (chatRoomId.isBlank()) {
            Log.w("AppRepository", "getFamilyMembers 호출 시 chatRoomId가 비어있습니다.")
            return emptyList()
        }

        return try {
            // 1. 채팅방 문서를 가져와서 멤버 ID 목록을 확보
            val chatRoomDoc = FirebaseFirestore.getInstance().collection("chatRooms").document(chatRoomId).get().await()
            val memberIds = (chatRoomDoc.get("members") as? List<*>)?.filterIsInstance<String>()

            if (memberIds.isNullOrEmpty()) {
                Log.w("AppRepository", "채팅방($chatRoomId)에 멤버가 없습니다.")
                return emptyList()
            }

            // 2. 멤버 ID 목록을 사용하여 'users' 컬렉션에서 'whereIn' 쿼리로 모든 사용자 정보를 한 번에 가져옴
            val userDocs = FirebaseFirestore.getInstance().collection("users")
                .whereIn(FieldPath.documentId(), memberIds) // FieldPath.documentId()는 문서 ID를 의미
                .get()
                .await()

            // 3. DocumentSnapshot을 User 객체로 변환
            userDocs.mapNotNull { it.toObject(User::class.java) }

        } catch (e: Exception) {
            Log.e("AppRepository", "가족 구성원 정보 로드 실패", e)
            emptyList()
        }
    }

    /**
     * 특정 채팅방의 메시지 중 이미지 URL이 있는 메시지만 필터링하여 PhotoItem 리스트로 반환
     * @param chatRoomId 이미지를 가져올 채팅방 ID
     * @return List<PhotoItem> 사진 아이템 목록, 실패 시 빈 리스트
     */
    suspend fun getImages(chatRoomId: String): List<PhotoItem> {
        return suspendCancellableCoroutine { continuation ->
            FirebaseFirestore.getInstance().collection("chatRooms")
                .document(chatRoomId)
                .collection("messages") // 'messages' 하위 컬렉션
                .whereNotEqualTo("imageUrl", "") // imageUrl 필드가 비어있지 않은 문서
                .get()
                .addOnSuccessListener { snapshot ->
                    val photoItems = snapshot.documents.mapNotNull { doc ->
                        val msg = doc.toObject(ChatMessage::class.java)
                        // 이미지 URL이 유효하고 타임스탬프가 있는 경우에만 PhotoItem으로 변환
                        if (msg?.imageUrl.isNullOrBlank() || msg?.timestamp == null) return@mapNotNull null

                        PhotoItem(
                            id = doc.id,
                            imageUrl = msg.imageUrl,
                            date = msg.timestamp.toInstant()
                                .atZone(ZoneId.of("Asia/Seoul")) // KST로 변환
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

    /**
     * 특정 채팅방의 해당 월(YearMonth)의 코멘트 목록을 가져옴
     * @param chatRoomId 채팅방 ID
     * @param yearMonth 조회할 연월 (예: 2025-11)
     * @return List<MonthlyComment> 월간 코멘트 목록
     */
    suspend fun getMonthlyComments(chatRoomId: String, yearMonth: YearMonth): List<MonthlyComment> {
        return try {
            // 컬렉션 경로 예: "2025-11"
            val collectionName = "${yearMonth.year}-${yearMonth.monthValue}"
            val snapshot = FirebaseFirestore.getInstance()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("monthly_comments") // 월간 코멘트 상위 컬렉션
                .document(collectionName)       // "2025-11" 문서
                .collection("comments")         // 그 하위의 "comments" 컬렉션
                .orderBy("timestamp", Query.Direction.DESCENDING) // 최신순 정렬
                .get()
                .await()

            snapshot.toObjects(MonthlyComment::class.java)
        } catch (e: Exception) {
            Log.e("AppRepository", "getMonthlyComments 실패", e)
            emptyList()
        }
    }

    /**
     * 특정 채팅방의 해당 월에 코멘트 추가
     * @param chatRoomId 채팅방 ID
     * @param yearMonth 추가할 연월
     * @param comment 추가할 MonthlyComment 객체
     */
    suspend fun addMonthlyComment(chatRoomId: String, yearMonth: YearMonth, comment: MonthlyComment) {
        try {
            val collectionName = "${yearMonth.year}-${yearMonth.monthValue}"
            FirebaseFirestore.getInstance()
                .collection("chatRooms")
                .document(chatRoomId)
                .collection("monthly_comments")
                .document(collectionName)
                .collection("comments")
                .add(comment) // 코멘트 문서 추가
                .await()
        } catch (e: Exception) {
            Log.e("AppRepository", "addMonthlyComment 실패", e)
        }
    }

    /**
     * 특정 사용자가 속한 채팅방 ID를 찾음
     * 'invitations' 컬렉션에서 수락(accepted)된 초대를 먼저 확인하고,
     * 없으면 'chatRooms' 컬렉션에서 'members' 배열에 포함된 경우를 찾음
     * @param userId 채팅방을 찾을 사용자의 ID
     * @return 채팅방 ID (String), 실패 시 null
     */
    suspend fun findUserChatRoomId(userId: String): String? {
        if (userId.isBlank()) {
            Log.e("AppRepository", "findUserChatRoomId 호출 시 userId가 비어있습니다.")
            return null
        }

        return try {
            // 1. 'users'/{userId}/'invitations' 컬렉션에서 'accepted' 상태인 초대 확인
            val invitationsSnap = FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .collection("invitations")
                .whereEqualTo("status", "accepted")
                .limit(1)
                .get()
                .await()

            val acceptedChatRoomId = invitationsSnap.documents.firstOrNull()
                ?.getString("chatRoomId")

            if (!acceptedChatRoomId.isNullOrBlank()) {
                return acceptedChatRoomId // 수락한 초대의 채팅방 ID 반환
            }

            // 2. 수락한 초대가 없으면, 'chatRooms' 컬렉션에서 'members' 필드로 검색 (Fallback)
            val chatRooms = FirebaseFirestore.getInstance().collection("chatRooms")
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .await()
            chatRooms.documents.firstOrNull()?.id // 찾은 채팅방 ID 반환
        } catch (e: Exception) {
            Log.e("AppRepository", "findUserChatRoomId 실행 중 오류 발생", e)
            null
        }
    }

    /**
     * 채팅방 ID로 채팅방 이름을 가져옴
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 이름 (String), 실패 시 null
     */
    suspend fun getChatRoomName(chatRoomId: String): String? {
        return try {
            val document = FirebaseFirestore.getInstance().collection("chatRooms")
                .document(chatRoomId)
                .get()
                .await()
            document.getString("name") // 'name' 필드 값 반환
        } catch (e: Exception) {
            Log.e("AppRepository", "getChatRoomName 실패", e)
            null
        }
    }

    /**
     * 채팅방 이름을 업데이트
     * @param chatRoomId 채팅방 ID
     * @param newName 새 채팅방 이름
     */
    suspend fun updateChatRoomName(chatRoomId: String, newName: String) {
        // 'chatRoomName' 필드를 'newName'으로 업데이트
        FirebaseFirestore.getInstance().collection("chatRooms").document(chatRoomId)
            .update("chatRoomName", newName)
            .await()
    }

    /**
     * 새로운 채팅방 생성
     * @param inviterUserId 채팅방을 생성한 사용자(초대자)의 ID
     * @return 생성된 채팅방 ID (String), 실패 시 null
     */
    suspend fun createNewChatRoom(inviterUserId: String): String? {
        return try {
            val newChatRoomRef = FirebaseFirestore.getInstance().collection("chatRooms").document() // 새 문서 참조 생성
            val chatRoomId = newChatRoomRef.id
            val data = hashMapOf(
                "members" to listOf(inviterUserId), // 생성자를 첫 멤버로 추가
                "createdAt" to Date()
            )
            newChatRoomRef.set(data).await() // 데이터 설정
            chatRoomId // 생성된 ID 반환
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 특정 채팅방의 메시지를 실시간으로 수신 대기합니다.
     * @param chatRoomId 메시지를 수신할 채팅방 ID
     * @param onMessagesUpdated 메시지 목록이 업데이트될 때마다 호출되는 콜백
     * @return ListenerRegistration 리스너를 제거할 때 사용할 수 있는 객체
     */
    fun listenForMessages(chatRoomId: String, onMessagesUpdated: (List<ChatMessage>) -> Unit): ListenerRegistration {
        return FirebaseFirestore.getInstance().collection("chatRooms").document(chatRoomId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // 시간 오름차순 (오래된 메시지부터)
            .addSnapshotListener { snapshot, error -> // 실시간 변경 감지
                if (error != null) {
                    Log.w("Repository", "Listen failed.", error)
                    onMessagesUpdated(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    onMessagesUpdated(messages) // 콜백으로 메시지 목록 전달
                }
            }
    }

    /**
     * 다른 사용자를 채팅방에 초대합니다.
     * @param inviterUserId 초대하는 사용자 ID
     * @param invitedUserEmail 초대받는 사용자의 이메일
     * @return AuthResult 초대 성공 또는 실패 결과
     */
    suspend fun createInvitation(inviterUserId: String, invitedUserEmail: String): AuthResult {
        return try {
            // 1. 이메일로 초대받는 사용자의 UID 조회
            val invitedSnapshot = FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("email", invitedUserEmail)
                .limit(1)
                .get()
                .await()

            val invitedUserId = invitedSnapshot.documents.firstOrNull()?.id
                ?: return AuthResult.Failure("해당 이메일의 사용자를 찾을 수 없습니다.")

            // 2. 초대하는 사람과 초대받는 사람이 이미 속한 채팅방이 있는지 확인
            val existingChatRoom = FirebaseFirestore.getInstance().collection("chatRooms")
                .whereArrayContains("members", inviterUserId)
                .get()
                .await()
                .documents
                .firstOrNull { doc ->
                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()
                    members?.contains(invitedUserId) ?: false
                }

            // 3. 기존 채팅방이 없으면 새로 생성
            val chatRoomId = existingChatRoom?.id ?: createNewChatRoom(inviterUserId)

            val invitationId = UUID.randomUUID().toString()
            val invitationData = hashMapOf(
                "inviterId" to inviterUserId,
                "chatRoomId" to chatRoomId,
                "status" to "pending", // 초기 상태 'pending'
                "createdAt" to Date()
            )

            // 4. 초대받는 사용자의 'invitations' 하위 컬렉션에 초대 문서 생성
            FirebaseFirestore.getInstance().collection("users").document(invitedUserId)
                .collection("invitations")
                .document(invitationId)
                .set(invitationData)
                .await()

            AuthResult.Success()
        } catch (e: Exception) {
            Log.e("AppRepository", "inviteMember 실패", e)
            AuthResult.Failure("초대 전송 중 오류가 발생했습니다.")
        }
    }

    /**
     * 받은 초대 수락
     * @param invitationId 수락할 초대의 ID
     * @return AuthResult 수락 성공 또는 실패 결과 (성공 시 채팅방 ID 포함 가능)
     */
    suspend fun acceptInvitation(invitationId: String): AuthResult {
        val inviteeId = authManager.getCurrentUserId() ?: return AuthResult.Failure("로그인이 필요합니다.") // 초대 수락자 (현재 사용자)

        return try {
            // 1. 내 'invitations' 컬렉션에서 해당 초대장 참조
            val invitationRef = FirebaseFirestore.getInstance().collection("users")
                .document(inviteeId)
                .collection("invitations")
                .document(invitationId)

            val invitationSnap = invitationRef.get().await()

            if (!invitationSnap.exists()) {
                AuthResult.Failure("초대 정보를 찾을 수 없습니다.")
            } else {
                val inviterId = invitationSnap.getString("inviterId") // 초대자 ID
                if (inviterId == null) {
                    AuthResult.Failure("초대자 정보가 없습니다.")
                } else {
                    var chatRoomId = invitationSnap.getString("chatRoomId")

                    // 2. (방어 코드) 혹시 채팅방 ID가 없다면 새로 생성
                    if (chatRoomId.isNullOrBlank()) {
                        chatRoomId = createNewChatRoom(inviterId)
                    }

                    if (chatRoomId == null) {
                        AuthResult.Failure("채팅방 생성에 실패했습니다.")
                    } else {
                        // 3. 채팅방 'members' 배열에 초대자와 초대 수락자를 모두 추가 (arrayUnion은 중복 방지)
                        FirebaseFirestore.getInstance().collection("chatRooms").document(chatRoomId)
                            .update("members", FieldValue.arrayUnion(inviterId, inviteeId))
                            .await()

                        // 4. 초대장 상태를 'accepted'로 변경
                        invitationRef.update(
                            mapOf(
                                "status" to "accepted",
                                "acceptedAt" to Date(),
                                "chatRoomId" to chatRoomId
                            )
                        ).await()

                        // 5. 각 사용자의 User 문서에도 편의를 위해 채팅방 ID 업데이트
                        FirebaseFirestore.getInstance().collection("users").document(inviterId)
                            .update("invitedChatRoomId", chatRoomId)
                            .await()

                        FirebaseFirestore.getInstance().collection("users").document(inviteeId)
                            .update("invitedChatRoomId", chatRoomId)
                            .await()

                        AuthResult.Success(chatRoomId) // 성공 (채팅방 ID 반환)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "acceptInvitation 실패", e)
            AuthResult.Failure("초대 수락 중 오류가 발생했습니다.")
        }
    }

    /**
     * 채팅방에 메시지(텍스트 또는 이미지) 전송
     * @param chatRoomId 메시지를 보낼 채팅방 ID
     * @param text 텍스트 내용 (이미지일 경우 빈 문자열 가능)
     * @param sender 메시지 발신자 (UID)
     * @param imageUrl 이미지 URL (텍스트 메시지일 경우 null)
     */
    fun sendMessage(
        chatRoomId: String,
        text: String,
        sender: String,
        imageUrl: String? = null
    ) {
        // 발신자가 없거나, 텍스트와 이미지가 모두 없는 경우는 무시
        if (sender.isBlank() || (text.isBlank() && imageUrl.isNullOrBlank())) return

        val type = if (!imageUrl.isNullOrBlank()) "image" else "text" // 메시지 타입 구분

        val message = hashMapOf(
            "sender" to sender,
            "content" to text,
            "timestamp" to Date(), // 서버 시간 기준 타임스탬프
            "imageUrl" to imageUrl,
            "type" to type
        )

        // 'messages' 하위 컬렉션에 새 메시지 문서 추가
        FirebaseFirestore.getInstance().collection("chatRooms")
            .document(chatRoomId)
            .collection("messages")
            .add(message)
    }

    /**
     * 이미지를 Firebase Storage에 업로드
     * @param uri 업로드할 이미지의 Uri
     * @param onComplete 업로드 완료 시 호출되는 콜백 (결과로 다운로드 URL 전달, 실패 시 null)
     */
    fun uploadImageToStorage(uri: Uri, onComplete: (String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        // 경로: "chat_images/{UUID}.jpg"
        val imageRef = storageRef.child("chat_images/${UUID.randomUUID()}.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                // 업로드 성공 시 다운로드 URL 가져오기
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                onComplete(null) // 실패 시 null 전달
            }
    }

    // 설정 (Settings) 관련
    // (현재 Mock 데이터(임시 데이터)를 반환)

    /**
     * 사용자 설정 정보를 Flow로 반환 (현재 하드코딩된 값)
     * @return UserSettings를 방출(emit)하는 Flow
     */
    fun getSettingsFlow(): Flow<UserSettings> {
        // 실제로는 DataStore나 SharedPreferences에서 값을 읽어와야 함
        return flowOf(
            UserSettings(
                questionFrequency = "매일",
                questionTime = "19:00",
                notificationEnabled = true,
                vibrationEnabled = false
            )
        )
    }

    /**
     * 사용자 설정을 저장(현재는 딜레이와 로그만 있음)
     * @param settings 저장할 UserSettings 객체
     */
    suspend fun saveSettings(settings: UserSettings) {
        // 실제로는 DataStore나 SharedPreferences에 값을 저장해야 함
        delay(200) // 가상 저장 시간
        Log.d("AppRepository", "설정 저장됨: $settings")
    }
}

/**
 * 인증(로그인, 회원가입 등) 작업의 결과를 나타내는 sealed class
 */
sealed class AuthResult {
    /**
     * 작업 성공
     * @param chatRoomId (선택적) 작업 완료 후 이동할 채팅방 ID
     */
    data class Success(val chatRoomId: String? = null) : AuthResult()

    /**
     * 작업 실패
     * @param message 실패 사유 (사용자에게 표시될 수 있음)
     */
    data class Failure(val message: String) : AuthResult()
}

/**
 * 앱 설정 정보를 담는 데이터 클래스
 */
data class UserSettings(
    val questionFrequency: String, // 질문 빈도
    val questionTime: String,      // 질문 시간
    val notificationEnabled: Boolean, // 알림 활성화 여부
    val vibrationEnabled: Boolean   // 진동 활성화 여부
)