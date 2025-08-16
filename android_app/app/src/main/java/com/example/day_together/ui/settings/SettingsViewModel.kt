package com.example.day_together.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.AuthManager
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.UserSettings // 예시 데이터 클래스
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 설정 화면의 UI 상태(State)를 나타내는 데이터 클래스
 * UI가 화면에 어떻게 보여야 하는지에 대한 모든 정보를 담음
 */
data class SettingsUiState(
    val questionFrequency: String = "",
    val questionTime: String = "",
    val notificationEnabled: Boolean = true,
    val vibrationEnabled: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * 설정 화면에서 발생하는 일회성 이벤트 정의
 */
sealed class SettingsEvent {
    data object NavigateToLogin : SettingsEvent()
}

/**
 * 설정 화면의 로직을 처리하는 ViewModel
 * UI(Screen)로부터 이벤트를 받아 로직을 처리하고, UI에 표시될 상태(State)를 관리
 */
class SettingsViewModel(
    // AppRepository는 데이터 통신을 담당하는 클래스라고 가정
    private val repository: AppRepository = AppRepository()
) : ViewModel() {

    // UI에 노출될 화면 상태(StateFlow)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // 화면 이동과 같은 일회성 이벤트를 전달하기 위한 SharedFlow
    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // ViewModel이 생성될 때 Repository로부터 초기 설정 값을 불러옴
        repository.getSettingsFlow()
            .onEach { settings ->
                // 설정 값을 성공적으로 불러오면, UI 상태 업데이트
                _uiState.update {
                    it.copy(
                        questionFrequency = settings.questionFrequency,
                        questionTime = settings.questionTime,
                        notificationEnabled = settings.notificationEnabled,
                        vibrationEnabled = settings.vibrationEnabled,
                        isLoading = false // 로딩 완료
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 질문 빈도 변경 시 호출되는 이벤트 핸들러
     */
    fun onFrequencyChange(frequency: String) {
        _uiState.update { it.copy(questionFrequency = frequency) }
        saveCurrentSettings()
    }

    /**
     * 질문 시간대 변경 시 호출되는 이벤트 핸들러
     */
    fun onTimeChange(time: String) {
        _uiState.update { it.copy(questionTime = time) }
        saveCurrentSettings()
    }

    /**
     * 알림 설정 토글 시 호출되는 이벤트 핸들러
     */
    fun onNotificationToggle(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
        saveCurrentSettings()
    }

    /**
     * 진동 설정 토글 시 호출되는 이벤트 핸들러
     */
    fun onVibrationToggle(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        saveCurrentSettings()
    }

    /**
     * 로그아웃 버튼 클릭 시 호출되는 이벤트 핸들러
     */
    fun onLogoutClicked() {
        viewModelScope.launch {
            repository.logout() // Repository에 로그아웃 요청
            _eventFlow.emit(SettingsEvent.NavigateToLogin) // UI에 로그인 화면으로 이동하라는 이벤트 전달
        }
    }

    /**
     * 현재 UI 상태를 Repository를 통해 저장하는 private 함수
     */
    private fun saveCurrentSettings() {
        viewModelScope.launch {
            val currentSettings = UserSettings(
                questionFrequency = _uiState.value.questionFrequency,
                questionTime = _uiState.value.questionTime,
                notificationEnabled = _uiState.value.notificationEnabled,
                vibrationEnabled = _uiState.value.vibrationEnabled
            )
            repository.saveSettings(currentSettings)
        }
    }
}