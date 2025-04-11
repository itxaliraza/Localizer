package languages_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_checked_box
import com.example.localizer.generated.resources.ic_unchecked_box
import common_components.EditText
import common_components.HorizontalSpacer
import domain.model.LanguageModel
import home_screen.HomeScreenState
import home_screen.HomeScreenViewModel
import home_screen.components.RectangleWithShadow
import home_screen.components.RoundedCard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import theme.PrimaryColor
import theme.ScreenColor

@Composable
fun LanguagesScreen(
    modifier: Modifier = Modifier,
    state: HomeScreenState,
    viewModel: HomeScreenViewModel
) {
    val scrollState = rememberLazyGridState()
    LaunchedEffect(state.searchedText) {
        if (state.searchedText.isEmpty()) {
            scrollState.scrollToItem(0)
        }
    }
    Column(modifier = modifier) {
        RectangleWithShadow(
            bgColor = PrimaryColor,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            EditText(
                value = state.searchedText,
                modifier = Modifier.padding(7.dp)
            ) {
                viewModel.searchLanguage(it)
            }
        }
        RectangleWithShadow(
            bgColor = PrimaryColor,
            modifier = Modifier.fillMaxSize().padding(top = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                RectangleWithShadow(
                    bgColor = ScreenColor,
                    modifier = Modifier.fillMaxWidth().padding(5.dp).wrapContentHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    ) {
                        val isSelected =
                            state.selectedLanguages.size == state.availableLanguages.size
                        RoundedCard(
                            modifier = Modifier.weight(1f),
                            bgColor = PrimaryColor
                        ) {
                            Text(
                                text = buildString {
                                    append("Selected: ")
                                    append(state.selectedLanguages.size)
                                    append("/")
                                    append(state.availableLanguages.size)
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
                            )
                        }
                        RoundedCard(
                            modifier = Modifier.weight(1f),
                            bgColor = PrimaryColor, onClick = {
                                viewModel.updateSelectedLanguages(null, true)
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(if (isSelected) Res.drawable.ic_checked_box else Res.drawable.ic_unchecked_box),
                                    contentDescription = "",
                                    colorFilter = ColorFilter.tint(Color.White),
                                    modifier = Modifier.size(16.dp)
                                )
                                HorizontalSpacer()
                                Text(
                                    text = if (isSelected) "Unselect All" else "Select All",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                LazyVerticalGridWithScrollIndicator(scrollState, state, viewModel)
            }
        }
    }
}

@Composable
private fun LanguagesItems(
    modifier: Modifier = Modifier,
    selectAll: Boolean = false,
    isSelected: Boolean = false,
    bgColor: Color = ScreenColor,
    model: LanguageModel,
    onClick: (LanguageModel) -> Unit
) {
    RoundedCard(
        bgColor = bgColor, strokeWidth = if (isSelected || selectAll) 1 else 0,
        onClick = { onClick(model) },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(vertical = 5.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(if (isSelected || selectAll) Res.drawable.ic_checked_box else Res.drawable.ic_unchecked_box),
                contentDescription = "",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(16.dp)
            )
            HorizontalSpacer()
            Text(
                text = model.langName,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().basicMarquee().weight(1f),
            )
        }
    }
}

@Composable
fun ColumnScope.LazyVerticalGridWithScrollIndicator(
    scrollState: LazyGridState,
    state: HomeScreenState,
    viewModel: HomeScreenViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var scrollbarY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxSize().weight(1f)) {

        // GRID CONTENT
        LazyVerticalGrid(
            columns = GridCells.Adaptive(145.dp),
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            items(state.filteredList, key = { it.langCode }) {
                LanguagesItems(
                    model = it,
                    isSelected = state.selectedLanguages.contains(it),
                    selectAll = state.selectedLanguages.size == state.availableLanguages.size
                ) { model ->
                    viewModel.updateSelectedLanguages(model)
                }
            }
        }

        // SCROLLBAR LOGIC
        val layoutInfo = scrollState.layoutInfo
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()

        if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
            val firstItem = visibleItemsInfo.first()
            val averageItemHeight =
                visibleItemsInfo.map { it.size.height }.average().toFloat().coerceAtLeast(1f)

            val totalContentHeight = totalItemsCount * averageItemHeight
            val scrollOffset =
                (firstItem.index * averageItemHeight - firstItem.offset.y).coerceAtLeast(0f)
            val maxScrollOffset = (totalContentHeight - viewportHeight).coerceAtLeast(1f)

            // Calculate scroll progress
            val scrollProgress = (scrollOffset / maxScrollOffset).coerceIn(0f, 1f)

            // Dynamic scrollbar height
            val proportionVisible = viewportHeight / totalContentHeight
            val scrollbarHeight = (viewportHeight * proportionVisible).coerceIn(50f, viewportHeight)

            val trackHeight = viewportHeight - scrollbarHeight
            scrollbarY = if (!isDragging) {
                (scrollProgress * trackHeight).coerceIn(0f, trackHeight)
            } else scrollbarY

            // SCROLL TRACK
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            ) {
                // SCROLL THUMB
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { scrollbarHeight.toDp() })
                        .offset { IntOffset(0, scrollbarY.toInt()) }
                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false },
                                onDrag = { _, dragAmount ->
                                    scrollbarY = (scrollbarY + dragAmount.y)
                                        .coerceIn(0f, trackHeight)

                                    val newScrollProgress = scrollbarY / trackHeight
                                    val targetScrollOffset = newScrollProgress * maxScrollOffset
                                    val targetIndex =
                                        (targetScrollOffset / averageItemHeight).toInt()

                                    coroutineScope.launch {
                                        scrollState.scrollToItem(targetIndex)
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}








