package home_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_checked_box
import com.example.localizer.generated.resources.ic_unchecked_box
import common_components.EditText
import common_components.HorizontalSpacer
import domain.model.LanguageModel
import home_screen.components.RoundedCard
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import theme.PrimaryColor
import theme.ScreenColor

@Composable
fun HomeScreenNew(viewModel: HomeScreenViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    var selectedFile by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberLazyGridState()
    LaunchedEffect(state.searchedText) {
        if (state.searchedText.isEmpty()) {
            scrollState.scrollToItem(0)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp).weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        .background(color = PrimaryColor, shape = RoundedCornerShape(10.dp))
                        .shadow(
                            elevation = 5.dp,
                            ambientColor = PrimaryColor,
                            spotColor = PrimaryColor
                        )
                ) {
                    EditText(
                        value = state.searchedText,
                        modifier = Modifier.padding(7.dp)
                    ) {
                        viewModel.searchLanguage(it)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 10.dp)
                        .background(color = PrimaryColor, shape = RoundedCornerShape(10.dp))
                        .shadow(
                            elevation = 5.dp,
                            ambientColor = PrimaryColor,
                            spotColor = PrimaryColor
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(5.dp).wrapContentHeight()
                                .background(color = ScreenColor, shape = RoundedCornerShape(10.dp))
                                .shadow(
                                    elevation = 5.dp,
                                    ambientColor = ScreenColor,
                                    spotColor = ScreenColor
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                            ) {
                                val isSelected = state.selectedLanguages.size == state.availableLanguages.size
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

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(145.dp),
                            state = scrollState, modifier = Modifier.fillMaxSize().weight(1f)
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
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().weight(0.8f)
            ) {

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
        bgColor = bgColor,
        onClick = { onClick(model) },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(5.dp),
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