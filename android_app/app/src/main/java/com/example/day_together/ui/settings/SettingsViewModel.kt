package com.example.day_together.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.day_together.data.repository.FakeRepository
import com.example.day_together.data.repository.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 1. UI 상태를 나타내는 데이터 클래스
data class SettingsUiState(
    val questionFrequency: String = "",
    val questionTime: String = "",
    val notificationEnabled: Boolean = true,
    val vibrationEnabled: Boolean = false,
    val isLoading: Boolean = true
)

// 2. 화면 이동 등 일회성 이벤트 처리 위한 Sealed Class
sealed class SettingsEvent {
    data object NavigateToLogin : SettingsEvent()
}

// 3. ViewModel
class SettingsViewModel(
    private val repository: FakeRepository = FakeRepository() // todo : 실제 레포지토리로 변경
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // ViewModel 생성시 Repository로부터 설정 값을 구독
        repository.getSettingsFlow()
            .onEach { settings ->
                _uiState.update {
                    it.copy(
                        questionFrequency = settings.questionFrequency,
                        questionTime = settings.questionTime,
                        notificationEnabled = settings.notificationEnabled,
                        vibrationEnabled = settings.vibrationEnabled,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    // 4. 이벤트 핸들러 함수
    fun onFrequencyChange(frequency: String) {
        _uiState.update { it.copy(questionFrequency = frequency) }
        saveCurrentSettings()
    }

    fun onTimeChange(time: String) {
        _uiState.update { it.copy(questionTime = time) }
        saveCurrentSettings()
    }

    fun onNotificationToggle(enabled: Boolean) {
        _uiState.update { it.copy(notificationEnabled = enabled) }
        saveCurrentSettings()
    }

    fun onVibrationToggle(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        saveCurrentSettings()
    }

    fun onLogoutClicked() {
        viewModelScope.launch {
            repository.logout() // Repository에 로그아웃 요청
            _eventFlow.emit(SettingsEvent.NavigateToLogin) // UI에 로그인 화면으로 이동하라는 이벤트 전달함
        }
    }

    // 현재 UI 상태 Repository에 저장하는 private 함수
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
