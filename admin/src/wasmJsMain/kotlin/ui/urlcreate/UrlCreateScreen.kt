package ui.urlcreate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun UrlCreateScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope) { UrlCreateScreenViewModel(coroutineScope = scope) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(object : UrlCreateScreenViewModel.Event {
                override fun onCreated() {
                    onCreated()
                }

                override fun onUnauthorized() {
                    onUnauthorized()
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
                Text("URL作成", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { paddingValues ->
        when (val loadingState = uiState.loadingState) {
            is UrlCreateScreenUiState.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UrlCreateScreenUiState.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loadingState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UrlCreateScreenUiState.LoadingState.Loaded -> {
                LoadedContent(
                    modifier = Modifier.padding(paddingValues),
                    state = loadingState,
                    callbacks = uiState.callbacks,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: UrlCreateScreenUiState.LoadingState.Loaded,
    callbacks: UrlCreateScreenUiState.Callbacks,
    viewModel: UrlCreateScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Spacer(Modifier.height(8.dp))

        // モード切り替え
        Row {
            FilterChip(
                selected = state.isAutoMode,
                onClick = { callbacks.onModeChange(true) },
                label = { Text("自動生成") },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !state.isAutoMode,
                onClick = { callbacks.onModeChange(false) },
                label = { Text("手動指定") },
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.originalUrl,
            onValueChange = { callbacks.onOriginalUrlChange(it) },
            label = { Text("リダイレクト先URL") },
            placeholder = { Text("https://example.com/...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        if (state.isAutoMode) {
            AutoModeContent(
                state = state,
                callbacks = callbacks,
                viewModel = viewModel,
            )
        } else {
            ManualModeContent(
                state = state,
                callbacks = callbacks,
            )
        }
    }
}

@Composable
private fun AutoModeContent(
    state: UrlCreateScreenUiState.LoadingState.Loaded,
    callbacks: UrlCreateScreenUiState.Callbacks,
    viewModel: UrlCreateScreenViewModel,
) {
    // 生成タイプ選択
    Text("スラッグタイプ", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        viewModel.slugTypes.forEach { (key, label) ->
            FilterChip(
                selected = state.selectedType == key,
                onClick = { callbacks.onSlugTypeChange(key) },
                label = { Text(label) },
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    val min = viewModel.minLengthFor(state.selectedType).toFloat()
    val max = viewModel.maxLengthFor(state.selectedType).toFloat()
    Text("文字数: ${state.slugLength}", style = MaterialTheme.typography.labelLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = state.slugLength.toFloat(),
            onValueChange = { callbacks.onSlugLengthChange(it.toInt()) },
            valueRange = min..max,
            steps = (max - min - 1).toInt(),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = state.slugLength.toString(),
            onValueChange = {
                it.toIntOrNull()?.coerceIn(min.toInt(), max.toInt())?.let { v ->
                    callbacks.onSlugLengthChange(v)
                }
            },
            singleLine = true,
            modifier = Modifier.width(72.dp),
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { callbacks.generateSlug() },
            enabled = !state.isGenerating,
        ) { Text(if (state.isGenerating) "生成中..." else "スラッグを生成") }

        if (state.previewSlug.isNotBlank()) {
            Spacer(Modifier.width(16.dp))
            Text("プレビュー: /${state.previewSlug}", style = MaterialTheme.typography.titleMedium)
        }
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { callbacks.create() },
        enabled = !state.isSaving && state.previewSlug.isNotBlank() && state.originalUrl.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (state.isSaving) "登録中..." else "登録") }
}

@Composable
private fun ManualModeContent(
    state: UrlCreateScreenUiState.LoadingState.Loaded,
    callbacks: UrlCreateScreenUiState.Callbacks,
) {
    OutlinedTextField(
        value = state.manualSlug,
        onValueChange = { callbacks.onManualSlugChange(it) },
        label = { Text("スラッグ（2〜128文字）") },
        placeholder = { Text("my-custom-slug") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        supportingText = { Text("Unicode（絵文字・日本語）可") },
    )

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = { callbacks.create() },
        enabled = !state.isSaving && state.manualSlug.isNotBlank() && state.originalUrl.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (state.isSaving) "登録中..." else "登録") }
}
