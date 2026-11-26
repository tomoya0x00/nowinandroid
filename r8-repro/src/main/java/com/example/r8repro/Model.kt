package com.example.r8repro

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow

@Immutable
data class FeedItem(val id: Int, val title: String)

@Immutable
data class FeedSection(val id: Int, val name: String, val items: List<FeedItem>)

@Immutable
data class Tag(val name: String)

@Immutable
data class Author(val name: String)

fun interface OnRecentTap {
    fun onTap(item: FeedItem, position: Int)
}

fun interface OnHighlightTap {
    fun onTap(section: FeedSection, position: Int)
}

fun interface OnPromoTap {
    fun onTap(url: String)
}

/** Screen state holder. */
@Stable
class FeedState {
    val sections: List<FeedSection> = List(3) { s ->
        FeedSection(s, "Section $s", List(4) { i -> FeedItem(s * 10 + i, "Item $s-$i") })
    }

    /** Read only inside [FeedContent]: bumping it restarts FeedContent alone. */
    var childTick by mutableIntStateOf(0)

    /** Read only inside [FeedScreen]: bumping it recomposes the parent. */
    var parentTick by mutableIntStateOf(0)

    val openEvents = MutableSharedFlow<Unit>()
}

@Stable
interface HeaderState {
    val title: String
}

class FixedHeaderState(override val title: String) : HeaderState
