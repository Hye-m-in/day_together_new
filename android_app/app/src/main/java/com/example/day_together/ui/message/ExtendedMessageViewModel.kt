package com.example.day_together.ui.message

import androidx.lifecycle.viewModelScope
import com.example.day_together.data.repository.AppRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExtendedMessageViewModel(
    private val repository: AppRepository
) : MessageViewModel(repository) {

    init {
        fetchTodayQuestion()
    }

    private fun fetchTodayQuestion() {
        // AppRepository의 suspend 함수를 사용하기 위해 코루틴 실행
        viewModelScope.launch {
            val question = repository.getTodaysQuestion()
            question?.let { q ->
                // _uiState는 MessageViewModel에서 protected로 선언되어 있어 접근 가능
                val currentMessages = _uiState.value.messages.toMutableList()

                // ChatMessage는 String을 받으므로 Question 객체(q) 대신 q.text를 사용하도록 수정
                currentMessages.add(
                    ChatMessage(content = q.text, sender = "system")
                )
                _uiState.update { state ->
                    state.copy(messages = currentMessages)
                }
            }
        }
    }
}