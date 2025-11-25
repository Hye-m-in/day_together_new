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


    // '가족 역할' UI를 위한 상태
    val familyMemberSelections: Map<String, Boolean> = emptyMap(),
    val otherFamilyMemberChecked: Boolean = false,
    val otherFamilyMemberText: String = "",

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

    // 가족 역할 선택지 목록
    private val defaultFamilyMembers = listOf("할아버지", "할머니", "아버지", "어머니", "아들", "딸")

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

            // AppRepository.getCurrentUser()가 .toObject(User::class.java)를 사용하는지 확인
            val currentUser = repository.getCurrentUser()
            if (currentUser != null) {

                // 'position' 문자열을 'FamilyMemberSelection' UI 상태로 변환
                val position = currentUser.position // 예: "아버지" 또는 "삼촌"

                // 1. 기본 역할 목록에 position이 포함되어 있는지 확인
                val isDefaultMember = defaultFamilyMembers.contains(position)

                // 2. 기본 역할 목록 기반으로 선택 상태 Map 생성
                val selections = defaultFamilyMembers.associateWith { it == position && isDefaultMember }

                // 3. '기타' 체크 여부 결정
                // null-safe 처리
                val isOther = !position.isNullOrBlank() && !isDefaultMember

                // 4. '기타' 텍스트 설정 ('기타'가 체크되었을 때만 position 값 사용)
                val otherText = if (isOther) position.orEmpty() else ""


                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = currentUser,
                        nameInput = currentUser.name,
                        birthDateInput = currentUser.birthDate ?: "",

                        // 변환된 가족 역할 UI 상태 업데이트
                        familyMemberSelections = selections,
                        otherFamilyMemberChecked = isOther,
                        otherFamilyMemberText = otherText
                        profile_image = currentUser.profile_image ?: ""
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, userMessage = "사용자 정보를 불러올 수 없습니다.") }
            }
        }
    }

    // 이벤트 핸들러: UI의 모든 입력 변경을 처리

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(nameInput = newName, nameError = null) }
    }


    // '가족 역할' UI 이벤트 핸들러 (AuthViewModel과 동일)
    fun onFamilyMemberSelectionChange(member: String, isSelected: Boolean) {
        val updatedSelections = _uiState.value.familyMemberSelections.toMutableMap().apply {
            keys.forEach { put(it, false) }
            put(member, isSelected)
        }
        _uiState.update { it.copy(familyMemberSelections = updatedSelections, otherFamilyMemberChecked = false, otherFamilyMemberText = "") }
    }

    fun onOtherFamilyMemberCheckedChange(isChecked: Boolean) {
        val updatedSelections = _uiState.value.familyMemberSelections.toMutableMap().apply {
            keys.forEach { put(it, false) }
        }
        val newText = if (!isChecked) "" else _uiState.value.otherFamilyMemberText
        _uiState.update { it.copy(otherFamilyMemberChecked = isChecked, otherFamilyMemberText = newText, familyMemberSelections = updatedSelections) }
    }

    fun onOtherFamilyMemberTextChange(text: String) {
        if (text.length <= 10) {
            _uiState.update { it.copy(otherFamilyMemberText = text) }
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

        // 유효성 검사
        if (currentState.nameInput.isBlank()) {
            _uiState.update { it.copy(nameError = "이름을 입력해주세요.") }
            return
        }
        // 생년월일은 수정되지 않으므로, 유효성 검사 제거
        // if (currentState.birthDateInput.length != 8) { ... }

        // 'FamilyMemberSelection' UI 상태를 'position' 문자열로 변환
        val selectedPosition = currentState.familyMemberSelections.filterValues { it }.keys.firstOrNull()
        val position = if (currentState.otherFamilyMemberChecked) {
            currentState.otherFamilyMemberText
        } else {
            selectedPosition ?: "" // 선택된 것이 없으면 빈 문자열
        }

        if (position.isBlank()) {
            _uiState.update { it.copy(userMessage = "가족 역할을 선택해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 비밀번호 변경 로직
            if (currentState.newPasswordInput.isNotEmpty()) {
                // 1. 기존 비밀번호 입력 확인
                if (currentState.oldPasswordInput.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "기존 비밀번호를 입력해주세요.") }
                    return@launch
                }

                // 2. 새 비밀번호 확인 일치 여부 (UI 상태에서 이미 에러가 있는지 확인)
                if (currentState.passwordError != null) {
                    _uiState.update { it.copy(isLoading = false, userMessage = "새 비밀번호를 확인해주세요.") }
                    return@launch
                }

                // login -> changePassword(old, new) 로직 변경
                // 이 함수 내부에서 기존 비밀번호로 재인증 후 새 비밀번호로 변경함
                val result = repository.changePassword(currentState.oldPasswordInput, currentState.newPasswordInput)

                if (result is AuthResult.Failure) {
                    _uiState.update { it.copy(isLoading = false, userMessage = result.message) }
                    return@launch
                }
            }

            // 사용자 정보 업데이트 로직
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
                position = position, // 변환된 position 문자열 저장
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
