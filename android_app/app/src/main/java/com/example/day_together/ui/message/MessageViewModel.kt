package com.example.day_together.ui.message

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
    val imageUrl: String = ""
)

/**
 * ChatInfoScreen 및 MessageScreen의 UI 상태 관리 위한 데이터 클래스
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
    // Search, DatePicker, Attachment 관련 상태 삭제
    // val searchText: String = "",
    // val showSearchBar: Boolean = false,
    // val showDatePicker: Boolean = false,
    // val showAttachmentOptions: Boolean = false,
    // val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    // val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    // val selectedDisplayDate: Int? = null,
    // val datesWithConversations: Set<LocalDate> = emptySet(),

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
    // 클래스 이름은 대문자로 시작
    data class AcceptInvitation(val invitationId: String) : MessageEvent
    data object CreateNewChatRoom : MessageEvent

    // 사용하지 않는 이벤트 삭제
    // data class OnSearchTextChanged(val text: String) : MessageEvent
    // data object ToggleSearchBar : MessageEvent
    // data object ToggleDatePicker : MessageEvent
    // data object DismissDatePicker : MessageEvent
    // data object ToggleAttachmentPanel : MessageEvent
    // data class SelectDate(val year: Int, val month: Int, val day: Int) : MessageEvent
    // data class ChangeMonth(val year: Int, val month: Int) : MessageEvent
}

/**
 * MessageScreen과 ChatInfoScreen의 상태 및 로직 담당 ViewModel
 */
// 생성자에서 questionRepository 삭제
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

    // AppRepository를 사용하도록 코루틴 함수로 변경
    private suspend fun loadChatRoomName(chatRoomId: String) {
        try {
            val name = repository.getChatRoomName(chatRoomId) ?: "가족 채팅방"
            _uiState.update { it.copy(chatRoomName = name) }
        } catch (e: Exception) {
            Log.e("MessageViewModel", "채팅방 이름 불러오기 실패", e)
        }
    }

    private fun listenForMessages(chatRoomId: String) {
        messagesListener?.remove()
        messagesListener = repository.listenForMessages(chatRoomId) { newMessages ->
            // content와 imageUrl은 non-nullable이므로 Elvis operator 삭제
            val updatedMessages = newMessages.map { msg ->
                msg.copy(
                    content = msg.content,
                    imageUrl = msg.imageUrl
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

        if (messageText.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(
                chatRoomId = chatRoomId,
                sender = currentState.currentUserName,
                text = messageText,
                imageUrl = null
                // type 파라미터 삭제 (repository.sendMessage 함수 시그니처 변경됨)
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
                        imageUrl = imageUrl
                        // type 파라미터 삭제
                    )
                    Log.d("MessageViewModel", "이미지 메시지 전송 완료")
                } else {
                    Log.e("MessageViewModel", "이미지 업로드 실패")
                    _uiState.update { it.copy(errorMessage = "이미지 업로드에 실패했습니다.") }
                }
            }
        }
    }

    // AppRepository의 getTodaysQuestion 함수를 사용하도록 변경
    private fun fetchTodayQuestion() {
        viewModelScope.launch {
            val question = repository.getTodaysQuestion() // 코루틴으로 호출
            question?.let { q ->
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(
                    ChatMessage(content = q, sender = "system")
                )
                _uiState.update { it.copy(messages = currentMessages) }
            }
        }
    }

    private fun fetchChatRoomInfo() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "로그인이 필요합니다.") }
                return@launch
            }

            _uiState.update { it.copy(currentUserName = currentUser.name) }

            val chatRoomId = repository.findUserChatRoomId(currentUser.uid)
            if (chatRoomId != null) {
                _uiState.update { it.copy(chatRoomId = chatRoomId) }
                loadChatRoomName(chatRoomId) // [수정] suspend 함수이므로 launch 스코프 내에서 호출
                listenForMessages(chatRoomId)
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
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "채팅방 생성에 실패했습니다.") }
            }
        }
    }

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
                _uiState.update { it.copy(showInviteDialog = false, isLoading = false) }
            } else if (result is AuthResult.Failure) {
                _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    // private으로 변경
    private fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.acceptInvitation(invitationId)
            if (result is AuthResult.Success) {
                // 수락 성공: 사용자의 채팅방 ID 다시 조회하거나 invitation 문서에서 chatRoomId를 읽어 업데이트
                val currentUid = AuthManager.getCurrentUserId()
                val newChatRoomId = repository.findUserChatRoomId(currentUid ?: "")
                if (!newChatRoomId.isNullOrBlank()) {
                    _uiState.update { it.copy(chatRoomId = newChatRoomId, isLoading = false) }
                    loadChatRoomName(newChatRoomId) // [수정] suspend 함수이므로 launch 스코프 내에서 호출
                    listenForMessages(newChatRoomId)
                } else {
                    // 안전장치: 실패했지만 수락 자체는 되었을 가능성 -> UI 상태 정리
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
            // 클래스 이름 변경에 따라 수정
            is MessageEvent.AcceptInvitation -> acceptInvitation(event.invitationId)
        }
    }

    // 생성자에서 questionRepository 삭제
    class MessageViewModelFactory(
        private val repository: AppRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                // 생성자에서 questionRepository 삭제
                return MessageViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}