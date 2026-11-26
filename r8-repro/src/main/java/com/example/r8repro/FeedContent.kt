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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * A composable with 26 parameters, so the Compose compiler generates three changed-flag ints
 * (`$changed`, `$changed1`, `$changed2`) plus `$default`. Its restart lambda captures all of
 * them. R8 9.5.29 stores the captured `$changed2` in a **byte** field.
 */
@Composable
fun FeedContent(
    state: FeedState,
    topBarState: TopBarState,
    lazyListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    nestedScrollConnection: NestedScrollConnection? = null,
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
    val rowState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val onBannerAction = rememberBannerAction(
        state = state,
        scope = scope,
        onOpenLink = onOpenLink,
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            Button(
                modifier = Modifier.testTag("restartChild"),
                onClick = { state.childTick++ },
            ) { Text("Restart child ($childTick)") }
            Button(
                modifier = Modifier.padding(start = 8.dp).testTag("banner"),
                onClick = onBannerAction,
            ) { Text("Banner") }
        }
        Text(
            text = if (topBarState.collapsed) "collapsed" else "expanded",
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxWidth()) {
            items(state.sections, key = { it.id }, contentType = { "section" }) { section ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = section.name,
                        modifier = Modifier.clickable {
                            onMoreClick(section)
                            onTabSelect(0, section)
                            onMoreCardClick(0, section)
                        },
                    )
                    LazyRow(state = if (section.id == 0) rowState else rememberLazyListState()) {
                        items(section.items, key = { it.id }, contentType = { "item" }) { item ->
                            Text(
                                text = item.title,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        onItemClick(item, item.id, section, section.items)
                                        onItemPreviewClick(item, item.id, section)
                                        onHistoryInfoClick.onClick(item, item.id)
                                        onHistoryImageClick.onClick(item, item.id)
                                        onHistoryDetailClick.onClick(item, item.id)
                                        onHistoryDeleteClick.onClick(item, item.id)
                                        onHistoryMenuClick.onClick(item, item.id)
                                        onSpotlightClick.onClick(section, item.id)
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
                        onHistoryMoreClick()
                        onHistoryMoreCardClick()
                        onTopicClick(IndexedValue(0, Topic("t")))
                        onCategoryClick(IndexedValue(0, Category("c")))
                        onBannerClick.onClick(BANNER_URL)
                        onDailyTipClick()
                        onTutorialBannerClick()
                        onTutorialBannerDismiss()
                        runCatching { uriHandler.openUri(BANNER_URL) }
                        Log.i(TAG, "footer clicked in ${context.packageName}")
                    },
                )
            }
        }
    }
}
