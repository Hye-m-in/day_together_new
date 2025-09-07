package com.example.day_together.ui.auth


import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * 데이터를 한 곳(ViewModel)에서만 통제함으로써 코드가 꼬이는 것을 막음
 * UI 화면이 마음대로 데이터를 바꾸면 앱이 복잡해질수록 어디서 버그가 생기는지 찾기 매우 어려워짐
 */

/**
 * 로그인, 회원가입, 계정 찾기 등 인증 관련 화면들의
 * UI 상태와 비즈니스 로직을 모두 관리하는 ViewModel
 */
class AuthViewModel : ViewModel() {

    private val repository: AppRepository = AppRepository


    /**
     * 인증 화면의 모든 UI 상태를 관리하는 StateFlow. View는 이 State를 구독(실시간 상태 감지)하여 UI에 반영
     * StateFlow : 실시간으로 업데이트되는 '상태 게시판'
     * 게시판(StateFlow)에는 항상 최신 정보(State) 하나만 존재
     * 게시판 내용이 변경될 경우, UI화면은 즉시 새로운 내용 확인하고 자신의 화면 업데이트 함
     */

    // private : ViewModel 내부에서만 수정 가능한 게시판
    private val _uiState = MutableStateFlow(AuthUiState())
    // public : ViewModel 외부에서는 오직 읽기만 가능한 공개용 게시판
    val uiState = _uiState.asStateFlow()


    /**
     * 이벤트 핸들러 함수들
     * 이벤트 핸들러
     * -> 사용자 행동을 처리하는 담당자
     * -> UI에서 발생하는 이벤트 처리 및 'UI State' 업데이트
     *
     * Event : 사용자가 앱에서 하는 모든 행동(버튼 클릭, 글자 입력, 화면 스크롤 등)
     * Handler : 행동 발생 시, 처리를 위해 실행되는 함수
     *
     * StateFlow ~ 이벤트 핸들러 흐름
     * 이벤트 발생 -> 핸들러 호출 -> ViewModel의 상태 업데이트 -> 화면 자동 변경
     */

    fun onLoginEmailChange(email: String) { _uiState.update { it.copy(loginEmail = email, loginError = null) } }
    fun onLoginPasswordChange(password: String) { _uiState.update { it.copy(loginPassword = password, loginError = null) } }

    fun onSignUpNameChange(name: String) { _uiState.update { it.copy(signUpName = name) } }
    fun onSignUpBirthDateChange(date: String) {
        // 생년월일은 8자리까지 입력 가능
        if (date.length <= 8) {
            val error = if (date.isNotEmpty() && date.length != 8) "8자리 형식(YYYYMMDD)으로 입력해주세요." else null
            _uiState.update { it.copy(signUpBirthDate = date, signUpBirthDateError = error) }
        }
    }
    fun onSignUpIsLunarChange(isLunar: Boolean) { _uiState.update { it.copy(signUpIsLunar = isLunar) } }

    fun onSignUpEmailChange(email: String) {
        // 이메일 형식 실시간 검증
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        val error = if (email.isNotEmpty() && !email.matches(emailRegex)) "이메일 형식이 올바르지 않아요." else null
        _uiState.update { it.copy(signUpEmail = email, signUpEmailError = error) }
    }

    fun onSignUpPasswordChange(password: String) {
        // 1. 비밀번호 설정 검사(영문, 숫자, 특수기호 포함 8자리 이상)
        val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#\$%^&*()_+=<>?]).{8,}$")
        val error = if (password.isNotEmpty() && !password.matches(passwordRegex)) "영문, 숫자, 특수기호를 포함해 8자리 이상이어야 해요." else null

        // 2. 상태 업데이트(입력된 비밀번호 및 에러 메세지 기록)
        _uiState.update { it.copy(signUpPassword = password, signUpPasswordError = error) }

        // 3. '비밀번호 확인' 칸에 값이 존재하는 경우, 그 값이 변경된 비밀번호와 일치하는지 검사하도록 다른 핸들러 호출
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
        // '기타' 체크 해제 시, 입력 텍스트도 초기화
        val newText = if (!isChecked) "" else _uiState.value.otherFamilyMemberText
        _uiState.update { it.copy(otherFamilyMemberChecked = isChecked, otherFamilyMemberText = newText) }
    }
    fun onOtherFamilyMemberTextChange(text: String) { if (text.length <= 10) _uiState.update { it.copy(otherFamilyMemberText = text) } }
    fun onFindPwNameChange(name: String) { _uiState.update { it.copy(findPwName = name) } }
    fun onFindPwEmailChange(email: String) { _uiState.update { it.copy(findPwEmail = email) } }
    fun onFindIdNameChange(name: String) { _uiState.update { it.copy(findIdName = name) } }
    fun onFindIdEmailChange(email: String) { _uiState.update { it.copy(findIdEmail = email) } }


