package com.example.day_together.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.model.User
import com.example.day_together.data.repository.AuthResult
import com.example.day_together.data.repository.AppRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 개인정보 수정 화면의 모든 UI 상태를 나타내는 데이터 클래스
 * ViewModel과 같은 파일에 두어 참조 및 중복 선언 오류 방지
 */

data class EditProfileUiState(
    // 데이터 로딩 상태
    val isLoading: Boolean = true,
    // 저장 완료 후 화면을 닫기 위한 신호
    val isSaveSuccess: Boolean = false,
    // 원본 사용자 정보
    val user: User? = null,

    // UI 입력 필드 상태
    val profile_image: String = "",
    val nameInput: String = "",
    val birthDateInput: String = "",
    val positionInput: String = "", // 가족 역할
    val oldPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmNewPasswordInput: String = "",
    val newProfileImageUri: Uri ?= null,
    val isOtherPositionSelected: Boolean = false, // '기타' 선택 여부
    val otherPositionText: String = "", // '기타' 텍스트

    // 유효성 검사 에러 메시지
    val nameError: String? = null,
    val birthDateError: String? = null,
    val passwordError: String? = null,

    // 사용자에게 보여줄 일회성 메시지 (Toast 등)
    val userMessage: String? = null
)

/**
 * 개인정보 수정 화면의 UI 상태와 비즈니스 로직을 관리하는 ViewModel
 */
class EditProfileViewModel(
    private val repository: AppRepository = AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        // ViewModel이 생성될 때 현재 로그인된 사용자의 정보를 불러옴
        loadUserProfile()
    }

    /**
     * Repository로부터 '현재 로그인된 사용자'의 정보를 불러오는 함수
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentUser = repository.getCurrentUser()
            if (currentUser != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = currentUser,
                        nameInput = currentUser.name,
                        positionInput = currentUser.position ?: "",
                        birthDateInput = currentUser.birthDate ?: "",
                        profile_image = currentUser.profile_image ?: ""
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, userMessage = "사용자 정보를 불러올 수 없습니다.") }
            }
        }
    }

    // --- 이벤트 핸들러: UI의 모든 입력 변경을 처리 ---

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(nameInput = newName, nameError = null) }
    }

    fun onBirthDateChange(newDate: String) {
        if (newDate.length <= 8) {
            val error = if (newDate.isNotEmpty() && newDate.length < 8) "8자리로 입력해주세요." else null
            _uiState.update { it.copy(birthDateInput = newDate, birthDateError = error) }
        }
    }

    fun onPositionChange(newPosition: String) {
        _uiState.update {
            it.copy(
            positionInput = newPosition,
            isOtherPositionSelected = false,
            otherPositionText = ""
            )
        }
    }

    fun onOtherPositionChecked(isChecked: Boolean) {
        val newText = if (!isChecked) "" else _uiState.value.otherPositionText
        _uiState.update { it.copy(isOtherPositionSelected = isChecked, otherPositionText = newText) }
    }

    fun onOtherPositionTextChange(text: String) {
        if (text.length <= 10) {
            _uiState.update { it.copy(otherPositionText = text, positionInput = text) }
        }
    }

    fun onOldPasswordChange(password: String) {
        _uiState.update { it.copy(oldPasswordInput = password, passwordError = null) }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update { it.copy(newPasswordInput = password, passwordError = null) }
    }

    fun onConfirmNewPasswordChange(password: String) {
        val error = if (password.isNotEmpty() && password != _uiState.value.newPasswordInput) "새 비밀번호가 일치하지 않습니다." else null
        _uiState.update { it.copy(confirmNewPasswordInput = password, passwordError = error) }
    }

    /** 화면에서 이미지 선택 시 로컬 Uri를 저장(프리뷰) */
    fun onProfileImageChanged(uri: Uri?) {
        // 프리뷰용으로 로컬 Uri만 저장하고, 업로드는 별도 버튼(또는 onSaveClicked에서)으로 처리할 수 있음
        _uiState.update { it.copy(newProfileImageUri = uri) }
    }

    /**
     * '완료' 버튼 클릭 시 호출되는 메인 저장 함수
     */
    fun onSaveClicked() {
        val originalUser = _uiState.value.user ?: return
        val currentState = _uiState.value

        // --- 유효성 검사 ---
        if (currentState.nameInput.isBlank()) {
            _uiState.update { it.copy(nameError = "이름을 입력해주세요.") }
            return
        }
        if (currentState.birthDateInput.length != 8) {
            _uiState.update { it.copy(birthDateError = "생년월일을 8자리로 입력해주세요.")}
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // --- 비밀번호 변경 로직 ---
            if (currentState.newPasswordInput.isNotEmpty()) {
                // 기존 비밀번호 재검증
                val loginResult = repository.login(originalUser.email, currentState.oldPasswordInput)
                if (loginResult is AuthResult.Failure) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "기존 비밀번호가 일치하지 않습니다.") }
                    return@launch
                }
                // 새 비밀번호 확인
                if (currentState.passwordError != null) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "비밀번호를 확인해주세요.") }
                    return@launch
                }

                // 비밀번호 변경
                repository.changePassword(originalUser.email, currentState.newPasswordInput)
            }

            var profileUrl = originalUser.profile_image

            // 프로필 이미지 변경이 있는 경우에만 업로드
            val newUri = currentState.newProfileImageUri
            if (newUri != null) {
                try {
                    profileUrl = repository.uploadProfileImage(newUri)
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "이미지 업로드 실패") }
                    return@launch
                }
            }

            // --- 사용자 정보 업데이트 로직 ---
            val updatedUser = originalUser.copy(
                name = currentState.nameInput,
                position = currentState.positionInput,
                birthDate = currentState.birthDateInput,
                profile_image = profileUrl
            )
            repository.updateUser(updatedUser)

            _uiState.update { it.copy(isLoading = false, isSaveSuccess = true, userMessage = "성공적으로 저장되었습니다.") }
        }
    }

    // 화면에 표시된 메시지를 초기화하는 함수
    fun userMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
