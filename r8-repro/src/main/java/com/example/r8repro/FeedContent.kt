package com.example.r8repro

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

/**
 * A composable with 26 parameters, so the Compose compiler passes three changed-flag ints
 * (`$changed`, `$changed1`, `$changed2`) plus `$default`. Its restart lambda captures them.
 * R8 9.5.29 stores the captured `$changed1` in a **byte** field.
 *
 * `onOpenLink` is deliberately parameter 16 (zero-based), so its flag bits sit in `$changed1`
 * above bit 7.
 */
@Composable
fun FeedContent(
    state: FeedState,
    headerState: HeaderState,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null,
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
    onOpenLink: (url: String, title: String) -> Unit,
    onRecentClear: () -> Unit,
    onRecentRefresh: () -> Unit,
    onTagTap: (IndexedValue<Tag>) -> Unit,
    onAuthorTap: (IndexedValue<Author>) -> Unit,
    onHighlightTap: OnHighlightTap,
    onPromoTap: OnPromoTap,
    onHelpTap: () -> Unit,
    onTipTap: () -> Unit,
    onTipDismiss: () -> Unit,
) {
    val rowState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val onBannerAction = rememberBannerAction(
        state = state,
        scope = scope,
        onOpenLink = onOpenLink,
    )

    // State read that only this composable observes: changing it restarts FeedContent through
    // its restart lambda, without recomposing FeedScreen.
    val childTick = state.childTick
    Log.i(TAG, "FeedContent composed, childTick=$childTick")

    Column(
        modifier = modifier.then(
            if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier,
        ),
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            Button(onClick = { state.childTick++ }) { Text("Restart child ($childTick)") }
            Button(
                modifier = Modifier.padding(start = 8.dp),
                onClick = onBannerAction,
            ) { Text("Banner") }
        }
        Text(text = headerState.title, modifier = Modifier.padding(horizontal = 8.dp))
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            items(state.sections, key = { it.id }, contentType = { "section" }) { section ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = section.name,
                        modifier = Modifier.clickable {
                            onSectionTitleTap(section)
                            onChipSelect(0, section)
                            onSeeAllTap(0, section)
                            snackbarHostState.currentSnackbarData?.dismiss()
                        },
                    )
                    LazyRow(state = if (section.id == 0) rowState else rememberLazyListState()) {
                        items(section.items, key = { it.id }, contentType = { "item" }) { item ->
                            Text(
                                text = item.title,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        onCardTap(item, item.id, section, section.items)
                                        onCardLongPress(item, item.id, section)
                                        onRecentOpen.onTap(item, item.id)
                                        onRecentShare.onTap(item, item.id)
                                        onRecentPin.onTap(item, item.id)
                                        onRecentRemove.onTap(item, item.id)
                                        onRecentReport.onTap(item, item.id)
                                        onHighlightTap.onTap(section, item.id)
                                    },
                            )
                        }
                    }
                }
            }
            item(key = "footer", contentType = "footer") {
                Text(
                    text = "Footer",
                    modifier = Modifier.clickable {
                        onRecentClear()
                        onRecentRefresh()
                        onTagTap(IndexedValue(0, Tag("t")))
                        onAuthorTap(IndexedValue(0, Author("a")))
                        onPromoTap.onTap(BANNER_URL)
                        onHelpTap()
                        onTipTap()
                        onTipDismiss()
                        runCatching { uriHandler.openUri(BANNER_URL) }
                        Log.i(TAG, "footer tapped in ${context.packageName}")
                    },
                )
            }
        }
    }
}
