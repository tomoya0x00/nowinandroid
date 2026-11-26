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
 * (as in a real app whose callbacks come from navigation), not folded into constants.
 */
class FeedCallbacks(private val tag: String) {
    private fun log(msg: String) {
        Log.i(tag, msg)
    }

    val onItemClick: (FeedItem, Int, FeedSection, List<FeedItem>) -> Unit = { item, _, _, _ -> log("item ${item.id}") }
    val onItemPreviewClick: (FeedItem, Int, FeedSection) -> Unit = { item, _, _ -> log("preview ${item.id}") }
    val onMoreClick: (FeedSection) -> Unit = { log("more ${it.id}") }
    val onMoreCardClick: (Int, FeedSection) -> Unit = { p, _ -> log("more card $p") }
    val onTabSelect: (Int, FeedSection) -> Unit = { p, _ -> log("tab $p") }
    val onHistoryInfoClick = OnHistoryClick { item, _ -> log("history info ${item.id}") }
    val onHistoryImageClick = OnHistoryClick { item, _ -> log("history image ${item.id}") }
    val onHistoryDetailClick = OnHistoryClick { item, _ -> log("history detail ${item.id}") }
    val onHistoryDeleteClick = OnHistoryClick { item, _ -> log("history delete ${item.id}") }
    val onHistoryMenuClick = OnHistoryClick { item, _ -> log("history menu ${item.id}") }
    val onHistoryMoreClick: () -> Unit = { log("history more") }
    val onHistoryMoreCardClick: () -> Unit = { log("history more card") }
    val onTopicClick: (IndexedValue<Topic>) -> Unit = { log("topic ${it.value.name}") }
    val onCategoryClick: (IndexedValue<Category>) -> Unit = { log("category ${it.value.name}") }
    val onSpotlightClick = OnSpotlightClick { s, p -> log("spotlight ${s.id} $p") }
    val onBannerClick = OnBannerClick { log("banner $it") }
    val onDailyTipClick: () -> Unit = { log("daily tip") }
    val onTutorialBannerClick: () -> Unit = { log("tutorial") }
    val onTutorialBannerDismiss: () -> Unit = { log("tutorial dismiss") }
    val onOpenLink: (String, String) -> Unit = { url, title -> log("open $url $title") }
}

@Composable
fun FeedRoute(callbacks: FeedCallbacks) {
    val state = remember { FeedState() }
    val topBarState = remember { TopBarStateImpl() }
    val snackbarHostState = remember { SnackbarHostState() }

    FeedScreen(
        state = state,
        topBarState = topBarState,
        snackbarHostState = snackbarHostState,
        nestedScrollConnection = null,
        onItemClick = callbacks.onItemClick,
        onItemPreviewClick = callbacks.onItemPreviewClick,
        onMoreClick = callbacks.onMoreClick,
        onMoreCardClick = callbacks.onMoreCardClick,
        onTabSelect = callbacks.onTabSelect,
        onHistoryInfoClick = callbacks.onHistoryInfoClick,
        onHistoryImageClick = callbacks.onHistoryImageClick,
        onHistoryDetailClick = callbacks.onHistoryDetailClick,
        onHistoryDeleteClick = callbacks.onHistoryDeleteClick,
        onHistoryMenuClick = callbacks.onHistoryMenuClick,
        onHistoryMoreClick = callbacks.onHistoryMoreClick,
        onHistoryMoreCardClick = callbacks.onHistoryMoreCardClick,
        onTopicClick = callbacks.onTopicClick,
        onCategoryClick = callbacks.onCategoryClick,
        onSpotlightClick = callbacks.onSpotlightClick,
        onBannerClick = callbacks.onBannerClick,
        onDailyTipClick = callbacks.onDailyTipClick,
        onTutorialBannerClick = callbacks.onTutorialBannerClick,
        onTutorialBannerDismiss = callbacks.onTutorialBannerDismiss,
        onOpenLink = callbacks.onOpenLink,
    )
}
