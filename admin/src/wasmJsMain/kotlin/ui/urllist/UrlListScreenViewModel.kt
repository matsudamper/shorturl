package ui.urllist

import api.ApiClient
import api.isUnauthorizedApiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.ShortenedUrl

private const val PAGE_SIZE = 20

internal class UrlListScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val eventHandler = Channel<(Event) -> Unit>(Channel.UNLIMITED)

    private val callbacks = object : UrlListScreenUiState.Callbacks {
        override fun onSearchQueryChange(query: String) {
            viewModelStateFlow.update { it.copy(query = query) }
        }

        override fun onSearch() {
            viewModelStateFlow.update { it.copy(offset = 0) }
            load()
        }

        override fun onPrevPage() {
            viewModelStateFlow.update { it.copy(offset = (it.offset - PAGE_SIZE).coerceAtLeast(0)) }
            load()
        }

        override fun onNextPage() {
            viewModelStateFlow.update { it.copy(offset = it.offset + PAGE_SIZE) }
            load()
        }

        override fun onDeleteRequest(url: ShortenedUrl) {
            viewModelStateFlow.update { it.copy(deleteConfirm = url) }
        }

        override fun onDeleteCancel() {
            if (viewModelStateFlow.value.isDeleteInProgress) return
            viewModelStateFlow.update { it.copy(deleteConfirm = null) }
        }

        override fun onDeleteConfirm() {
            val target = viewModelStateFlow.value.deleteConfirm ?: return
            coroutineScope.launch {
                viewModelStateFlow.update { it.copy(isDeleteInProgress = true) }
                ApiClient.deleteUrl(target.id).fold(
                    onSuccess = {
                        viewModelStateFlow.update { it.copy(deleteConfirm = null, isDeleteInProgress = false) }
                        load()
                    },
                    onFailure = { e ->
                        viewModelStateFlow.update { it.copy(isDeleteInProgress = false) }
                        if (e.isUnauthorizedApiError()) {
                            eventHandler.trySend { it.onUnauthorized() }
                        } else {
                            eventHandler.trySend { it.showSnackBar(e.message ?: "削除エラー") }
                        }
                    },
                )
            }
        }

        override fun onLogout() {
            coroutineScope.launch {
                ApiClient.logout()
                eventHandler.trySend { it.onLoggedOut() }
            }
        }
    }

    val uiState: StateFlow<UrlListScreenUiState> = MutableStateFlow(
        UrlListScreenUiState(
            callbacks = callbacks,
            loadingState = UrlListScreenUiState.LoadingState.Loading,
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = run loadingState@{
                            if (viewModelState.errorMessage != null && viewModelState.urls.isEmpty()) {
                                return@loadingState UrlListScreenUiState.LoadingState.Error(viewModelState.errorMessage)
                            }
                            if (viewModelState.isInitialLoading) {
                                return@loadingState UrlListScreenUiState.LoadingState.Loading
                            }
                            UrlListScreenUiState.LoadingState.Loaded(
                                urls = viewModelState.urls,
                                total = viewModelState.total,
                                offset = viewModelState.offset,
                                pageSize = PAGE_SIZE,
                                query = viewModelState.query,
                                deleteConfirm = viewModelState.deleteConfirm,
                                isDeleteInProgress = viewModelState.isDeleteInProgress,
                            )
                        }
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        load()
    }

    private fun load() {
        coroutineScope.launch {
            val currentState = viewModelStateFlow.value
            viewModelStateFlow.update { it.copy(isReloading = true, errorMessage = null) }
            ApiClient.listUrls(currentState.offset, PAGE_SIZE, currentState.query.ifBlank { null }).fold(
                onSuccess = { paged ->
                    viewModelStateFlow.update {
                        it.copy(
                            urls = paged.items,
                            total = paged.total,
                            isInitialLoading = false,
                            isReloading = false,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { e ->
                    if (e.isUnauthorizedApiError()) {
                        eventHandler.trySend { it.onUnauthorized() }
                    } else {
                        viewModelStateFlow.update {
                            it.copy(
                                isInitialLoading = false,
                                isReloading = false,
                                errorMessage = e.message ?: "エラー",
                            )
                        }
                    }
                },
            )
        }
    }

    interface Event {
        fun onUnauthorized()
        fun onLoggedOut()
        fun showSnackBar(message: String)
    }

    data class ViewModelState(
        val urls: List<ShortenedUrl> = emptyList(),
        val total: Long = 0L,
        val offset: Int = 0,
        val query: String = "",
        val isInitialLoading: Boolean = true,
        val isReloading: Boolean = false,
        val errorMessage: String? = null,
        val deleteConfirm: ShortenedUrl? = null,
        val isDeleteInProgress: Boolean = false,
    )
}
