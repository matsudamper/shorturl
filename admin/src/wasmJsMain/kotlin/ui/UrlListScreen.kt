package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import api.ApiClient
import kotlinx.coroutines.launch
import model.ShortenedUrl

private const val PAGE_SIZE = 20

@Composable
fun UrlListScreen(
    onCreateNew: () -> Unit,
    onManageUsers: () -> Unit,
    onEdit: (ShortenedUrl) -> Unit,
    onAnalytics: (ShortenedUrl) -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var urls by remember { mutableStateOf<List<ShortenedUrl>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }
    var offset by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf<ShortenedUrl?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = ""
            ApiClient.listUrls(offset, PAGE_SIZE, query.ifBlank { null }).fold(
                onSuccess = { urls = it.items; total = it.total },
                onFailure = { error = it.message ?: "エラー" },
            )
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("URL一覧", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    scope.launch { ApiClient.logout(); onLogout() }
                }) { Text("ログアウト") }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreateNew) { Text("+ 新規作成") }
                OutlinedButton(onClick = onManageUsers) { Text("ユーザー管理") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 検索
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("スラッグ・URLで検索") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { offset = 0; load() }) { Text("検索") }
        }

        Spacer(Modifier.height(8.dp))

        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(urls, key = { it.id }) { url ->
                    UrlCard(
                        url = url,
                        onEdit = { onEdit(url) },
                        onAnalytics = { onAnalytics(url) },
                        onDelete = { deleteConfirm = url },
                    )
                }
            }

            // ページネーション
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("合計: $total 件", modifier = Modifier.weight(1f))
                TextButton(onClick = { offset -= PAGE_SIZE; load() }, enabled = offset > 0) { Text("← 前") }
                Text("${offset / PAGE_SIZE + 1} / ${((total - 1) / PAGE_SIZE + 1).coerceAtLeast(1)}")
                TextButton(onClick = { offset += PAGE_SIZE; load() }, enabled = offset + PAGE_SIZE < total) { Text("次 →") }
            }
        }
    }

    // 削除確認ダイアログ
    deleteConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("削除の確認") },
            text = { Text("「/${target.slug}」を削除しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ApiClient.deleteUrl(target.id).onSuccess { load() }
                        deleteConfirm = null
                    }
                }) { Text("削除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = null }) { Text("キャンセル") }
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
                Text("/${url.slug}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
