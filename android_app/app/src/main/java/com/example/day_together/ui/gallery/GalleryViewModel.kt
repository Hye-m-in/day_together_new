package com.example.day_together.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth



/**
 * 데이터를 한 곳(ViewModel)에서만 통제함으로써 코드가 꼬이는 것을 막음
 * UI 화면이 마음대로 데이터를 바꾸면 앱이 복잡해질수록 어디서 버그가 생기는지 찾기 매우 어려워짐
 */

/**
 * 갤러리 화면의 UI 상태를 나타내는 데이터 클래스
 *
 * @property isLoading 데이터 로딩 중인지
 * @property allMonthlyPhotoGroups 월별로 그룹화된 모든 사진 목록
 * @property currentDisplayYearMonth 현재 사용자가 보고 있거나 선택한 년/월
 * @property commentSheetYearMonth 댓글창이 열린 해당 년/월. null이면 닫힌 상태
 * @property comments 현재 열린 댓글창 댓글 목록
 * @property newCommentText 사용자가 입력 중인 댓글
 */
data class GalleryUiState(
    val chatRoomId: String? = null,
    val isLoading: Boolean = true,
    val allMonthlyPhotoGroups: List<MonthlyPhotoGroupData> = emptyList(),
    val currentDisplayYearMonth: YearMonth = YearMonth.now(),
    val commentSheetYearMonth: YearMonth? = null,
    val comments: List<MonthlyComment> = emptyList(),
    val newCommentText: String = ""
)

class GalleryViewModel(
    private val repository: AppRepository = AppRepository
) : ViewModel() {

    // 1. 상태(State) 관리
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            // invitedChatRoomId 우선, 없으면 getMyChatRoomId() 확인
            val chatRoomId = user?.invitedChatRoomId ?: repository.getMyChatRoomId()

            // chatRoomId를 상태에 저장
            _uiState.update { it.copy(chatRoomId = chatRoomId) }

            chatRoomId?.let { loadImages(it) }
        }
    }

    /**
     * 2. 데이터 로딩
     * Repository에서 사진 데이터를 비동기적으로 불러와 UI 상태 업데이트
     * 로딩 시작 -> 데이터 요청 -> 상태 업데이트
     */
    private fun loadImages(chatRoomId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val photos = repository.getImages(chatRoomId)

            _uiState.update { currentState ->
                val photosByYearMonth = photos.groupBy { photo ->
                    YearMonth.from(LocalDate.parse(photo.date))
                }

                val distinctYearMonths = (photosByYearMonth.keys + currentState.currentDisplayYearMonth)
                    .distinct()
                    .sorted()

                val monthlyGroups = distinctYearMonths.map { ym ->
                    MonthlyPhotoGroupData(
                        yearMonth = ym,
                        photos = photosByYearMonth[ym]?.sortedBy { it.date } ?: emptyList()
                    )
                }

                currentState.copy(
                    isLoading = false,
                    allMonthlyPhotoGroups = monthlyGroups
                )
            }
        }
    }

    /** 특정 월의 댓글 목록을 불러옴. */
    private fun loadCommentsFor(yearMonth: YearMonth) {
        val chatRoomId = _uiState.value.chatRoomId ?: return

        viewModelScope.launch {
            // chatRoomId를 함께 전달
            val comments = repository.getMonthlyComments(chatRoomId, yearMonth)
            _uiState.update { it.copy(comments = comments) }
        }
    }

    /** 3. 이벤트 처리 함수
     * 타임피커에서 특정 년/월을 선택했을 때 호출
     * 선택된 월을 UI 상태에 반영하고, 해당 월의 사진 그룹이 없다면 빈 그룹을 생성함
     *
     * */
    fun onYearMonthSelected(yearMonth: YearMonth) {
        _uiState.update { currentState ->
            // 현재 상태에서 선택된 년/월만 업데이트
            val stateWithUpdatedMonth = currentState.copy(currentDisplayYearMonth = yearMonth)

            // 새로 선택된 월에 해당하는 사진 그룹이 없으면, 빈 그룹 추가
            if (stateWithUpdatedMonth.allMonthlyPhotoGroups.none { it.yearMonth == yearMonth }) {
                val newGroup = MonthlyPhotoGroupData(yearMonth = yearMonth, photos = emptyList())
                val updatedGroups = (stateWithUpdatedMonth.allMonthlyPhotoGroups + newGroup)
                    .sortedBy { it.yearMonth }
                // 그룹 목록까지 업데이트된 최종 상태를 반환함
                stateWithUpdatedMonth.copy(allMonthlyPhotoGroups = updatedGroups)
            } else {
                // 사진 그룹이 이미 있으면, 년/월만 업데이트된 상태로 반환
                stateWithUpdatedMonth
            }
        }
    }

    // 댓글 아이콘을 클릭했을 때 호출
    fun onCommentIconClicked(yearMonth: YearMonth) {
        _uiState.update { it.copy(commentSheetYearMonth = yearMonth) }
        loadCommentsFor(yearMonth)
    }

    // 댓글 BottomSheet가 닫혔을 때 호출
    fun onCommentSheetDismissed() {
        _uiState.update { it.copy(commentSheetYearMonth = null, comments = emptyList()) }
    }

    // 댓글 입력창의 텍스트가 변경될 때마다 호출
    fun onNewCommentChange(text: String) {
        _uiState.update { it.copy(newCommentText = text) }
    }

    // '전송' 버튼을 눌러 댓글을 등록할 때 호출
    fun onSendComment() {
        if (_uiState.value.newCommentText.isBlank()) return

        val newComment = MonthlyComment(
            author = "나", // 실제 앱에서는 로그인된 유저 정보를 사용해야 함
            text = _uiState.value.newCommentText,
            timestamp = "방금 전"
        )
        val targetYearMonth = _uiState.value.commentSheetYearMonth ?: return
        val chatRoomId = _uiState.value.chatRoomId ?: return

        // '낙관적 업데이트': 서버 응답을 기다리지 않고 UI에 먼저 변경사항 반영
        _uiState.update {
            it.copy(
                comments = listOf(newComment) + it.comments, // 새 댓글을 목록 맨 앞에 추가
                newCommentText = "" // 입력창 비우기
            )
        }

        // 실제 서버(Repository)에 댓글 추가 요청
        viewModelScope.launch {
            // chatRoomId를 함께 전달
            repository.addMonthlyComment(chatRoomId, targetYearMonth, newComment)
            // TODO: 요청 성공/실패에 따른 추가 로직 (예: 에러 메시지 표시, 실패 시 롤백 등)
        }
    }
}