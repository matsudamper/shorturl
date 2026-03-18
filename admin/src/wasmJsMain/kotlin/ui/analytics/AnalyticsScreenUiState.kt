package ui.analytics

import androidx.compose.runtime.Immutable
import model.AnalyticsSummary

data class AnalyticsScreenUiState(
    val callbacks: Callbacks,
    val loadingState: LoadingState,
) {
    @Immutable
    interface Callbacks

    sealed interface LoadingState {
        data object Loading : LoadingState
        data class Error(val message: String) : LoadingState
        data class Loaded(
            val slug: String,
            val summary: AnalyticsSummary,
        ) : LoadingState
    }
}
