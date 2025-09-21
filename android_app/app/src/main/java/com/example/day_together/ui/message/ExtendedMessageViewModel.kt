package com.example.day_together.ui.message

import com.example.day_together.AuthManager
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.data.repository.QuestionRepository
import kotlinx.coroutines.flow.update


class ExtendedMessageViewModel(
    repository: AppRepository,
    private val questionRepository: QuestionRepository
) : MessageViewModel(repository, QuestionRepository()) {

    init {
        fetchTodayQuestion()
    }

    private fun fetchTodayQuestion() {
        val uid = AuthManager.getCurrentUserId() ?: return
        questionRepository.loadTodayQuestion(uid) { question ->
            question?.let { q ->
                val currentMessages = _uiState.value.messages.toMutableList()
                currentMessages.add(
                    ChatMessage(content = q, sender = "system")
                )
                _uiState.update { state->
                    state.copy(messages = currentMessages) }
            }
        }
    }
}