package ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.receiveAsFlow
import model.AnalyticsSummary

@Composable
fun AnalyticsScreen(
    urlId: String,
    onBack: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope, urlId) { AnalyticsScreenViewModel(coroutineScope = scope, urlId = urlId) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(object : AnalyticsScreenViewModel.Event {
                override fun onUnauthorized() {
                    onUnauthorized()
                }
            })
        }
    }

    Scaffold(
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← 戻る") }
                val title = when (val state = uiState.loadingState) {
                    is AnalyticsScreenUiState.LoadingState.Loaded -> "アクセス解析: /${state.slug}"
                    else -> "アクセス解析"
                }
                Text(title, style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            is AnalyticsScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is AnalyticsScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loadingState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is AnalyticsScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.padding(paddingValues),
                    state = loadingState,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: AnalyticsScreenUiState.LoadingState.Loaded,
    modifier: Modifier = Modifier,
) {
    val s = state.summary
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("総クリック数", style = MaterialTheme.typography.labelLarge)
                    Text("${s.totalClicks}", style = MaterialTheme.typography.displaySmall)
                }
            }
        }

        item {
            StatSection(
                title = "日別アクセス",
                data = s.dailyStats.entries.sortedByDescending { it.key }.take(14).associate { it.key to it.value },
            )
        }

        item {
            StatSection(
                title = "時間帯別アクセス",
                data = (0..23).associate { h ->
                    h.toString().padStart(2, '0') + "時" to (s.hourlyStats[h.toString()] ?: 0L)
                },
            )
        }

        item { StatSection(title = "デバイス", data = s.deviceTypes) }

        item { StatSection(title = "ブラウザ", data = s.browsers) }

        item {
            StatSection(
                title = "リファラー（上位10件）",
                data = s.referrers.entries.sortedByDescending { it.value }.take(10).associate { it.key to it.value },
            )
        }
    }
}

@Composable
private fun StatSection(title: String, data: Map<String, Long>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (data.isEmpty()) {
                Text("データなし", color = MaterialTheme.colorScheme.outline)
            } else {
                val maxVal = data.values.maxOrNull() ?: 1L
                data.entries.forEach { (label, count) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(count.toFloat() / maxVal.toFloat())
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("$count", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
