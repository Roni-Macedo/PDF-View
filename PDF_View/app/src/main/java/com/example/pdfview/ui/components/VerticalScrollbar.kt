package com.example.pdfview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    currentPage: Int,
    pageCount: Int
) {
    val layoutInfo = listState.layoutInfo
    val viewportHeight = layoutInfo.viewportSize.height

    val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return
    val totalContentHeight = itemSize * layoutInfo.totalItemsCount

    if (totalContentHeight <= viewportHeight) return

    val scrollOffset by remember(listState, itemSize) {
        derivedStateOf {
            listState.firstVisibleItemIndex * itemSize +
                    listState.firstVisibleItemScrollOffset
        }
    }

    val maxScroll = totalContentHeight - viewportHeight
    val progress = (scrollOffset.toFloat() / maxScroll).coerceIn(0f, 1f)

    val dotSize = 50.dp
    val dotWidth = 64.dp
    val dotHeight = 34.dp
    val dotSizePx = with(LocalDensity.current) { dotSize.toPx() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(50.dp)
    ) {
        Box(
            modifier = Modifier
                .size(dotWidth, dotHeight)
                .offset {
                    IntOffset(
                        0,
                        ((viewportHeight - dotSizePx) * progress).toInt()
                    )
                }
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = Color.Gray.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = "$currentPage / $pageCount",
                fontSize = 12.sp,
                color = Color.DarkGray.copy(0.7f)
            )
        }
    }
}