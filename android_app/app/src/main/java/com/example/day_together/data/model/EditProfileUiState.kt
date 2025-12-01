
package com.example.day_together.data.model

/**
 * 개인정보 수정 화면의 모든 UI 상태를 나타내는 데이터 클래스
 */
data class EditProfileUiState @JvmOverloads constructor(
    // 데이터 로딩 상태
    val isLoading: Boolean = true,
    // 저장 완료 후 화면을 닫기 위한 신호
    val isSaveSuccess: Boolean = false,
    // 원본 사용자 정보
    val user: User? = null,

    // UI 입력 필드 상태
    val nameInput: String = "",
    val birthDateInput: String = "",
    val isLunar: Boolean = false,
    val positionInput: String = "", // 가족 역할
    val oldPasswordInput: String = "",
    val newPasswordInput: String = "",
    val confirmNewPasswordInput: String = "",

    // 유효성 검사 에러 메시지
    val nameError: String? = null,
    val birthDateError: String? = null,
    val passwordError: String? = null,

    // 토스트 메세지
    val userMessage: String? = null
)