import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import api.ApiClient
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlin.js.ExperimentalWasmJsInterop
import org.w3c.dom.events.Event
import ui.*
import ui.edit.UrlEditScreen

private const val adminBasePath = "/admin"
private const val adminLoginPath = "/admin/"

@Serializable
data object LoginRoute : NavKey

@Serializable
data object HashGeneratorRoute : NavKey

@Serializable
data object UrlListRoute : NavKey

@Serializable
data object UserListRoute : NavKey

@Serializable
data object UrlCreateRoute : NavKey

@Serializable
data class UrlEditRoute(val urlId: String) : NavKey

@Serializable
data class AnalyticsRoute(val urlId: String) : NavKey

private enum class HistoryMode {
    Push,
    Replace,
    None,
}

private fun createTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

private fun currentAdminPath(): String = window.location.pathname.ifBlank { adminLoginPath }

private fun normalizeAdminPath(pathname: String): String {
    val trimmed = pathname.trim().ifBlank { adminLoginPath }
    if (trimmed == adminBasePath || trimmed == adminLoginPath) {
        return adminLoginPath
    }
    if (!trimmed.startsWith("$adminBasePath/")) {
        return adminLoginPath
    }
    return trimmed.trimEnd('/').ifBlank { adminLoginPath }
}

private fun routeStackFromPath(pathname: String): List<NavKey> {
    val normalizedPath = normalizeAdminPath(pathname)
    val segments = normalizedPath
        .removePrefix(adminBasePath)
        .split('/')
        .filter { it.isNotBlank() }

    return when {
        segments.isEmpty() -> listOf(LoginRoute)
        segments == listOf("hash-generator") -> listOf(LoginRoute, HashGeneratorRoute)
        segments == listOf("urls") -> listOf(UrlListRoute)
        segments == listOf("users") -> listOf(UrlListRoute, UserListRoute)
        segments == listOf("urls", "new") -> listOf(UrlListRoute, UrlCreateRoute)
        segments.size == 3 && segments[0] == "urls" && segments[2] == "edit" ->
            listOf(UrlListRoute, UrlEditRoute(segments[1]))
        segments.size == 3 && segments[0] == "urls" && segments[2] == "analytics" ->
            listOf(UrlListRoute, AnalyticsRoute(segments[1]))
        else -> listOf(LoginRoute)
    }
}

private fun pathForRoute(route: NavKey): String =
    when (route) {
        LoginRoute -> adminLoginPath
        HashGeneratorRoute -> "$adminBasePath/hash-generator"
        UrlListRoute -> "$adminBasePath/urls"
        UserListRoute -> "$adminBasePath/users"
        UrlCreateRoute -> "$adminBasePath/urls/new"
        is UrlEditRoute -> "$adminBasePath/urls/${route.urlId}/edit"
        is AnalyticsRoute -> "$adminBasePath/urls/${route.urlId}/analytics"
        else -> adminLoginPath
    }


@OptIn(ExperimentalWasmJsInterop::class)
private fun syncBrowserPath(path: String, mode: HistoryMode) {
    when (mode) {
        HistoryMode.Push -> {
            if (currentAdminPath() == path) {
                window.history.replaceState(null, "", path)
            } else {
                window.history.pushState(null, "", path)
            }
        }
        HistoryMode.Replace -> window.history.replaceState(null, "", path)
        HistoryMode.None -> Unit
    }
}

@Composable
fun App() {
    val initialBackStack = remember { routeStackFromPath(currentAdminPath()) }
    val backStack = remember { NavBackStack<NavKey>(*initialBackStack.toTypedArray()) }
    val fontState = rememberCustomFontState()
    var sessionChecking by remember {
        mutableStateOf(initialBackStack == listOf(LoginRoute))
    }

    fun push(route: NavKey) {
        backStack.add(route)
        syncBrowserPath(pathForRoute(route), HistoryMode.Push)
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            syncBrowserPath(pathForRoute(backStack.lastOrNull() ?: LoginRoute), HistoryMode.Replace)
        }
    }

    fun resetTo(route: NavKey) {
        backStack.clear()
        backStack.add(route)
        syncBrowserPath(pathForRoute(route), HistoryMode.Replace)
    }

    DisposableEffect(backStack) {
        syncBrowserPath(pathForRoute(backStack.lastOrNull() ?: LoginRoute), HistoryMode.Replace)
        val listener: (Event) -> Unit = {
            val routes = routeStackFromPath(currentAdminPath())
            backStack.clear()
            backStack.addAll(routes)
        }
        window.addEventListener(type = "popstate", callback = listener)
        onDispose {
            window.removeEventListener(type = "popstate", callback = listener)
        }
    }

    LaunchedEffect(fontState.isReady) {
        if (!fontState.isReady) return@LaunchedEffect
        if (!sessionChecking) return@LaunchedEffect
        val authenticated = ApiClient.checkSession()
        if (authenticated) {
            backStack.clear()
            backStack.add(UrlListRoute)
            syncBrowserPath(pathForRoute(UrlListRoute), HistoryMode.Replace)
        }
        sessionChecking = false
    }

    if (!fontState.isReady || sessionChecking) {
        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    MaterialTheme(typography = createTypography(fontState.fontFamily)) {
        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
        ) { key: NavKey ->
            when (key) {
                LoginRoute -> NavEntry(key) {
                    LoginScreen(
                        onLoggedIn = { resetTo(UrlListRoute) },
                        onHashGenerator = { push(HashGeneratorRoute) },
                    )
                }
                HashGeneratorRoute -> NavEntry(key) {
                    HashGeneratorScreen(
                        onBack = { pop() },
                    )
                }
                UrlListRoute -> NavEntry(key) {
                    UrlListScreen(
                        onCreateNew = { push(UrlCreateRoute) },
                        onManageUsers = { push(UserListRoute) },
                        onEdit = { push(UrlEditRoute(it.id)) },
                        onAnalytics = { push(AnalyticsRoute(it.id)) },
                        onLogout = { resetTo(LoginRoute) },
                        onUnauthorized = { resetTo(LoginRoute) },
                    )
                }
                UserListRoute -> NavEntry(key) {
                    UserListScreen(
                        onBack = { pop() },
                        onLoggedOut = { resetTo(LoginRoute) },
                    )
                }
                UrlCreateRoute -> NavEntry(key) {
                    UrlCreateScreen(
                        onBack = { pop() },
                        onCreated = { pop() },
                        onUnauthorized = { resetTo(LoginRoute) },
                    )
                }
                is UrlEditRoute -> NavEntry(key) {
                    UrlEditScreen(
                        urlId = key.urlId,
                        onBack = { pop() },
                        onUnauthorized = { resetTo(LoginRoute) },
                    )
                }
                is AnalyticsRoute -> NavEntry(key) {
                    AnalyticsScreen(
                        urlId = key.urlId,
                        onBack = { pop() },
                        onUnauthorized = { resetTo(LoginRoute) },
                    )
                }
                else -> NavEntry(key) {}
            }
        }
    }
}
