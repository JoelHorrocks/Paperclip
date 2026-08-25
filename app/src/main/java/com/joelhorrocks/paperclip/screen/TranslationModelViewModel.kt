package com.joelhorrocks.paperclip.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelhorrocks.paperclip.history.HistoryEntry
import com.joelhorrocks.paperclip.history.HistoryRepository
import com.joelhorrocks.paperclip.ml.TranslationModel
import com.joelhorrocks.paperclip.ml.TranslationModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TranslationModelLoadingState {
    LOADING, SUCCESS, ERROR
}

@HiltViewModel
class TranslationModelViewModel @Inject constructor(
    private val translationModelRepository: TranslationModelRepository
) : ViewModel() {

    data class TranslationModelUiState(
        val translationModelLoadingState: TranslationModelLoadingState = TranslationModelLoadingState.LOADING,
        val modelList: List<TranslationModel> = listOf(),
    )

    private val _uiState = MutableStateFlow(TranslationModelUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                translationModelLoadingState = TranslationModelLoadingState.LOADING
            )
        }
        viewModelScope.launch {
            // TODO: errorhandling, proper loading state
            translationModelRepository.fetchModels()
            translationModelRepository.models.collect { modelList ->
                _uiState.update {
                    it.copy(
                        translationModelLoadingState = TranslationModelLoadingState.SUCCESS,
                        modelList = modelList,
                    )
                }
            }
        }
    }
}