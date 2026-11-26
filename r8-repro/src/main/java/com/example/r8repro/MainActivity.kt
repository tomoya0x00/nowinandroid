package com.example.r8repro

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callbacks = FeedCallbacks(tag = TAG)
        setContent {
            MaterialTheme {
                Surface { FeedRoute(callbacks) }
            }
        }
    }
}

/**
 * Callbacks built outside composition. Reading them as properties gives the Compose compiler
 * no static information, so the changed flags passed further down are computed at runtime
 * instead of being folded into constants. With compile-time-static lambdas here, R8 folds the
 * flags and the byte field does not appear.
 */
class FeedCallbacks(private val tag: String) {
    private fun log(msg: String) {
        Log.i(tag, msg)
    }

    val onCardTap: (FeedItem, Int, FeedSection, List<FeedItem>) -> Unit = { item, _, _, _ -> log("card ${item.id}") }
    val onCardLongPress: (FeedItem, Int, FeedSection) -> Unit = { item, _, _ -> log("long press ${item.id}") }
    val onSectionTitleTap: (FeedSection) -> Unit = { log("section ${it.id}") }
    val onSeeAllTap: (Int, FeedSection) -> Unit = { p, _ -> log("see all $p") }
    val onChipSelect: (Int, FeedSection) -> Unit = { p, _ -> log("chip $p") }
    val onRecentOpen = OnRecentTap { item, _ -> log("recent open ${item.id}") }
    val onRecentShare = OnRecentTap { item, _ -> log("recent share ${item.id}") }
    val onRecentPin = OnRecentTap { item, _ -> log("recent pin ${item.id}") }
    val onRecentRemove = OnRecentTap { item, _ -> log("recent remove ${item.id}") }
    val onRecentReport = OnRecentTap { item, _ -> log("recent report ${item.id}") }
    val onRecentClear: () -> Unit = { log("recent clear") }
    val onRecentRefresh: () -> Unit = { log("recent refresh") }
    val onTagTap: (IndexedValue<Tag>) -> Unit = { log("tag ${it.value.name}") }
    val onAuthorTap: (IndexedValue<Author>) -> Unit = { log("author ${it.value.name}") }
    val onHighlightTap = OnHighlightTap { s, p -> log("highlight ${s.id} $p") }
    val onPromoTap = OnPromoTap { log("promo $it") }
    val onHelpTap: () -> Unit = { log("help") }
    val onTipTap: () -> Unit = { log("tip") }
    val onTipDismiss: () -> Unit = { log("tip dismiss") }
    val onOpenLink: (String, String) -> Unit = { url, title -> log("open $url $title") }
}

@Composable
fun FeedRoute(callbacks: FeedCallbacks) {
    val state = remember { FeedState() }
    val headerState = remember { FixedHeaderState("Feed") }
    val snackbarHostState = remember { SnackbarHostState() }

    FeedScreen(
        state = state,
        headerState = headerState,
        snackbarHostState = snackbarHostState,
        nestedScrollConnection = null,
        onCardTap = callbacks.onCardTap,
        onCardLongPress = callbacks.onCardLongPress,
        onSectionTitleTap = callbacks.onSectionTitleTap,
        onSeeAllTap = callbacks.onSeeAllTap,
        onChipSelect = callbacks.onChipSelect,
        onRecentOpen = callbacks.onRecentOpen,
        onRecentShare = callbacks.onRecentShare,
        onRecentPin = callbacks.onRecentPin,
        onRecentRemove = callbacks.onRecentRemove,
        onRecentReport = callbacks.onRecentReport,
        onRecentClear = callbacks.onRecentClear,
        onRecentRefresh = callbacks.onRecentRefresh,
        onTagTap = callbacks.onTagTap,
        onAuthorTap = callbacks.onAuthorTap,
        onHighlightTap = callbacks.onHighlightTap,
        onPromoTap = callbacks.onPromoTap,
        onHelpTap = callbacks.onHelpTap,
        onTipTap = callbacks.onTipTap,
        onTipDismiss = callbacks.onTipDismiss,
        onOpenLink = callbacks.onOpenLink,
    )
}
