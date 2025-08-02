package com.example.day_together.ui.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.repository.AuthRepository
import com.example.day_together.data.repository.FakeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 로그인, 회원가입, 계정 찾기 등 인증 관련 화면들의
 * UI 상태와 비즈니스 로직을 모두 관리하는 ViewModel
 */
class AuthViewModel : ViewModel() {

    // 데이터 통신을 담당하는 가짜 저장소
    //private val repository = FakeRepository()
    //TODO:가짜 저장소 뺄 수 있게 수정하기 -> 일단은 실제 저장소 AuthRepository() 만들어놨습니다. 서버 통신해서 구글/네이버 로그인 되는지 확인해야해요

    //실제 서버 호출을 담당할 레포지토리
    private val repository = AuthRepository()

    // 인증 화면들의 모든 UI 상태를 담는 StateFlow
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    // --- 이벤트 핸들러 함수들 ---
    fun onLoginEmailChange(email: String) { _uiState.update { it.copy(loginEmail = email, loginError = null) } }
    fun onLoginPasswordChange(password: String) { _uiState.update { it.copy(loginPassword = password, loginError = null) } }

    fun onSignUpNameChange(name: String) { _uiState.update { it.copy(signUpName = name) } }
    fun onSignUpBirthDateChange(date: String) {
        if (date.length <= 8) {
            val error = if (date.isNotEmpty() && date.length != 8) "8자리 형식(YYYYMMDD)으로 입력해주세요." else null
            _uiState.update { it.copy(signUpBirthDate = date, signUpBirthDateError = error) }
        }
    }
    fun onSignUpIsLunarChange(isLunar: Boolean) { _uiState.update { it.copy(signUpIsLunar = isLunar) } }

    fun onSignUpEmailChange(email: String) {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        val error = if (email.isNotEmpty() && !email.matches(emailRegex)) "이메일 형식이 올바르지 않아요." else null
        _uiState.update { it.copy(signUpEmail = email, signUpEmailError = error) }
    }

    fun onSignUpPasswordChange(password: String) {
        val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+=<>?]).{8,}$")
        val error = if (password.isNotEmpty() && !password.matches(passwordRegex)) "영문, 숫자, 특수기호를 포함해 8자리 이상이어야 해요." else null
        _uiState.update { it.copy(signUpPassword = password, signUpPasswordError = error) }

        if (_uiState.value.signUpConfirmPassword.isNotEmpty()) {
            onSignUpConfirmPasswordChange(_uiState.value.signUpConfirmPassword)
        }
    }

    fun onSignUpConfirmPasswordChange(password: String) {
        val error = if (password.isNotEmpty() && password != _uiState.value.signUpPassword) "비밀번호가 일치하지 않아요." else null
        _uiState.update { it.copy(signUpConfirmPassword = password, signUpConfirmPasswordError = error) }
    }

    fun onProfileImageChanged(uri: Uri?) {
        _uiState.update { it.copy(profileImageUri = uri) }
    }

    fun onFamilyMemberSelectionChange(member: String, isSelected: Boolean) {
        val updatedSelections = _uiState.value.familyMemberSelections.toMutableMap().apply { this[member] = isSelected }
        _uiState.update { it.copy(familyMemberSelections = updatedSelections) }
    }
    fun onOtherFamilyMemberCheckedChange(isChecked: Boolean) {
        val newText = if (!isChecked) "" else _uiState.value.otherFamilyMemberText
        _uiState.update { it.copy(otherFamilyMemberChecked = isChecked, otherFamilyMemberText = newText) }
    }
    fun onOtherFamilyMemberTextChange(text: String) { if (text.length <= 10) _uiState.update { it.copy(otherFamilyMemberText = text) } }
    fun onFindPwNameChange(name: String) { _uiState.update { it.copy(findPwName = name) } }
    fun onFindPwEmailChange(email: String) { _uiState.update { it.copy(findPwEmail = email) } }
    fun onFindIdNameChange(name: String) { _uiState.update { it.copy(findIdName = name) } }
    fun onFindIdEmailChange(email: String) { _uiState.update { it.copy(findIdEmail = email) } }

    // --- 로직 실행 함수들 ---

