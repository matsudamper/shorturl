package ui.hashgenerator

import api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HashGeneratorScreenViewModel(
    private val coroutineScope: CoroutineScope,
) {
    private val viewModelStateFlow = MutableStateFlow(ViewModelState())
    val eventHandler = Channel<(Event) -> Unit>(Channel.UNLIMITED)

    private val callbacks = object : HashGeneratorScreenUiState.Callbacks {
        override fun updateUsername(value: String) {
            viewModelStateFlow.update { it.copy(username = value) }
        }

        override fun updatePassword(value: String) {
            viewModelStateFlow.update { it.copy(password = value, hash = "") }
        }

        override fun generateHash() {
            coroutineScope.launch {
                viewModelStateFlow.update { it.copy(isLoading = true, hash = "") }
                ApiClient.generateHash(viewModelStateFlow.value.password).fold(
                    onSuccess = { hash ->
                        viewModelStateFlow.update { it.copy(hash = hash) }
                    },
                    onFailure = { e ->
                        eventHandler.trySend { it.showSnackBar(e.message ?: "エラー") }
                    },
                )
                viewModelStateFlow.update { it.copy(isLoading = false) }
            }
        }
    }

    val uiState: StateFlow<HashGeneratorScreenUiState> = MutableStateFlow(
        HashGeneratorScreenUiState(
            callbacks = callbacks,
            state = HashGeneratorScreenUiState.State(),
        )
    ).also { uiStateFlow ->
        coroutineScope.launch {
            viewModelStateFlow.collectLatest { viewModelState ->
                uiStateFlow.update {
                    it.copy(
                        state = HashGeneratorScreenUiState.State(
                            username = viewModelState.username,
                            password = viewModelState.password,
                            hash = viewModelState.hash,
                            isLoading = viewModelState.isLoading,
                        )
                    )
                }
            }
        }
    }.asStateFlow()

    interface Event {
        fun showSnackBar(message: String)
    }

    data class ViewModelState(
        val username: String = "",
        val password: String = "",
        val hash: String = "",
        val isLoading: Boolean = false,
    )
}
