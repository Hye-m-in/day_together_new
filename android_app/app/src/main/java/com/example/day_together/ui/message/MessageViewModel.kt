package com.example.day_together.ui.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.R
import com.example.day_together.data.model.ChatMessage
import com.example.day_together.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale



/**
 * ChatInfoScreen 및 MessageScreen의 UI 상태 관리 위한
 * (임시)데이터 클래스
 */
data class MessageUiState(
    // 공통 상태
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val chatRoomId: String? = null,

    // MessageScreen 상태
    val messages: List<MessageItem> = emptyList(),
    val pinnedMessage: MessageItem? = null, // 고정 메시지
    val datesWithConversations: Set<LocalDate> = emptySet(), // 대화가 있는 날짜
    val messageText: String = "", // 메시지 입력창 텍스트
    val searchText: String = "", // 검색창 텍스트
    val showSearchBar: Boolean = false, // 검색창 표시 여부
    val showDatePicker: Boolean = false, // 날짜 선택기 표시 여부
    val showAttachmentOptions: Boolean = false, // 첨부 파일 패널 표시 여부
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // Calendar의 월(0~11)
    val selectedDisplayDate: Int? = null, // 달력에 선택된 날짜

    // ChatInfoScreen 상태
    val myUserName: String = "사용자",
    val creationDate: String = "",
    val familyMembers: List<FamilyMember> = emptyList(),
    val showInviteDialog: Boolean = false, // 초대 다이얼로그 표시 여부
    val inviteEmailInput: String = "" // 초대할 이메일 입력 텍스트
)

/**
 * UI에서 발생하는 모든 사용자 이벤트를 정의
 * MessageScreen과 ChatInfoScreen의 모든 상호작용이 포함돼있음
 */
sealed interface MessageEvent {
    // MessageScreen 이벤트
    data class OnMessageTextChanged(val text: String) : MessageEvent
    data class OnSearchTextChanged(val text: String) : MessageEvent
    data object SendMessage : MessageEvent
    data object ToggleSearchBar : MessageEvent
    data object ToggleDatePicker : MessageEvent
    data object DismissDatePicker : MessageEvent
    data object ToggleAttachmentPanel : MessageEvent
    data class SelectDate(val year: Int, val month: Int, val day: Int) : MessageEvent
    data class ChangeMonth(val year: Int, val month: Int) : MessageEvent

    // ChatInfoScreen 이벤트
    data object ShowInviteDialog : MessageEvent
    data object DismissInviteDialog : MessageEvent
    data class InviteMember(val email: String) : MessageEvent
}

/**
 * MessageScreen과 ChatInfoScreen의 상태 및 로직 담당 ViewModel
 */
class MessageViewModel(
    private val repository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // TODO : Repository로부터 실제 데이터를 가져오는 로직 필요
                // UI 파일 참고해서 임시 데이터로 초기화 함
                val chatRoomId = "testRoomId"
                val currentUser = repository.getCurrentUser() // AppRepository 임시 데이터 사용

                // 가족 멤버 정보 (ChatInfoScreen)
                val familyMembers = listOf(
                    FamilyMember("1", "엄마"), FamilyMember("2", "아빠"),
                    FamilyMember("3", "동생"), FamilyMember("4", "누나"), FamilyMember("5", "형")
                )
                val creationDate = "2024.01.01"

                // 메시지 정보 (MessageScreen)
                val chatMessages = repository.getChatMessages(chatRoomId).mapIndexed { index, chatMsg ->
                    MessageItem(
                        id = index,
                        text = chatMsg.content,
                        time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(chatMsg.timestamp),
                        isSentByMe = chatMsg.sender == currentUser?.name,
                        senderName = chatMsg.sender
                    )
                }

                // 대화가 있는 날짜 정보 (MessageScreen)
                val datesWithConversations = setOf(
                    LocalDate.now().minusDays(2), LocalDate.now().minusDays(5)
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        chatRoomId = chatRoomId,
                        myUserName = currentUser?.name ?: "사용자",
                        familyMembers = familyMembers,
                        creationDate = creationDate,
                        messages = chatMessages,
                        datesWithConversations = datesWithConversations
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                Log.e("MessageViewModel", "초기 데이터 로딩 실패", e)
            }
        }
    }

    /**
     * UI에서 발생한 모든 이벤트 처리하는 함수
     */
    fun onEvent(event: MessageEvent) {
        when (event) {
            // MessageScreen.kt의 모든 이벤트를 반영
            is MessageEvent.OnMessageTextChanged -> _uiState.update { it.copy(messageText = event.text) }
            is MessageEvent.OnSearchTextChanged -> _uiState.update { it.copy(searchText = event.text) }
            MessageEvent.SendMessage -> sendMessage()
            MessageEvent.ToggleSearchBar -> _uiState.update {
                val newState = !it.showSearchBar
                // 검색창이 켜지면 달력은 닫히도록 UI 로직 반영
                it.copy(showSearchBar = newState, showDatePicker = if (newState) false else it.showDatePicker)
            }
            MessageEvent.ToggleDatePicker -> _uiState.update {
                val newState = !it.showDatePicker
                // 달력이 켜지면 검색창은 닫히도록 UI 로직 반영
                it.copy(showDatePicker = newState, showSearchBar = if (newState) false else it.showSearchBar)
            }
            MessageEvent.DismissDatePicker -> _uiState.update { it.copy(showDatePicker = false) }
            MessageEvent.ToggleAttachmentPanel -> _uiState.update { it.copy(showAttachmentOptions = !it.showAttachmentOptions) }
            is MessageEvent.SelectDate -> {
                _uiState.update { it.copy(selectedDisplayDate = event.day, showDatePicker = false) }
                // TODO: 선택된 날짜(event.year, event.month, event.day)의 메시지를 Repository에서 로드하는 로직
            }
            is MessageEvent.ChangeMonth -> {
                _uiState.update { it.copy(selectedYear = event.year, selectedMonth = event.month, selectedDisplayDate = null) }
                // TODO: 변경된 월(event.year, event.month)에 대화가 있는 날짜들을 Repository에서 로드하는 로직
            }

            // ChatInfoScreen.kt의 모든 이벤트 반영
            MessageEvent.ShowInviteDialog -> _uiState.update { it.copy(showInviteDialog = true) }
            MessageEvent.DismissInviteDialog -> _uiState.update { it.copy(showInviteDialog = false, inviteEmailInput = "") }
            is MessageEvent.InviteMember -> inviteMember(event.email)
        }
    }

    private fun sendMessage() {
        val uiState = _uiState.value
        val chatRoomId = uiState.chatRoomId
        if (uiState.messageText.isBlank() || chatRoomId == null) return

        val newMessage = MessageItem(
            id = (uiState.messages.maxOfOrNull { it.id } ?: 0) + 1,
            text = uiState.messageText,
            time = "방금",
            isSentByMe = true,
            senderName = uiState.myUserName
        )

        // UI 즉시 업데이트
        _uiState.update {
            it.copy(
                messages = it.messages + newMessage,
                messageText = ""
            )
        }

        // Repository에 실제 데이터 전송 요청
        viewModelScope.launch {
            try {
                repository.sendMessage(chatRoomId, uiState.messageText, uiState.myUserName)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "메시지 전송에 실패했습니다.") }
            }
        }
    }

    private fun inviteMember(email: String) {
        _uiState.update { it.copy(showInviteDialog = false) }
        viewModelScope.launch {
            try {
                // TODO: AppRepository에 초대 관련 함수를 만들고 호출
                Log.d("MessageViewModel", "Repository에 초대 요청: $email")
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "초대에 실패했습니다.") }
            }
        }
    }
}