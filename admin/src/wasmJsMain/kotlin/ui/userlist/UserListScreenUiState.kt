package ui.userlist

import androidx.compose.runtime.Immutable
import model.UserSummary

data class UserListScreenUiState(
    val callbacks: Callbacks,
    val loadingState: LoadingState,
) {
    @Immutable
    interface Callbacks {
        fun onDeleteRequest(user: UserSummary)
        fun onDeleteCancel()
        fun onDeleteConfirm()
    }

    sealed interface LoadingState {
        data object Loading : LoadingState
        data class Error(val message: String) : LoadingState
        data class Loaded(
            val users: List<UserSummary>,
            val deleteConfirm: UserSummary?,
            val isDeleteInProgress: Boolean,
        ) : LoadingState
    }
}
