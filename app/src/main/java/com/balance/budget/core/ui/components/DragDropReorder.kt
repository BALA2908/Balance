package com.balance.budget.core.ui.components

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Minimal long-press drag-to-reorder state for a [LazyListState] — no external
 * library. Height-agnostic: it resolves the hovered row from the list's live
 * [LazyListState.layoutInfo] rather than assuming a fixed row height. The caller
 * keeps the visual order in a local list and reorders it on [onMove]; commit the
 * final order on drag end.
 */
class DragDropState internal constructor(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var initialOffset = 0
    private var dragDelta by mutableFloatStateOf(0f)

    private val draggingLayoutInfo: LazyListItemInfo?
        get() = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == draggingItemIndex }

    /** Pixel offset to visually translate the dragged row by while it floats. */
    val draggingItemOffset: Float
        get() = draggingLayoutInfo?.let { (initialOffset + dragDelta) - it.offset } ?: 0f

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.y.toInt() in it.offset..(it.offset + it.size) }
            ?.also {
                draggingItemIndex = it.index
                initialOffset = it.offset
                dragDelta = 0f
            }
    }

    fun onDrag(dragAmount: Offset) {
        val from = draggingItemIndex ?: return
        dragDelta += dragAmount.y
        val current = draggingLayoutInfo ?: return
        val middle = current.offset + dragDelta + current.size / 2f
        val target = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            item.index != from && middle.toInt() in item.offset..(item.offset + item.size)
        } ?: return
        onMove(from, target.index)
        draggingItemIndex = target.index
        dragDelta += current.offset - target.offset
    }

    fun onDragEnd() {
        draggingItemIndex = null
        dragDelta = 0f
        initialOffset = 0
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit,
): DragDropState = remember(lazyListState) { DragDropState(lazyListState, onMove) }
