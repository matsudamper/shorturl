package ui.analytics

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
import model.AnalyticsSummary

internal class AnalyticsScreenViewModel(
    private val coroutineScope: CoroutineScope,
    private val urlId: String,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val eventHandler = Channel<(Event) -> Unit>(Channel.UNLIMITED)

    private val callbacks = object : AnalyticsScreenUiState.Callbacks {}

    val uiState: StateFlow<AnalyticsScreenUiState> = MutableStateFlow(
        AnalyticsScreenUiState(
            callbacks = callbacks,
            loadingState = AnalyticsScreenUiState.LoadingState.Loading,
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = run loadingState@{
                            if (viewModelState.errorMessage != null) {
                                return@loadingState AnalyticsScreenUiState.LoadingState.Error(viewModelState.errorMessage)
                            }
                            val slug = viewModelState.slug
                            val summary = viewModelState.summary
                            if (slug == null || summary == null) {
                                return@loadingState AnalyticsScreenUiState.LoadingState.Loading
                            }
                            AnalyticsScreenUiState.LoadingState.Loaded(
                                slug = slug,
                                summary = summary,
                            )
                        }
                    )
                }
            }
        }
    }.asStateFlow()

    init {
        coroutineScope.launch {
            var unauthorized = false

            ApiClient.getUrl(urlId).fold(
                onSuccess = { url ->
                    viewModelStateFlow.update { it.copy(slug = url.slug) }
                },
                onFailure = { e ->
                    if (e.isUnauthorizedApiError()) {
                        unauthorized = true
                        eventHandler.trySend { it.onUnauthorized() }
                    } else {
                        viewModelStateFlow.update { it.copy(errorMessage = e.message ?: "URL取得エラー") }
                    }
                },
            )

            if (!unauthorized && viewModelStateFlow.value.errorMessage == null) {
                ApiClient.getAnalytics(urlId).fold(
                    onSuccess = { summary ->
                        viewModelStateFlow.update { it.copy(summary = summary) }
                    },
                    onFailure = { e ->
                        if (e.isUnauthorizedApiError()) {
                            eventHandler.trySend { it.onUnauthorized() }
                        } else {
                            viewModelStateFlow.update { it.copy(errorMessage = e.message ?: "エラー") }
                        }
                    },
                )
            }
        }
    }

    interface Event {
        fun onUnauthorized()
    }

    data class ViewModelState(
        val slug: String? = null,
        val summary: AnalyticsSummary? = null,
        val errorMessage: String? = null,
    )
}
