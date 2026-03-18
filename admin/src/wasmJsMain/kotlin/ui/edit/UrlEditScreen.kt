package ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ui.CopyableReadOnlyTextField

@Composable
fun UrlEditScreen(
    urlId: String,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope) {
        UrlEditScreenViewModel(
            coroutineScope = scope,
            urlId = urlId,
        )
    }
    val clipboardManager by rememberUpdatedState(LocalClipboard.current)
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(
                object : UrlEditScreenViewModel.Event {
                    override fun sendToClipboard(value: String) {
                        scope.launch {
                            clipboardManager.setClipEntry(
                                ClipEntry.withPlainText(value)
                            )
                            snackbarHostState.showSnackbar("コピーしました")
                        }
                    }

                    override fun onUnauthorized() {
                        onUnauthorized()
                    }

                    override fun showSnackBar(message: String) {
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text("← 戻る")
                }
                Text(
                    text = "URL編集",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            is UrlEditScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loadingState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UrlEditScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.padding(paddingValues),
                    state = loadingState,
                    callbacks = uiState.callbacks,
                )
            }

            is UrlEditScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(paddingValues)
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: UrlEditScreenUiState.LoadingState.Loaded,
    callbacks: UrlEditScreenUiState.Callbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        CopyableReadOnlyTextField(
            value = "/${state.slug}",
            label = "スラッグ（変更不可）",
            modifier = Modifier.fillMaxWidth(),
            onClickCopy = {
                callbacks.onClickCopy()
            }
        )

        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "⚠ 301リダイレクトはブラウザにキャッシュされます。変更後も古い宛先にリダイレクトされる場合があります。",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))
        val textFieldState = rememberTextFieldState(state.url)
        LaunchedEffect(Unit) {
            snapshotFlow { textFieldState.text }
                .collectLatest {
                    callbacks.updateUrl(it.toString())
                }
        }
        OutlinedTextField(
            state = textFieldState,
            label = { Text("新しいリダイレクト先URL") },
            modifier = Modifier.fillMaxWidth(),
            lineLimits = TextFieldLineLimits.SingleLine,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                callbacks.save()
            },
            enabled = state.isSaveButtonEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isSaving) "保存中..." else "保存") }
    }
}
