package ui.urlcreate

import androidx.compose.runtime.Immutable

data class UrlCreateScreenUiState(
    val callbacks: Callbacks,
    val loadingState: LoadingState,
) {
    @Immutable
    interface Callbacks {
        fun onOriginalUrlChange(value: String)
        fun onModeChange(isAutoMode: Boolean)
        fun onSlugTypeChange(type: String)
        fun onSlugLengthChange(length: Int)
        fun onManualSlugChange(value: String)
        fun generateSlug()
        fun create()
    }

    sealed interface LoadingState {
        data object Loading : LoadingState
        data class Error(val message: String) : LoadingState
        data class Loaded(
            val isAutoMode: Boolean,
            val originalUrl: String,
            val selectedType: String,
            val slugLength: Int,
            val previewSlug: String,
            val manualSlug: String,
            val isGenerating: Boolean,
            val isSaving: Boolean,
        ) : LoadingState
    }
}
