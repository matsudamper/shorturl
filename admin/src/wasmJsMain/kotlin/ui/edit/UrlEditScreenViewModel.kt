package ui.edit

import api.ApiClient
import api.isUnauthorizedApiError
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import model.ShortenedUrl

class UrlEditScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val urlId: String,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val eventHandler = Channel<(Event) -> Unit>(Channel.UNLIMITED)

    private val callbacks = object : UrlEditScreenUiState.Callbacks {
        override fun onClickCopy() {
            val slug = viewModelStateFlow.value.url?.slug ?: return
            eventHandler.trySend {
                it.sendToClipboard(window.location.host + "/" + slug)
            }
        }

        override fun save() {
            coroutineScope.launch {
                val currentUrl = viewModelStateFlow.value.url ?: return@launch
                val newUrl = viewModelStateFlow.value.editNewUrl ?: return@launch
                viewModelStateFlow.update {
                    it.copy(
                        isSaving = true
                    )
                }
                try {
                    ApiClient.updateUrl(currentUrl.id, newUrl).fold(
                        onSuccess = {
                            eventHandler.trySend {
                                it.showSnackBar("保存しました。")
                            }
                            fetchNewData()
                        },
                        onFailure = { e ->
                            if (e.isUnauthorizedApiError()) {
                                eventHandler.trySend {
                                    it.onUnauthorized()
                                }
                            } else {
                                eventHandler.trySend {
                                    it.showSnackBar(e.message ?: "保存エラー")
                                }
                            }
                        },
                    )
                } finally {
                    viewModelStateFlow.update {
                        it.copy(
                            isSaving = false,
                        )
                    }
                }
            }
        }

        override fun updateUrl(text: String) {
            viewModelStateFlow.update {
                it.copy(
                    editNewUrl = text,
                )
            }
        }
    }

    val uiState: StateFlow<UrlEditScreenUiState> = MutableStateFlow(
        UrlEditScreenUiState(
            callbacks = callbacks,
            loadingState = UrlEditScreenUiState.LoadingState.Loading,
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = run loadingState@{
                            if (viewModelState.errorMessage != null) {
                                return@loadingState UrlEditScreenUiState.LoadingState.Error(viewModelState.errorMessage)
                            }
                            if (viewModelState.url == null) {
                                return@loadingState UrlEditScreenUiState.LoadingState.Loading
                            }
                            UrlEditScreenUiState.LoadingState.Loaded(
                                url = viewModelState.url.originalUrl,
                                slug = viewModelState.url.slug,
                                isSaveButtonEnabled = viewModelState.url.originalUrl != viewModelState.editNewUrl,
                                isSaving = viewModelState.isSaving,
                            )
                        }
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        fetchNewData()
    }

    private fun fetchNewData() {
        coroutineScope.launch {
            ApiClient.getUrl(urlId).fold(
                onSuccess = { url ->
                    viewModelStateFlow.update {
                        it.copy(
                            url = url,
                            editNewUrl = url.originalUrl,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { e ->
                    if (e.isUnauthorizedApiError()) {
                        eventHandler.trySend {
                            it.onUnauthorized()
                        }
                    } else {
                        viewModelStateFlow.update {
                            it.copy(
                                errorMessage = e.message ?: "読込エラー",
                            )
                        }
                    }
                },
            )
        }
    }

    interface Event {
        fun sendToClipboard(value: String)
        fun onUnauthorized()
        fun showSnackBar(message: String)
    }

    data class ViewModelState(
        val url: ShortenedUrl? = null,
        val editNewUrl: String? = null,
        val errorMessage: String? = null,
        val isSaving: Boolean = false,
    )
}