//    fun login() {
//        _uiState.update { it.copy(isLoading = true, isLoginSuccess = false, loginError = null) }
//        viewModelScope.launch {
//            val result = repository.login(email = _uiState.value.loginEmail, password = _uiState.value.loginPassword)
//            _uiState.update {
//                when(result) {
//                    is AuthResult.Success -> it.copy(isLoading = false, isLoginSuccess = true)
//                    is AuthResult.Failure -> it.copy(isLoading = false, loginError = result.message)
//                }
//            }
//
//        }
//    }


    /** Google 로그인 */
    fun loginWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, loginError = null) }
        viewModelScope.launch {
            try {
                val res = repository.loginWithGoogle(idToken)
                FirebaseAuth.getInstance()
                    .signInWithCustomToken(res.custom_token)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful) {
                            _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, loginError = task.exception?.message) }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loginError = e.message) }
            }
        }
    }

    /** Naver 로그인 */
    fun loginWithNaver(accessToken: String) {
        _uiState.update { it.copy(isLoading = true, loginError = null) }
        viewModelScope.launch {
            try {
                val res = repository.loginWithNaver(accessToken)
                FirebaseAuth.getInstance()
                    .signInWithCustomToken(res.custom_token)
                    .addOnCompleteListener { task ->
                        if(task.isSuccessful) {
                            _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                        } else {
                            _uiState.update { it.copy(isLoading = false, loginError = task.exception?.message) }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, loginError = e.message) }
            }
        }
    }




//    fun signUp() {
//        _uiState.update { it.copy(isLoading = true, signUpResult = null) }
//        viewModelScope.launch {
//            val result = repository.signUp(
//                name = _uiState.value.signUpName,
//                email = _uiState.value.signUpEmail,
//                password = _uiState.value.signUpPassword
//            )
//            if (result is AuthResult.Success) {
//                repository.login(_uiState.value.signUpEmail, _uiState.value.signUpPassword)
//                _uiState.update { it.copy(isLoading = false, signUpResult = result, isSignUpAndLoginSuccess = true) }
//            } else {
//                _uiState.update { it.copy(isLoading = false, signUpResult = result) }
//            }
//        }
//    }
//
//    fun resetPassword() {
//        _uiState.update { it.copy(isLoading = true, findAccountResult = null) }
//        viewModelScope.launch {
//            val result = repository.resetPassword(_uiState.value.findPwEmail)
//            _uiState.update { it.copy(isLoading = false, findAccountResult = result) }
//        }
//    }
//
//    fun findId() {
//        _uiState.update { it.copy(isLoading = true, findAccountResult = null) }
//        viewModelScope.launch {
//            val result = repository.findId(
//                name = _uiState.value.findIdName,
//                email = _uiState.value.findIdEmail
//            )
//            _uiState.update { it.copy(isLoading = false, findAccountResult = result) }
//        }
//    }

    // --- 상태 초기화 함수들 ---
    fun clearLoginError() { _uiState.update { it.copy(loginError = null, isLoginSuccess = false) } }
    fun clearSignUpResult() { _uiState.update { it.copy(signUpResult = null, isSignUpAndLoginSuccess = false) } }
    fun clearFindAccountResult() { _uiState.update { it.copy(findAccountResult = null) } }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginSuccess: Boolean = false,
    val loginError: String? = null,
    val signUpName: String = "",
    val signUpBirthDate: String = "",
    val signUpIsLunar: Boolean = false,
    val signUpEmail: String = "",
    val signUpPassword: String = "",
    val signUpConfirmPassword: String = "",
    val signUpEmailError: String? = null,
    val signUpPasswordError: String? = null,
    val signUpConfirmPasswordError: String? = null,
    val signUpBirthDateError: String? = null,
    val profileImageUri: Uri? = null,
    val familyMemberSelections: Map<String, Boolean> = emptyMap(),
    val otherFamilyMemberChecked: Boolean = false,
    val otherFamilyMemberText: String = "",
    val signUpResult: AuthResult? = null,
    val isSignUpAndLoginSuccess: Boolean = false,
    val findIdName: String = "",
    val findIdEmail: String = "",
    val findPwName: String = "",
    val findPwEmail: String = "",
    val findAccountResult: AuthResult? = null,
)

sealed class AuthResult {
    object Success : AuthResult()
    data class Failure(val message: String) : AuthResult()
}