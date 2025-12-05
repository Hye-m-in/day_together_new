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

import com.google.firebase.firestore.ListenerRegistration



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
    val newCommentText: String = "",

    val currentUserName: String = "" // 현재 로그인한 사용자 이름 저장용
)

class GalleryViewModel(
    // 리스너 등록 객체 (나중에 연결 끊을 때 사용)
    private var commentListener: ListenerRegistration? = null,

    private val repository: AppRepository = AppRepository
) : ViewModel() {

    // 1. 상태(State) 관리
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()


    init {
        viewModelScope.launch {
            // [1] 사용자 정보 가져오기
            val user = repository.getCurrentUser()
            val chatRoomId = user?.invitedChatRoomId ?: repository.getMyChatRoomId()

            // [2] 이름 가져오기
            // 1순위: DB에 저장된 이름 (user.name)
            // 2순위: 로그인된 계정의 프로필 이름 (Firebase Auth)
            // 3순위: 둘 다 없으면 "알 수 없음" (나중에 "가족"으로 변환됨)

            var myName = user?.name

            if (myName.isNullOrBlank()) {
                // DB에 이름이 없으면 구글/네이버 로그인 정보에서 이름 가져오기 시도
                myName = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
            }

            // 로그로 확인 (Logcat에서 "GalleryViewModel" 검색해보세요)
            android.util.Log.d("GalleryViewModel", "가져온 내 이름: $myName")

            // [3] 상태 업데이트
            _uiState.update {
                it.copy(
                    chatRoomId = chatRoomId,
                    currentUserName = myName ?: "", // 없으면 빈 문자열

                    isLoading = chatRoomId != null //채팅방 ID가 있으면 상태 유지, 없으면 로딩 끝냄
                )
            }

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

                val distinctYearMonths =
                    (photosByYearMonth.keys + currentState.currentDisplayYearMonth)
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

    /** 댓글 목록을 실시간으로 감지함 */
    private fun loadCommentsFor(yearMonth: YearMonth) {
        val chatRoomId = _uiState.value.chatRoomId ?: return

        // 기존에 연결된 리스너가 있다면 끊어줌 (중복 방지)
        commentListener?.remove()

        // 새로운 리스너 연결
        commentListener =
            repository.listenForMonthlyComments(chatRoomId, yearMonth) { updatedComments ->
                // DB에 데이터가 변경될 때마다 이 코드가 실행됨 -> UI 자동 갱신
                _uiState.update {
                    it.copy(comments = updatedComments)
                }
            }
    }


    // ViewModel이 아예 사라질 때도 안전하게 제거
    override fun onCleared() {
        super.onCleared()
        commentListener?.remove()
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

        val currentTimestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // 이름 결정 로직
        val safeAuthorName = _uiState.value.currentUserName.ifBlank { "익명" }

        val newComment = MonthlyComment(
            author = safeAuthorName,
            text = _uiState.value.newCommentText,
            timestamp = currentTimestamp
        )

        val targetYearMonth = _uiState.value.commentSheetYearMonth ?: return
        val chatRoomId = _uiState.value.chatRoomId ?: return

        _uiState.update { it.copy(newCommentText = "") }

        viewModelScope.launch {
            repository.addMonthlyComment(chatRoomId, targetYearMonth, newComment)
        }
    }
}