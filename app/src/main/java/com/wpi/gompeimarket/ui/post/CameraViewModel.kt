package com.wpi.gompeimarket.ui.post

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wpi.gompeimarket.data.repository.AIRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIUiState(
    val isAnalyzing: Boolean = false,
    val category: String = "",
    val description: String = "",
    val relatedSearches: List<String> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _aiState = MutableStateFlow(AIUiState())
    val aiState: StateFlow<AIUiState> = _aiState

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _aiState.value = AIUiState(isAnalyzing = true)
            val result = aiRepository.analyzeImage(bitmap)
            if (result.isSuccess) {
                val r = result.getOrThrow()
                _aiState.value = AIUiState(
                    category = r.category,
                    description = r.description,
                    relatedSearches = r.relatedSearches
                )
            } else {
                _aiState.value = AIUiState(errorMessage = result.exceptionOrNull()?.message ?: "AI analysis failed")
            }
        }
    }

    fun resetAIState() { _aiState.value = AIUiState() }
}
