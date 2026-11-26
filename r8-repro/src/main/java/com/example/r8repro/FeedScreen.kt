package com.example.r8repro

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

/**
 * Restartable parent that forwards its own parameters to [FeedContent]. The changed flags it
 * passes are therefore computed from its own dirty ints (not constants), and a recomposition of
 * this function passes "Same" for every unchanged argument.
 */
@Composable
fun FeedScreen(
    state: FeedState,
    topBarState: TopBarState,
    snackbarHostState: SnackbarHostState,
    nestedScrollConnection: NestedScrollConnection?,
    modifier: Modifier = Modifier,
    onItemClick: (item: FeedItem, position: Int, section: FeedSection, items: List<FeedItem>) -> Unit,
    onItemPreviewClick: (item: FeedItem, position: Int, section: FeedSection) -> Unit,
    onMoreClick: (section: FeedSection) -> Unit,
    onMoreCardClick: (position: Int, section: FeedSection) -> Unit,
    onTabSelect: (tabPosition: Int, section: FeedSection) -> Unit,
    onHistoryInfoClick: OnHistoryClick,
    onHistoryImageClick: OnHistoryClick,
    onHistoryDetailClick: OnHistoryClick,
    onHistoryDeleteClick: OnHistoryClick,
    onHistoryMenuClick: OnHistoryClick,
    onHistoryMoreClick: () -> Unit,
    onHistoryMoreCardClick: () -> Unit,
    onTopicClick: (IndexedValue<Topic>) -> Unit,
    onCategoryClick: (IndexedValue<Category>) -> Unit,
    onSpotlightClick: OnSpotlightClick,
    onBannerClick: OnBannerClick,
    onDailyTipClick: () -> Unit,
    onTutorialBannerClick: () -> Unit,
    onTutorialBannerDismiss: () -> Unit,
    onOpenLink: (url: String, title: String) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    var hasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { hasFocus }
            .filter { it }
            .collectLatest { topBarState.collapse() }
    }

    // State read in this function's own body (not inside a content lambda): changing it
    // recomposes FeedScreen, which calls FeedContent again with "Same" flags for every
    // unchanged argument.
    val parentTick = state.parentTick
    Log.i(TAG, "FeedScreen composed, parentTick=$parentTick")

    Column(modifier = modifier) {
        Button(
            modifier = Modifier.padding(8.dp).testTag("recomposeParent"),
            onClick = { state.parentTick++ },
        ) { Text("Recompose parent ($parentTick)") }

        FeedContent(
            // A new Modifier on each parent recomposition, so FeedContent does not skip: it runs
            // with "Different" for `modifier` and "Same" for every other argument, and its
            // restart lambda captures those flags.
            modifier = Modifier
                .onFocusChanged { hasFocus = it.hasFocus }
                .padding(top = (parentTick % 2).dp),
            state = state,
            topBarState = topBarState,
            lazyListState = lazyListState,
            snackbarHostState = snackbarHostState,
            nestedScrollConnection = nestedScrollConnection,
            onItemClick = onItemClick,
            onSpotlightClick = onSpotlightClick,
            onBannerClick = onBannerClick,
            onItemPreviewClick = onItemPreviewClick,
            onMoreClick = onMoreClick,
            onMoreCardClick = onMoreCardClick,
            onTabSelect = onTabSelect,
            onHistoryInfoClick = onHistoryInfoClick,
            onHistoryImageClick = onHistoryImageClick,
            onHistoryDetailClick = onHistoryDetailClick,
            onHistoryDeleteClick = onHistoryDeleteClick,
            onHistoryMenuClick = onHistoryMenuClick,
            onHistoryMoreClick = onHistoryMoreClick,
            onHistoryMoreCardClick = onHistoryMoreCardClick,
            onTopicClick = onTopicClick,
            onCategoryClick = onCategoryClick,
            onDailyTipClick = onDailyTipClick,
            onTutorialBannerClick = onTutorialBannerClick,
            onTutorialBannerDismiss = onTutorialBannerDismiss,
            onOpenLink = onOpenLink,
        )
    }
}
