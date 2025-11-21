package com.example.day_together.ui.message

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.day_together.AuthManager
import com.example.day_together.data.model.User
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.AuthResult
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

/**
 * 채팅 메시지를 위한 데이터 클래스
 * Firestore의 자동 변환을 위해 모든 속성에 기본값 할당
 */
data class ChatMessage(
    val content: String = "",
    val sender: String = "",
    val timestamp: Date = Date(),
    val imageUrl: String = "",
    val type: String = ""
)

/**
 * ChatInfoScreen 및 MessageScreen의 UI 상태 관리 위한 데이터 클래스
 * (참고: FamilyMember 클래스는 다른 파일에 정의되어 있다고 가정합니다)
 */
data class MessageUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val chatRoomId: String? = null,
    val chatRoomName: String? = "가족 채팅방",
    val currentUser: User? = null,
    val currentUserName: String = "사용자",

    // MessageScreen 상태
    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",

    // ChatInfoScreen 상태
    val creationDate: String = "",
    val familyMembers: List<FamilyMember> = emptyList(),
    val showInviteDialog: Boolean = false,
)

/**
 * UI에서 발생하는 모든 사용자 이벤트를 정의
 */
sealed interface MessageEvent {
    data class OnMessageTextChanged(val text: String) : MessageEvent
    data class SendMessage(val text: String) : MessageEvent
    data class SendImage(val imageUri: Uri) : MessageEvent
    data object ShowInviteDialog : MessageEvent
    data class EditChatRoomName(val newName: String) : MessageEvent
    data object DismissInviteDialog : MessageEvent
    data class InviteMember(val email: String) : MessageEvent
    data class AcceptInvitation(val invitationId: String) : MessageEvent
    data object CreateNewChatRoom : MessageEvent
}

/**
 * MessageScreen과 ChatInfoScreen의 상태 및 로직 담당 ViewModel
 */
