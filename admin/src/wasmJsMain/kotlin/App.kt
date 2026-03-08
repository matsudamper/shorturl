import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import model.ShortenedUrl
import ui.*

// ---- Route definitions ----
@Serializable
data object LoginRoute : NavKey

@Serializable
data object HashGeneratorRoute : NavKey

@Serializable
data object UrlListRoute : NavKey

@Serializable
data object UrlCreateRoute : NavKey

@Serializable
data class UrlEditRoute(val url: ShortenedUrl) : NavKey

@Serializable
data class AnalyticsRoute(val urlId: String, val slug: String) : NavKey

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(LoginRoute::class)
            subclass(HashGeneratorRoute::class)
            subclass(UrlListRoute::class)
            subclass(UrlCreateRoute::class)
            subclass(UrlEditRoute::class)
            subclass(AnalyticsRoute::class)
        }
    }
}

@Composable
fun App() {
    val backStack = rememberNavBackStack(navConfig, LoginRoute)

    MaterialTheme {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
        ) { key: NavKey ->
            when (key) {
                LoginRoute -> NavEntry(key) {
                    LoginScreen(
                        onLoggedIn = { backStack.add(UrlListRoute) },
                        onHashGenerator = { backStack.add(HashGeneratorRoute) },
                    )
                }
                HashGeneratorRoute -> NavEntry(key) {
                    HashGeneratorScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                UrlListRoute -> NavEntry(key) {
                    UrlListScreen(
                        onCreateNew = { backStack.add(UrlCreateRoute) },
                        onEdit = { backStack.add(UrlEditRoute(it)) },
                        onAnalytics = { backStack.add(AnalyticsRoute(it.id, it.slug)) },
                        onLogout = {
                            backStack.removeAll { true }
                            backStack.add(LoginRoute)
                        },
                    )
                }
                UrlCreateRoute -> NavEntry(key) {
                    UrlCreateScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onCreated = { backStack.removeLastOrNull() },
                    )
                }
                is UrlEditRoute -> NavEntry(key) {
                    UrlEditScreen(
                        url = (key as UrlEditRoute).url,
                        onBack = { backStack.removeLastOrNull() },
                        onSaved = { backStack.removeLastOrNull() },
                    )
                }
                is AnalyticsRoute -> NavEntry(key) {
                    val route = key as AnalyticsRoute
                    AnalyticsScreen(
                        urlId = route.urlId,
                        slug = route.slug,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                else -> NavEntry(key) {}
            }
        }
    }
}
