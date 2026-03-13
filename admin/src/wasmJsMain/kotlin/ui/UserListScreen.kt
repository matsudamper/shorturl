package ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import api.ApiClient
import api.isUnauthorizedApiError
import kotlinx.coroutines.launch
import model.UserSummary

@Composable
fun UserListScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var deleteConfirm by remember { mutableStateOf<UserSummary?>(null) }
    var deletingUserId by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = ""
            ApiClient.listUsers().fold(
                onSuccess = { users = it },
                onFailure = {
                    if (it.isUnauthorizedApiError()) {
                        onLoggedOut()
                    } else {
                        error = it.message ?: "エラー"
                    }
                },
            )
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← 戻る") }
            Text("ユーザー管理", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(12.dp))

        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Text("合計: ${users.size} 人", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                users.forEach { user ->
                    UserCard(
                        user = user,
                        deleting = deletingUserId == user.id,
                        onDelete = { deleteConfirm = user },
                    )
                }
            }
        }
    }

    deleteConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (deletingUserId == null) {
                    deleteConfirm = null
                }
            },
            title = { Text("削除の確認") },
            text = { Text("ユーザー「${target.username}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    enabled = deletingUserId == null,
                    onClick = {
                        scope.launch {
                            deletingUserId = target.id
                            error = ""
                            ApiClient.deleteUser(target.id).fold(
                                onSuccess = { response ->
                                    deleteConfirm = null
                                    deletingUserId = null
                                    if (response.deletedCurrentUser) {
                                        onLoggedOut()
                                    } else {
                                        load()
                                    }
                                },
                                onFailure = {
                                    if (it.isUnauthorizedApiError()) {
                                        onLoggedOut()
                                    } else {
                                        error = it.message ?: "削除エラー"
                                        deletingUserId = null
                                    }
                                },
                            )
                        }
                    },
                ) {
                    Text(
                        if (deletingUserId == target.id) "削除中..." else "削除",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = deletingUserId == null,
                    onClick = { deleteConfirm = null },
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
