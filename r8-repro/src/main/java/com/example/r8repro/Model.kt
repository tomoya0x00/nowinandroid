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
data class Topic(val name: String)

@Immutable
data class Category(val name: String)

fun interface OnHistoryClick {
    fun onClick(item: FeedItem, position: Int)
}

fun interface OnSpotlightClick {
    fun onClick(section: FeedSection, position: Int)
}

fun interface OnBannerClick {
    fun onClick(url: String)
}

/** Screen state holder, standing in for a ViewModel. */
@Stable
class FeedState {
    val sections: List<FeedSection> = List(3) { s ->
        FeedSection(s, "Section $s", List(4) { i -> FeedItem(s * 10 + i, "Item $s-$i") })
    }

    /** Read only inside [FeedContent]: bumping it restarts FeedContent alone. */
    var childTick by mutableIntStateOf(0)

    /** Read only inside [FeedScreen]: bumping it recomposes the parent. */
    var parentTick by mutableIntStateOf(0)

    val onBannerShown = MutableSharedFlow<Unit>()
}

@Stable
interface TopBarState {
    val collapsed: Boolean
    fun collapse()
}

class TopBarStateImpl : TopBarState {
    private var _collapsed by mutableIntStateOf(0)
    override val collapsed: Boolean get() = _collapsed != 0
    override fun collapse() {
        _collapsed = 1
    }
}
