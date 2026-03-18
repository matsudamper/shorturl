package ui.urllist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import model.ShortenedUrl

@Composable
fun UrlListScreen(
    onCreateNew: () -> Unit,
    onManageUsers: () -> Unit,
    onEdit: (ShortenedUrl) -> Unit,
    onAnalytics: (ShortenedUrl) -> Unit,
    onLogout: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope) { UrlListScreenViewModel(coroutineScope = scope) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(object : UrlListScreenViewModel.Event {
                override fun onUnauthorized() {
                    onUnauthorized()
                }

                override fun onLoggedOut() {
                    onLogout()
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    "URL一覧",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { uiState.callbacks.onLogout() }) {
                    Text("ログアウト")
                }
            }
        },
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            is UrlListScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UrlListScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loadingState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UrlListScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.padding(paddingValues),
                    state = loadingState,
                    callbacks = uiState.callbacks,
                    onCreateNew = onCreateNew,
                    onManageUsers = onManageUsers,
                    onEdit = onEdit,
                    onAnalytics = onAnalytics,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: UrlListScreenUiState.LoadingState.Loaded,
    callbacks: UrlListScreenUiState.Callbacks,
    onCreateNew: () -> Unit,
    onManageUsers: () -> Unit,
    onEdit: (ShortenedUrl) -> Unit,
    onAnalytics: (ShortenedUrl) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreateNew) { Text("+ 新規作成") }
            OutlinedButton(onClick = onManageUsers) { Text("ユーザー管理") }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { callbacks.onSearchQueryChange(it) },
                label = { Text("スラッグ・URLで検索") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { callbacks.onSearch() }) { Text("検索") }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(state.urls, key = { it.id }) { url ->
                UrlCard(
                    url = url,
                    onEdit = { onEdit(url) },
                    onAnalytics = { onAnalytics(url) },
                    onDelete = { callbacks.onDeleteRequest(url) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("合計: ${state.total} 件", modifier = Modifier.weight(1f))
            TextButton(
                onClick = { callbacks.onPrevPage() },
                enabled = state.offset > 0,
            ) { Text("← 前") }
            Text("${state.offset / state.pageSize + 1} / ${((state.total - 1) / state.pageSize + 1).coerceAtLeast(1)}")
            TextButton(
                onClick = { callbacks.onNextPage() },
                enabled = state.offset + state.pageSize < state.total,
            ) { Text("次 →") }
        }
    }

    state.deleteConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { callbacks.onDeleteCancel() },
            title = { Text("削除の確認") },
            text = { Text("「/${target.slug}」を削除しますか？") },
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
private fun UrlCard(
    url: ShortenedUrl,
    onEdit: () -> Unit,
    onAnalytics: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "/${url.slug}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("${url.clickCount} クリック", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                url.originalUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onAnalytics) { Text("解析") }
                TextButton(onClick = onEdit) { Text("編集") }
                TextButton(onClick = onDelete) { Text("削除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
