package com.example.day_together.ui.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day_together.AuthManager
import com.example.day_together.FirebaseService
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.AuthResult
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
    val imageUrl: String = ""
)

/**
 * ChatInfoScreen 및 MessageScreen의 UI 상태 관리 위한 데이터 클래스
 */
data class MessageUiState(
    // 공통 상태
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val chatRoomId: String? = null,
    val chatRoomName: String? = "가족 채팅방 테스트", //기본값
    val currentUserName: String = "사용자",

    // MessageScreen 상태
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
class MessageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private var messagesListener: ListenerRegistration? = null

    init {
        fetchChatRoomInfo()
    }

    override fun onCleared() {
        messagesListener?.remove()
        super.onCleared()
    }

    fun loadChatRoomName(chatRoomId: String){
        FirebaseService.db.collection("chatRooms").document(chatRoomId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "가족 채팅방 테스트"
                    _uiState.update { it.copy(chatRoomName = name) }
                }
            }
            .addOnFailureListener {
                Log.e("MessageViewModel", "채팅방 이름 불러오기 실패", it)
            }
    }
    /**
     * ChatActivity의 fetchAcceptedChatRoomId와 사용자 이름 로딩 로직 통합
     */
    private fun fetchChatRoomInfo() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "로그인이 필요합니다.") }
                Log.d("MessageViewModel", "현재 로그인된 사용자 없음")
                return@launch // 코루틴 종료
            }

            // 사용자 이름과 채팅방 ID를 순서대로 가져옴
            _uiState.update { it.copy(currentUserName = currentUser.name) }
            Log.d("MessageViewModel", "현재 사용자 UID: ${currentUser.uid}, 이름: ${currentUser.name}")
            val chatRoomId = repository.findUserChatRoomId(currentUser.uid)
            Log.d("MessageViewModel", "찾은 채팅방 ID: $chatRoomId")

            if (chatRoomId != null) {
                _uiState.update { it.copy(chatRoomId = chatRoomId) }
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
        val currentState = _uiState.value
        if (currentState.messageText.isBlank() || currentState.chatRoomId == null) return

        repository.sendMessage(
            currentState.chatRoomId, currentState.messageText, currentState.currentUserName,
            imageUrl = TODO()
        )
        _uiState.update { it.copy(messageText = "") }
    }

    private fun sendImage(uri: String){
        val currentState = _uiState.value
        val chatRoomId = currentState.chatRoomId ?: return

        viewModelScope.launch {
            repository.uploadImageToStorage(uri) { imageUrl ->
                if (imageUrl != null) {
                    repository.sendMessage(
                        chatRoomId = chatRoomId,
                        sender = currentState.currentUserName,
                        text = "", // 텍스트는 비워둠
                        imageUrl = imageUrl // 이미지 URL 저장
                    )
                } else {
                    Log.e("MessageViewModel", "이미지 업로드 실패")
                }
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

    private fun updateChatRoomName(newName: String){
        val chatRoomId = _uiState.value.chatRoomId ?: return
        viewModelScope.launch {
            try{
                repository.updateChatRoomName(chatRoomId, newName)
                _uiState.update{ it.copy(chatRoomName = newName)}
            } catch (e: Exception){
                Log.e("MessageViewModel", "채팅방 이름 수정 실패", e)
            }
        }
    }

    fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnMessageTextChanged -> _uiState.update { it.copy(messageText = event.text) }
            MessageEvent.SendMessage -> sendMessage()
            MessageEvent.CreateNewChatRoom -> createNewChatRoom()
            MessageEvent.ShowInviteDialog -> _uiState.update { it.copy(showInviteDialog = true) }
            MessageEvent.DismissInviteDialog -> _uiState.update { it.copy(showInviteDialog = false) }
            is MessageEvent.InviteMember -> inviteMember(event.email)
            is MessageEvent.EditChatRoomName -> {updateChatRoomName(event.newName)}
            else -> {}
        }
    }
}

class MessageViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MessageViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}