package home_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localizer.generated.resources.Res
import com.example.localizer.generated.resources.ic_checked_box
import com.example.localizer.generated.resources.ic_unchecked_box
import common_components.EditText
import common_components.HorizontalSpacer
import common_components.VerticalSpacer
import domain.model.LanguageModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import theme.PrimaryColor
import theme.ScreenColor

@Composable
fun HomeScreenNew(viewModel: HomeScreenViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    var selectedFile by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberLazyGridState()
    var selectAll by remember { mutableStateOf(false) }
    LaunchedEffect(state.searchedText) {
        if (state.searchedText.isEmpty()) {
            scrollState.scrollToItem(0)
        }
    }
    LaunchedEffect(selectAll) {
        viewModel.updateSelectedLanguages(null, selectAll)
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
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(145.dp),
                            state = scrollState, modifier = Modifier.fillMaxSize().weight(1f)
                        ) {
                            items(state.filteredList, key = { it.langCode }) {
                                LanguagesItems(model = it, isSelected = state.selectedLanguagesList.contains(it)) { model ->
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun LanguagesItems(
    modifier: Modifier = Modifier,
    selectAll: Boolean = false,
    isSelected: Boolean = false,
    model: LanguageModel,
    onClick: (LanguageModel) -> Unit
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(7.dp),
        backgroundColor = ScreenColor,
        onClick = { onClick(model) },
        modifier = modifier.fillMaxWidth().padding(5.dp).height(40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
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