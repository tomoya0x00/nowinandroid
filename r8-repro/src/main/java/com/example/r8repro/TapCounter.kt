package com.example.r8repro

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Counts taps before forwarding them. */
interface TapCounter {
    fun forward(onTap: () -> Unit)

    companion object {
        fun create(): TapCounter = TapCounterImpl()
    }
}

private class TapCounterImpl : TapCounter {
    private var taps = 0

    override fun forward(onTap: () -> Unit) {
        taps += 1
        Log.i(TAG, "tap #$taps")
        onTap()
    }
}

/**
 * Unkeyed `remember`. When the slot table is shifted by one, this reads the value that the
 * preceding keyed `remember` stored (a lambda) and the cast to TapCounterImpl fails.
 */
@Composable
fun (() -> Unit).rememberTapCounter(): () -> Unit {
    val counter = remember { TapCounter.create() }
    return { counter.forward(this) }
}
