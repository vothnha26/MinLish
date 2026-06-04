package com.edu.minlish.features.library.presentation.viewmodel

sealed class ExportUiState {
    object Idle : ExportUiState()
    object FetchingData : ExportUiState()
    object Exporting : ExportUiState()
    data class Success(val fileName: String) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}
