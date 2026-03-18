package ui.userlist

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
import model.UserSummary

internal class UserListScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val eventHandler = Channel<(Event) -> Unit>(Channel.UNLIMITED)

    private val callbacks = object : UserListScreenUiState.Callbacks {
        override fun onDeleteRequest(user: UserSummary) {
            viewModelStateFlow.update { it.copy(deleteConfirm = user) }
        }

        override fun onDeleteCancel() {
            if (viewModelStateFlow.value.isDeleteInProgress) return
            viewModelStateFlow.update { it.copy(deleteConfirm = null) }
        }

        override fun onDeleteConfirm() {
            val target = viewModelStateFlow.value.deleteConfirm ?: return
            coroutineScope.launch {
                viewModelStateFlow.update { it.copy(isDeleteInProgress = true) }
                ApiClient.deleteUser(target.id).fold(
                    onSuccess = { response ->
                        viewModelStateFlow.update { it.copy(deleteConfirm = null, isDeleteInProgress = false) }
                        if (response.deletedCurrentUser) {
                            eventHandler.trySend { it.onLoggedOut() }
                        } else {
                            load()
                        }
                    },
                    onFailure = { e ->
                        viewModelStateFlow.update { it.copy(isDeleteInProgress = false) }
                        if (e.isUnauthorizedApiError()) {
                            eventHandler.trySend { it.onLoggedOut() }
                        } else {
                            eventHandler.trySend { it.showSnackBar(e.message ?: "削除エラー") }
                        }
                    },
                )
            }
        }
    }

    val uiState: StateFlow<UserListScreenUiState> = MutableStateFlow(
        UserListScreenUiState(
            callbacks = callbacks,
            loadingState = UserListScreenUiState.LoadingState.Loading,
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        loadingState = run loadingState@{
                            if (viewModelState.errorMessage != null) {
                                return@loadingState UserListScreenUiState.LoadingState.Error(viewModelState.errorMessage)
                            }
                            if (viewModelState.isLoading) {
                                return@loadingState UserListScreenUiState.LoadingState.Loading
                            }
                            UserListScreenUiState.LoadingState.Loaded(
                                users = viewModelState.users,
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
            viewModelStateFlow.update { it.copy(isLoading = true, errorMessage = null) }
            ApiClient.listUsers().fold(
                onSuccess = { users ->
                    viewModelStateFlow.update { it.copy(users = users, isLoading = false) }
                },
                onFailure = { e ->
                    if (e.isUnauthorizedApiError()) {
                        eventHandler.trySend { it.onLoggedOut() }
                    } else {
                        viewModelStateFlow.update {
                            it.copy(isLoading = false, errorMessage = e.message ?: "エラー")
                        }
                    }
                },
            )
        }
    }

    interface Event {
        fun onLoggedOut()
        fun showSnackBar(message: String)
    }

    data class ViewModelState(
        val users: List<UserSummary> = emptyList(),
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val deleteConfirm: UserSummary? = null,
        val isDeleteInProgress: Boolean = false,
    )
}