open class MessageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    protected val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private var messagesListener: ListenerRegistration? = null

    init {
        fetchChatRoomInfo()
        fetchTodayQuestion()
    }

    override fun onCleared() {
        messagesListener?.remove()
        super.onCleared()
    }

    private suspend fun loadChatRoomName(chatRoomId: String) {
        try {
            // AppRepository에 getChatRoomName이 없으므로 우선 기본값 사용
            // 추후 Repository에 fun getChatRoomName(chatRoomId: String): String? 구현 필요
            // val name = repository.getChatRoomName(chatRoomId) ?: "가족 채팅방"
            val name = "가족 채팅방"
            _uiState.update { it.copy(chatRoomName = name) }
        } catch (e: Exception) {
            Log.e("MessageViewModel", "채팅방 이름 불러오기 실패", e)
        }
    }

    private fun listenForMessages(chatRoomId: String) {
        messagesListener?.remove()
        messagesListener = repository.listenForMessages(chatRoomId) { newMessages ->

            val updatedMessages = newMessages.map { msg ->
                ChatMessage(
                    content = msg.content,              // 혹시 몰라서 ?: "" 써도 됨
                    sender = msg.sender,
                    timestamp = msg.timestamp,
                    imageUrl = msg.imageUrl ?: "",     // null이면 빈 문자열로
                    type = msg.type
                )
            }

            _uiState.update {
                it.copy(messages = updatedMessages, isLoading = false)
            }
        }
    }


    // 텍스트 메세지 전송
    private fun sendMessage() {
        val currentState = _uiState.value
        val chatRoomId = currentState.chatRoomId ?: return
        val messageText = currentState.messageText.trim()

        // 디버그 로그 추가
        Log.d("MessageViewModel", "sendMessage() 호출 - chatRoomId=$chatRoomId, text='$messageText', sender=${currentState.currentUserName}")


        if (messageText.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(
                chatRoomId = chatRoomId,
                sender = currentState.currentUserName,
                text = messageText,
                imageUrl = null,
                type = "text" // type 파라미터 추가
            )
            _uiState.update { it.copy(messageText = "") }
        }
    }

    // 이미지 메세지 전송
    private fun sendImage(uri: Uri) {
        val currentState = _uiState.value
        val chatRoomId = currentState.chatRoomId ?: return

        Log.d("MessageViewModel", "sendImage 호출됨, URI = $uri")

        viewModelScope.launch {
            repository.uploadImageToStorage(uri) { imageUrl ->
                if (imageUrl != null) {
                    Log.d("MessageViewModel", "이미지 업로드 성공: $imageUrl")
                    repository.sendMessage(
                        chatRoomId = chatRoomId,
                        sender = currentState.currentUserName,
                        text = "",
                        imageUrl = imageUrl,
                        type = "image" // type 파라미터 추가
                    )
                } else {
                    Log.e("MessageViewModel", "이미지 업로드 실패")
                    _uiState.update { it.copy(errorMessage = "이미지 업로드에 실패했습니다.") }
                }
            }
        }
    }

    private fun fetchTodayQuestion() {
        viewModelScope.launch {
            val question = repository.getTodaysQuestion()
            question?.let { q ->
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(
                    // ChatMessage는 String을 받으므로 q.text 사용
                    ChatMessage(content = q.text, sender = "system")
                )
                _uiState.update { it.copy(messages = currentMessages) }
            }
        }
    }

    private fun fetchChatRoomInfo() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            val currentUserId = AuthManager.getCurrentUserId()

            if (currentUser == null || currentUserId.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "로그인이 필요합니다."
                    )
                }
                return@launch
            }

            // 이름은 프로필에서 가져오고
            _uiState.update { it.copy(currentUserName = currentUser.name) }

            // 채팅방 찾을 때는 항상 Firebase UID 사용
            val chatRoomId = repository.findUserChatRoomId(currentUserId)
            if (chatRoomId != null) {
                _uiState.update { it.copy(chatRoomId = chatRoomId) }

                loadChatRoomName(chatRoomId)
                listenForMessages(chatRoomId)
                loadChatRoomExtraInfo(chatRoomId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }

        }
    }


    private fun createNewChatRoom() {
        val currentUserId = AuthManager.getCurrentUserId() ?: return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val newRoomId = repository.createNewChatRoom(currentUserId)
            if (newRoomId != null) {
                _uiState.update { it.copy(chatRoomId = newRoomId) }
                listenForMessages(newRoomId)
                loadChatRoomExtraInfo(newRoomId)
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "채팅방 생성에 실패했습니다.") }
            }
        }
    }

    // 초대 성공 시 화면 즉시 전환 로직 추가
    private fun inviteMember(email: String) {
        val inviterId = AuthManager.getCurrentUserId()
        if (inviterId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "로그인이 필요합니다.") }
            return
        }
        _uiState.update { it.copy(showInviteDialog = false, isLoading = true) }

        viewModelScope.launch {
            val result = repository.createInvitation(inviterId, email)
            if (result is AuthResult.Success) {
                // AuthResult.Success에는 ID가 없으므로, DB에서 갱신된 정보를 다시 조회
                // 초대 생성 시 채팅방이 새로 만들어졌을 수 있으므로 확인 필요
                val newChatRoomId = repository.findUserChatRoomId(inviterId)

                if (newChatRoomId != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showInviteDialog = false,
                            chatRoomId = newChatRoomId
                        )
                    }
                    // 채팅방 이름 및 메시지 리스너 로드
                    loadChatRoomName(newChatRoomId)
                    listenForMessages(newChatRoomId)
                    loadChatRoomExtraInfo(newChatRoomId)
                } else {
                    // ID가 없을 경우(예외적 상황) 다시 로드 시도
                    _uiState.update { it.copy(showInviteDialog = false, isLoading = false) }
                    fetchChatRoomInfo()
                }
            } else if (result is AuthResult.Failure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.acceptInvitation(invitationId)
            if (result is AuthResult.Success) {
                val currentUid = AuthManager.getCurrentUserId()
                val newChatRoomId = repository.findUserChatRoomId(currentUid ?: "")
                if (!newChatRoomId.isNullOrBlank()) {
                    _uiState.update { it.copy(chatRoomId = newChatRoomId, isLoading = false) }
                    loadChatRoomName(newChatRoomId)
                    listenForMessages(newChatRoomId)
                    loadChatRoomExtraInfo(newChatRoomId)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else if (result is AuthResult.Failure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    private fun updateChatRoomName(newName: String) {
        val chatRoomId = _uiState.value.chatRoomId ?: return
        viewModelScope.launch {
            try {
                repository.updateChatRoomName(chatRoomId, newName)
                _uiState.update { it.copy(chatRoomName = newName) }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "채팅방 이름 수정 실패", e)
            }
        }
    }

    private suspend fun loadChatRoomExtraInfo(chatRoomId: String) {
        try {
            // 1) 가족 멤버 목록
            val members: List<User> = repository.getFamilyMembers(chatRoomId)

            val familyMembersUi = members.map { user ->
                FamilyMember(
                    id = user.uid,
                    name = user.name.ifBlank { "이름 없음" }
                )
            }

            // 2) 채팅방 생성일
            val createdAt: Date? = repository.getChatRoomCreationDate(chatRoomId)
            val creationDateStr: String = createdAt?.let { date ->
                val formatter = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
                formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                formatter.format(date)
            } ?: ""

            _uiState.update {
                it.copy(
                    familyMembers = familyMembersUi,
                    creationDate = creationDateStr
                )
            }
        } catch (e: Exception) {
            Log.e("MessageViewModel", "loadChatRoomExtraInfo 실패", e)
        }
    }


    fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnMessageTextChanged -> _uiState.update { it.copy(messageText = event.text) }
            is MessageEvent.SendMessage -> sendMessage()
            is MessageEvent.SendImage -> sendImage(event.imageUri)
            MessageEvent.CreateNewChatRoom -> createNewChatRoom()
            MessageEvent.ShowInviteDialog -> _uiState.update { it.copy(showInviteDialog = true) }
            MessageEvent.DismissInviteDialog -> _uiState.update { it.copy(showInviteDialog = false) }
            is MessageEvent.InviteMember -> inviteMember(event.email)
            is MessageEvent.EditChatRoomName -> {
                updateChatRoomName(event.newName)
            }
            is MessageEvent.AcceptInvitation -> acceptInvitation(event.invitationId)
        }
    }

    class MessageViewModelFactory(
        private val repository: AppRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MessageViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}