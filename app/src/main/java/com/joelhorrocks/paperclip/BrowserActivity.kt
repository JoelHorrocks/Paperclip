package com.joelhorrocks.paperclip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.Container
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import dagger.hilt.android.AndroidEntryPoint
import org.mozilla.geckoview.GeckoView
import kotlin.math.roundToInt

enum class DragAnchors {
    Start,
    End,
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val browserViewModel: BrowserViewModel by viewModels()

        enableEdgeToEdge()
        setContent {
            PaperclipTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(bottom = 52.dp)
                                .fillMaxHeight()
                        ) {
                            Box {
                                BrowserScreen(browserViewModel)
                            }
                            BackHandler {
                                browserViewModel.goBack()
                            }
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            NavBarContainer(browserViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavBarContainer(browserViewModel: BrowserViewModel) {
    val density = LocalDensity.current
    val anchoredDraggableState = remember { AnchoredDraggableState(
        initialValue = DragAnchors.Start,
        anchors = DraggableAnchors {
            DragAnchors.Start at (348).dp.toPx()
            DragAnchors.End at 0f
        }
    ) }
    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    0,
                    anchoredDraggableState.requireOffset().roundToInt()
                )
            }
            .anchoredDraggable(anchoredDraggableState, Orientation.Vertical)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
    ) {
        NavBar(browserViewModel)
        Box(modifier = Modifier.height(348.dp).padding(top = WindowInsets.navigationBars.getBottom(density).toDp(), start = 4.dp, end = 4.dp)) {
            val tabs = browserViewModel.uiState.collectAsStateWithLifecycle().value.tabs
            LazyHorizontalGrid(rows = GridCells.Fixed(2)) {
                items(tabs.size) { index ->
                    val tab = tabs[index]
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = BorderStroke(1.dp, Color.Black),
                        modifier = Modifier.width(180.dp).padding(4.dp).clickable {
                            browserViewModel.selectTab(index)
                        }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.background(
                                    if(browserViewModel.uiState.collectAsStateWithLifecycle().value.currentTabIndex == index)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ).fillMaxWidth().padding(8.dp)
                            ) {
                                Text(
                                    text = tab.currentUrl,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        browserViewModel.closeTab(index)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.Black,
                                thickness = 1.dp,
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Tab Screenshot Icon",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(48.dp)
                                        .align(Alignment.Center),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBar(browserViewModel: BrowserViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(52.dp)
            .padding(4.dp)
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        val url = browserViewModel.uiState.collectAsStateWithLifecycle().value.currentUrl
        val interactionSource = remember { MutableInteractionSource() }
        val focusManager = LocalFocusManager.current
        BasicTextField(
            value = url,
            onValueChange = {
                browserViewModel.updateUrl(it)
            },
            modifier = Modifier
                .height(40.dp)
                .weight(1f),
            interactionSource = interactionSource,
            singleLine = true,
            keyboardActions = KeyboardActions(
                onGo = {
                    browserViewModel.submitUrl()
                    focusManager.clearFocus()
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            )
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = url,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                    top = 0.dp,
                    bottom = 0.dp,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "NavBar Icon"
                    )
                },
                placeholder = {
                    Text("Search or enter address")
                },
                container = {
                    Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = TextFieldDefaults.colors(),
                        shape = RoundedCornerShape(32.dp),
                    )
                }
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        var moreMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = { moreMenuExpanded = !moreMenuExpanded },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }
            DropdownMenu(
                expanded = moreMenuExpanded,
                onDismissRequest = { moreMenuExpanded = false }
            ) {
                Row {
                    IconButton(
                        onClick = {
                            browserViewModel.goBack()
                            moreMenuExpanded = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("New Tab") },
                    onClick = {
                        browserViewModel.createTab()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun BrowserScreen(browserViewModel: BrowserViewModel) {
    val state by browserViewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        state.currentTab?.geckoSession?.let { geckoSession ->
            AndroidView(
                factory = {
                    GeckoView(it)
                },
                update = {
                    if (it.session != geckoSession) {
                        it.setSession(geckoSession)
                    }
                }
            )
        } ?: run {
            return@Box
        }
    }
}