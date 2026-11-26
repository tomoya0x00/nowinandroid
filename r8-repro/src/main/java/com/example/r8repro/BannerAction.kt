package com.example.r8repro

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val BANNER_URL = "https://example.com/about"

const val TAG = "R8Repro"

/**
 * Keyed `remember` followed by an unkeyed one (inside [rememberTapCounter]).
 *
 * With correct flags, the keys never change after the first composition, so the "computed"
 * log line appears once. With the miscompiled flags it appears again on restart, and the
 * unkeyed remember afterwards can read the wrong slot.
 */
@Composable
fun rememberBannerAction(
    state: FeedState,
    scope: CoroutineScope,
    onOpenLink: (url: String, title: String) -> Unit,
): () -> Unit {
    val title = stringResource(R.string.banner_title)
    val recordOpen: () -> Unit = remember(state, scope) {
        { scope.launch { state.openEvents.emit(Unit) } }
    }
    return remember(recordOpen, onOpenLink, title) {
        Log.i(TAG, "rememberBannerAction: keyed remember computed")
        val action: () -> Unit = {
            recordOpen()
            onOpenLink(BANNER_URL, title)
        }
        action
    }.rememberTapCounter()
}
