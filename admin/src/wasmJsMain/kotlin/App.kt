import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import model.ShortenedUrl
import ui.*

// ---- Route definitions ----
object LoginRoute
object HashGeneratorRoute
object UrlListRoute
object UrlCreateRoute
data class UrlEditRoute(val url: ShortenedUrl)
data class AnalyticsRoute(val urlId: String, val slug: String)

@Composable
fun App() {
    val backStack = rememberNavBackStack(LoginRoute)

    MaterialTheme {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
        ) { entry: NavEntry<Any> ->
            when (val route = entry.key) {
                LoginRoute -> LoginScreen(
                    onLoggedIn = { backStack.add(UrlListRoute) },
                    onHashGenerator = { backStack.add(HashGeneratorRoute) },
                )
                HashGeneratorRoute -> HashGeneratorScreen(
                    onBack = { backStack.removeLastOrNull() },
                )
                UrlListRoute -> UrlListScreen(
                    onCreateNew = { backStack.add(UrlCreateRoute) },
                    onEdit = { backStack.add(UrlEditRoute(it)) },
                    onAnalytics = { backStack.add(AnalyticsRoute(it.id, it.slug)) },
                    onLogout = {
                        backStack.removeAll { true }
                        backStack.add(LoginRoute)
                    },
                )
                UrlCreateRoute -> UrlCreateScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onCreated = { backStack.removeLastOrNull() },
                )
                is UrlEditRoute -> UrlEditScreen(
                    url = route.url,
                    onBack = { backStack.removeLastOrNull() },
                    onSaved = { backStack.removeLastOrNull() },
                )
                is AnalyticsRoute -> AnalyticsScreen(
                    urlId = route.urlId,
                    slug = route.slug,
                    onBack = { backStack.removeLastOrNull() },
                )
                else -> {}
            }
        }
    }
}
