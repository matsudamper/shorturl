package ui.userlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import model.UserSummary

@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope) { UserListScreenViewModel(coroutineScope = scope) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(object : UserListScreenViewModel.Event {
                override fun onLoggedOut() {
                    onLoggedOut()
                }

                override fun showSnackBar(message: String) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← 戻る") }
                Text("ユーザー管理", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            is UserListScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UserListScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loadingState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UserListScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.padding(paddingValues),
                    state = loadingState,
                    callbacks = uiState.callbacks,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: UserListScreenUiState.LoadingState.Loaded,
    callbacks: UserListScreenUiState.Callbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("合計: ${state.users.size} 人", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            state.users.forEach { user ->
                UserCard(
                    user = user,
                    deleting = state.isDeleteInProgress && state.deleteConfirm?.id == user.id,
                    onDelete = { callbacks.onDeleteRequest(user) },
                )
            }
        }
    }

    state.deleteConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { callbacks.onDeleteCancel() },
            title = { Text("削除の確認") },
            text = { Text("ユーザー「${target.username}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleteInProgress,
                    onClick = { callbacks.onDeleteConfirm() },
                ) {
                    Text(
                        if (state.isDeleteInProgress) "削除中..." else "削除",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isDeleteInProgress,
                    onClick = { callbacks.onDeleteCancel() },
                ) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun UserCard(
    user: UserSummary,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(user.username, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "ID: ${user.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    enabled = !deleting,
                    onClick = onDelete,
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
