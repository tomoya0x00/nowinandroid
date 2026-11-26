package com.example.r8repro

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope

const val BANNER_URL = "https://example.com/about"

/**
 * Keyed `remember` followed by an unkeyed one (inside [rememberClickGuard]).
 *
 * With correct flags, the keys never change after the first composition, so the "computed"
 * log line appears once. With the miscompiled flags it appears on every restart, and the
 * unkeyed remember afterwards can read the wrong slot.
 */
@Composable
fun rememberBannerAction(
    state: FeedState,
    scope: CoroutineScope,
    onOpenLink: (url: String, title: String) -> Unit,
): () -> Unit {
    val title = stringResource(R.string.banner_title)
    val markAsShown = state.onBannerShown.rememberEmitter(scope = scope)
    return remember(markAsShown, onOpenLink, title) {
        Log.i(TAG, "rememberBannerAction: keyed remember computed")
        val action: () -> Unit = {
            markAsShown()
            onOpenLink(BANNER_URL, title)
        }
        action
    }.rememberClickGuard()
}

const val TAG = "R8Repro"
