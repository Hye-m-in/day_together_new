package com.example.day_together.ui.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.day_together.AuthManager
import com.example.day_together.FirebaseService
import com.example.day_together.data.model.User 
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.AuthResult
import com.example.day_together.data.repository.QuestionRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

/**
 * 채팅 메시지를 위한 데이터 클래스
 * Firestore의 자동 변환을 위해 모든 속성에 기본값 할당
 */
data class ChatMessage(
    val content: String = "",
    val sender: String = "",
    val timestamp: Date = Date(),
    val imageUrl: String? = null 
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

    val messages: List<ChatMessage> = emptyList(),
    val messageText: String = "",
    val searchText: String = "",
    val showSearchBar: Boolean = false,
    val showDatePicker: Boolean = false,
    val showAttachmentOptions: Boolean = false,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedDisplayDate: Int? = null,
    val datesWithConversations: Set<LocalDate> = emptySet(),

    val creationDate: String = "",
    val familyMembers: List<FamilyMember> = emptyList(),
    val showInviteDialog: Boolean = false,
)

/**
 * UI에서 발생하는 모든 사용자 이벤트를 정의
 */
sealed interface MessageEvent {
    data class OnMessageTextChanged(val text: String) : MessageEvent
    data class OnSearchTextChanged(val text: String) : MessageEvent
    data object SendMessage : MessageEvent
    data class SendImage(val uri: String) : MessageEvent
    data object ToggleSearchBar : MessageEvent
    data object ToggleDatePicker : MessageEvent
    data object DismissDatePicker : MessageEvent
    data object ToggleAttachmentPanel : MessageEvent
    data class SelectDate(val year: Int, val month: Int, val day: Int) : MessageEvent
    data class ChangeMonth(val year: Int, val month: Int) : MessageEvent
    data object ShowInviteDialog : MessageEvent
    data class EditChatRoomName(val newName: String) : MessageEvent
    data object DismissInviteDialog : MessageEvent
    data class InviteMember(val email: String) : MessageEvent
    data object CreateNewChatRoom : MessageEvent
}

/**
 * MessageScreen과 ChatInfoScreen의 상태 및 로직 담당 ViewModel
 */
open class MessageViewModel(
    private val repository: AppRepository,
    private val questionRepository: QuestionRepository
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

    fun loadChatRoomName(chatRoomId: String) {
        FirebaseService.db.collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("chatRoomName") ?: "가족 채팅방"
                    _uiState.update { it.copy(chatRoomName = name) }
                }
            }
            .addOnFailureListener {
                Log.e("MessageViewModel", "채팅방 이름 불러오기 실패", it)
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

            _uiState.update { it.copy(currentUser = currentUser, currentUserName = currentUser.name) }
            val chatRoomId = repository.findUserChatRoomId(currentUser.uid)

            if (chatRoomId != null) {
                _uiState.update { it.copy(chatRoomId = chatRoomId) }
                loadChatRoomName(chatRoomId)
                listenForMessages(chatRoomId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }


    private fun listenForMessages(chatRoomId: String) {
        messagesListener?.remove()
        messagesListener = repository.listenForMessages(chatRoomId) { newMessages ->
            _uiState.update {
                it.copy(messages = newMessages, isLoading = false)
            }
        }
    }

    private fun sendMessage() {
        val uiState = _uiState.value
        val currentUser = uiState.currentUser ?: return
        if (uiState.messageText.isBlank() || uiState.chatRoomId == null) {
            return
        }

        // suspend 함수는 viewModelScope.launch 안에서 호출
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    chatRoomId = uiState.chatRoomId,
                    sender = currentUser.name,
                    text = uiState.messageText,
                    imageUrl = null
                    // onComplete 콜백 없음
                )
                _uiState.update { it.copy(messageText = "") }
            } catch (e: Exception) {
                Log.e("MessageViewModel", "메시지 전송 실패", e)
            }
        }
    }

    private fun sendImage(uri: String) {
        val currentState = _uiState.value
        val chatRoomId = currentState.chatRoomId ?: return
        val currentUser = currentState.currentUser ?: return

        // uploadImageToStorage는 콜백 사용
        repository.uploadImageToStorage(uri) { imageUrl ->
            if (imageUrl != null) {
                // sendMessage는 suspend 함수이므로 viewModelScope.launch로 호출
                viewModelScope.launch {
                    try {
                        repository.sendMessage(
                            chatRoomId = chatRoomId,
                            sender = currentUser.name,
                            text = "",
                            imageUrl = imageUrl
                            // onComplete 콜백 없음
                        )
                    } catch (e: Exception) {
                        Log.e("MessageViewModel", "이미지 메시지 전송 실패", e)
                    }
                }
            } else {
                Log.e("MessageViewModel", "이미지 업로드 실패")
            }
        }
    }

    private fun fetchTodayQuestion() {
        val uid = AuthManager.getCurrentUserId() ?: return
        questionRepository.loadTodayQuestion(uid) { question ->
            question?.let { q ->
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(
                    ChatMessage(content = q, sender = "system")
                )
                _uiState.update { it.copy(messages = currentMessages) }
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
        val uiState = _uiState.value
        val chatRoomId = uiState.chatRoomId
        val inviterId = AuthManager.getCurrentUserId()

        if (chatRoomId == null || inviterId == null) {
            _uiState.update { it.copy(errorMessage = "초대 정보를 보낼 수 없습니다.") }
            return
        }

        _uiState.update { it.copy(showInviteDialog = false, isLoading = true) }

        viewModelScope.launch {
            val result = repository.inviteMember(chatRoomId, inviterId, email)
            if (result is AuthResult.Failure) {
                _uiState.update { it.copy(errorMessage = result.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
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
            is MessageEvent.SendImage -> sendImage(event.uri)
            MessageEvent.SendMessage -> sendMessage()
            MessageEvent.CreateNewChatRoom -> createNewChatRoom()
            MessageEvent.ShowInviteDialog -> _uiState.update { it.copy(showInviteDialog = true) }
            MessageEvent.DismissInviteDialog -> _uiState.update { it.copy(showInviteDialog = false) }
            is MessageEvent.InviteMember -> inviteMember(event.email)
            is MessageEvent.EditChatRoomName -> updateChatRoomName(event.newName)
            else -> {}
        }
    }
}

class MessageViewModelFactory(
    private val repository: AppRepository,
    private val questionRepository: QuestionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageViewModel(repository, questionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}