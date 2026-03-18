package ui.login

import api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LoginScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val eventHandler = Channel<(Event) -> Unit>(Channel.UNLIMITED)

    private val callbacks = object : LoginScreenUiState.Callbacks {
        override fun updateUsername(value: String) {
            viewModelStateFlow.update { it.copy(username = value) }
        }

        override fun updatePassword(value: String) {
            viewModelStateFlow.update { it.copy(password = value) }
        }

        override fun login() {
            coroutineScope.launch {
                viewModelStateFlow.update { it.copy(isLoading = true) }
                try {
                    val error = ApiClient.login(
                        viewModelStateFlow.value.username,
                        viewModelStateFlow.value.password,
                    )
                    if (error == null) {
                        eventHandler.trySend { it.onLoggedIn() }
                    } else {
                        eventHandler.trySend { it.showSnackBar(error) }
                    }
                } catch (e: Throwable) {
                    eventHandler.trySend { it.showSnackBar(e.message ?: "ログインに失敗しました") }
                } finally {
                    viewModelStateFlow.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    val uiState: StateFlow<LoginScreenUiState> = MutableStateFlow(
        LoginScreenUiState(
            callbacks = callbacks,
            state = LoginScreenUiState.State(),
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        state = LoginScreenUiState.State(
                            username = viewModelState.username,
                            password = viewModelState.password,
                            isLoading = viewModelState.isLoading,
                        )
                    )
                }
            }
        }
    }.asStateFlow()

    interface Event {
        fun onLoggedIn()
        fun showSnackBar(message: String)
    }

    data class ViewModelState(
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
    )
}
