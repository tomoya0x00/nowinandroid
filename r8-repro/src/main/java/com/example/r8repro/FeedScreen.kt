package com.example.r8repro

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp

/**
 * Restartable parent that forwards its own parameters to [FeedContent]. The changed flags it
 * passes are therefore computed from its own dirty ints at runtime (not constants), and a
 * recomposition of this function passes "Same" for every unchanged argument.
 */
@Composable
fun FeedScreen(
    state: FeedState,
    headerState: HeaderState,
    snackbarHostState: SnackbarHostState,
    nestedScrollConnection: NestedScrollConnection?,
    modifier: Modifier = Modifier,
    onCardTap: (item: FeedItem, position: Int, section: FeedSection, items: List<FeedItem>) -> Unit,
    onCardLongPress: (item: FeedItem, position: Int, section: FeedSection) -> Unit,
    onSectionTitleTap: (section: FeedSection) -> Unit,
    onSeeAllTap: (position: Int, section: FeedSection) -> Unit,
    onChipSelect: (chipPosition: Int, section: FeedSection) -> Unit,
    onRecentOpen: OnRecentTap,
    onRecentShare: OnRecentTap,
    onRecentPin: OnRecentTap,
    onRecentRemove: OnRecentTap,
    onRecentReport: OnRecentTap,
    onRecentClear: () -> Unit,
    onRecentRefresh: () -> Unit,
    onTagTap: (IndexedValue<Tag>) -> Unit,
    onAuthorTap: (IndexedValue<Author>) -> Unit,
    onHighlightTap: OnHighlightTap,
    onPromoTap: OnPromoTap,
    onHelpTap: () -> Unit,
    onTipTap: () -> Unit,
    onTipDismiss: () -> Unit,
    onOpenLink: (url: String, title: String) -> Unit,
) {
    val listState = rememberLazyListState()

    // State read in this function's own body (not inside a content lambda): changing it
    // recomposes FeedScreen, which calls FeedContent again with "Same" flags for every
    // unchanged argument.
    val parentTick = state.parentTick
    Log.i(TAG, "FeedScreen composed, parentTick=$parentTick")

    Column(modifier = modifier) {
        Button(
            modifier = Modifier.padding(8.dp),
            onClick = { state.parentTick++ },
        ) { Text("Recompose parent ($parentTick)") }

        FeedContent(
            // A new Modifier on each parent recomposition, so FeedContent does not skip: it runs
            // with "Different" for `modifier` and "Same" for every other argument, and its
            // restart lambda captures those flags.
            modifier = Modifier.padding(top = (parentTick % 2).dp),
            state = state,
            headerState = headerState,
            listState = listState,
            snackbarHostState = snackbarHostState,
            nestedScrollConnection = nestedScrollConnection,
            onCardTap = onCardTap,
            onHighlightTap = onHighlightTap,
            onPromoTap = onPromoTap,
            onCardLongPress = onCardLongPress,
            onSectionTitleTap = onSectionTitleTap,
            onSeeAllTap = onSeeAllTap,
            onChipSelect = onChipSelect,
            onRecentOpen = onRecentOpen,
            onRecentShare = onRecentShare,
            onRecentPin = onRecentPin,
            onRecentRemove = onRecentRemove,
            onRecentReport = onRecentReport,
            onRecentClear = onRecentClear,
            onRecentRefresh = onRecentRefresh,
            onTagTap = onTagTap,
            onAuthorTap = onAuthorTap,
            onHelpTap = onHelpTap,
            onTipTap = onTipTap,
            onTipDismiss = onTipDismiss,
            onOpenLink = onOpenLink,
        )
    }
}
