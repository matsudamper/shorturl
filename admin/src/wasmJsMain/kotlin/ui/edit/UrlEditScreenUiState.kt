package ui.edit

import androidx.compose.runtime.Immutable

data class UrlEditScreenUiState(
    val callbacks: Callbacks,
    val loadingState: LoadingState,
) {
    @Immutable
    interface Callbacks {
        fun onClickCopy()
        fun save()
        fun updateUrl(text: String)
    }

    sealed interface LoadingState {
        data class Error(val message: String) : LoadingState
        object Loading : LoadingState
        data class Loaded(
            val url: String,
            val slug: String,
            val isSaveButtonEnabled: Boolean,
            val isSaving: Boolean,
        ) : LoadingState
    }
}