    /**
     * 실제 비즈니스 로직(로그인, 회원가입 등) 실행하는 함수
     * -> 사용자 액션 처리(로그인이나 회원가입 버튼 눌렀을 때)
     *
     * 역할1. 비동기 처리 : 서버 통신 등 시간 걸리는 작업 'viewModelScope.launch'안에서 처리함으로써 앱이 계속 동작하도록 함
     * 역할2. 로딩 상태 관리 : 작업 시작 전 isLoading -> true, 작업 완료 후 false 처리로 ui에 로딩 스피너 표시
     * 역할3. 결과 처리 : repository로부터 받은 작업 결과(AuthResult) 성공/실패 여부에 따라 _uiState 다르게 업데이트
     *
     * 로딩 스피너(Loading Spinner) : 앱에 데이터 불러오거나 작업 처리 중일 때 사용자에게 알려주는 로딩 아이콘
     *
     */

    // 로그인 로직 실행
    fun login() {
        // 로그인 로딩 상태 시작
        _uiState.update { it.copy(isLoading = true, isLoginSuccess = false, loginError = null) }
        // 실제 작업 수행
        viewModelScope.launch {
            // Repository에게 이메일과 비밀번호 주고 로그인 요청 -> 결과 기다림
            val result = repository.login(email = _uiState.value.loginEmail, password = _uiState.value.loginPassword)
            // 결과에 따라 상태 업데이트
            _uiState.update {
                when(result) {
                    // 로딩 끝 : 로그인 성공 시 true, 실패 시 에러 메시지 기록
                    is AuthResult.Success -> it.copy(isLoading = false, isLoginSuccess = true)
                    is AuthResult.Failure -> it.copy(isLoading = false, loginError = result.message)
                }
            }
        }
    }

    /**
     * 구글 ID 토큰으로 Firebase에 로그인하는 로직 실행
     */
    fun signInWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, isLoginSuccess = false, loginError = null) }
        viewModelScope.launch {
            val result = repository.signInWithGoogle(idToken)
            _uiState.update {
                when (result) {
                    is AuthResult.Success -> it.copy(isLoading = false, isLoginSuccess = true)
                    is AuthResult.Failure -> it.copy(isLoading = false, loginError = result.message)
                }
            }
        }
    }

    // 회원가입 로직 실행
    fun signUp() {
        _uiState.update { it.copy(isLoading = true, signUpResult = null) }
        viewModelScope.launch {
            val result = repository.signUp(
                name = _uiState.value.signUpName,
                email = _uiState.value.signUpEmail,
                password = _uiState.value.signUpPassword
            )
            // 회원가입 성공 시, 자동 로그인 수행
            if (result is AuthResult.Success) {
                repository.login(_uiState.value.signUpEmail, _uiState.value.signUpPassword)
                _uiState.update { it.copy(isLoading = false, signUpResult = result, isSignUpAndLoginSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, signUpResult = result) }
            }
        }
    }

    // 비밀번호 재설정 로직
    fun resetPassword() {
        _uiState.update { it.copy(isLoading = true, findAccountResult = null) }
        viewModelScope.launch {
            val result = repository.resetPassword(_uiState.value.findPwEmail)
            _uiState.update { it.copy(isLoading = false, findAccountResult = result) }
        }
    }

    // 아이디 찾기 로직
    fun findId() {
        _uiState.update { it.copy(isLoading = true, findAccountResult = null) }
        viewModelScope.launch {
            val result = repository.findId(
                name = _uiState.value.findIdName,
                email = _uiState.value.findIdEmail
            )
            _uiState.update { it.copy(isLoading = false, findAccountResult = result) }
        }
    }

    /**
     * 상태 초기화 함수 : 특정 액션이 끝난 후, 관련 상태를 초기화하여 UI를 정리
     * -> 일회성 상태 제거 -> 에러메세지, 작업 성공 여부 등 더 이상 필요 없는 상태값 초기화
     * -> ui 호출 : ㅘ면 이동, 메세지 표시 등의 동작 직후에 함수 호출 후 정리
     */

    /**
     * 로그인 실패 시 loginError에 '비밀번호가 틀렸습니다.' 에러메세지 저장
     * -> 사용자가 다시 입력하게 되면, 위 에러메시지는 사라짐
     *
     */
    fun clearLoginError() { _uiState.update { it.copy(loginError = null, isLoginSuccess = false) } }


    fun clearSignUpResult() { _uiState.update { it.copy(signUpResult = null, isSignUpAndLoginSuccess = false) } }
    fun clearFindAccountResult() { _uiState.update { it.copy(findAccountResult = null) } }

}

/**
 * AuthUiState 클래스 : 비동기 작업 포함한 앱의 상태 관리하는 도구
 *
 * AuthUiState : UI 화면 설계도, 인증 화면에 필요한 모든 정보의 설계도
 * -> 화면에 필요한 모든 데이터(이메일 입력 값, 비밀번호 입력 값, 로딩 중 여부, 에러메시지 등)를 'data class'에 모아둠
 */
data class AuthUiState(
    // 공통 상태
    val isLoading: Boolean = false,

    // 로그인 화면 상태
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginSuccess: Boolean = false,
    val loginError: String? = null,

    // 회원가입 화면 상태
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

    // 계정 찾기 화면 상태
    val findIdName: String = "",
    val findIdEmail: String = "",
    val findPwName: String = "",
    val findPwEmail: String = "",
    val findAccountResult: AuthResult? = null
)