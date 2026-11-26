package com.example.r8repro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Drops taps that arrive within [windowMillis] of the previous accepted tap. */
interface ClickGuard {
    fun process(onClick: () -> Unit)

    companion object {
        fun create(windowMillis: Long = 1000L): ClickGuard = ClickGuardImpl(windowMillis)
    }
}

private class ClickGuardImpl(private val windowMillis: Long) : ClickGuard {
    private var lastAcceptedMs = 0L

    override fun process(onClick: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastAcceptedMs > windowMillis) {
            lastAcceptedMs = now
            onClick()
        }
    }
}

/**
 * Unkeyed `remember`. When the slot table is shifted by one, this reads the value that the
 * preceding keyed `remember` stored (a lambda) and the cast to ClickGuardImpl fails.
 */
@Composable
fun (() -> Unit).rememberClickGuard(): () -> Unit {
    val guard = remember { ClickGuard.create() }
    return { guard.process(this) }
}

@Composable
fun MutableSharedFlow<Unit>.rememberEmitter(scope: CoroutineScope): () -> Unit =
    remember(this, scope) { { scope.launch { emit(Unit) } } }
