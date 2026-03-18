package ui.urllist

import androidx.compose.runtime.Immutable
import model.ShortenedUrl

data class UrlListScreenUiState(
    val callbacks: Callbacks,
    val loadingState: LoadingState,
) {
    @Immutable
    interface Callbacks {
        fun onSearchQueryChange(query: String)
        fun onSearch()
        fun onPrevPage()
        fun onNextPage()
        fun onDeleteRequest(url: ShortenedUrl)
        fun onDeleteCancel()
        fun onDeleteConfirm()
        fun onLogout()
    }

    sealed interface LoadingState {
        data object Loading : LoadingState
        data class Error(val message: String) : LoadingState
        data class Loaded(
            val urls: List<ShortenedUrl>,
            val total: Long,
            val offset: Int,
            val pageSize: Int,
            val query: String,
            val deleteConfirm: ShortenedUrl?,
            val isDeleteInProgress: Boolean,
        ) : LoadingState
    }
}
