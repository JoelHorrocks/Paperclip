package com.joelhorrocks.paperclip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.flow.distinctUntilChanged
import org.mozilla.geckoview.GeckoSession
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
                    val state by browserViewModel.uiState.collectAsStateWithLifecycle()
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(bottom = 52.dp)
                                .fillMaxHeight()
                        ) {
                            Box {
                                Content(state.currentTab?.geckoSession, state.currentUrl)
                            }
                            BackHandler {
                                browserViewModel.goBack()
                            }
                        }
                        var drawerOpen by remember { mutableFloatStateOf(0f) }
                        // TODO: tap background to close drawer
                        if(drawerOpen != 0f) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(0xff000000).copy(alpha = drawerOpen / 2))
                            )
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            NavBarContainer(
                                state.currentTabIndex,
                                state.tabs,
                                state.navBarText,
                                updateDrawerPercentage = { state ->
                                    drawerOpen = state
                                },
                                updateUrl = { url ->
                                    browserViewModel.updateUrl(url)
                                },
                                submitUrl = {
                                    browserViewModel.submitUrl()
                                },
                                goBack = {
                                    browserViewModel.goBack()
                                },
                                createTab = {
                                    browserViewModel.createTab()
                                },
                                selectTab = { index ->
                                    browserViewModel.selectTab(index)
                                },
                                closeTab = { index ->
                                    browserViewModel.closeTab(index)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavBarContainer(
    currentTabIndex: Int?,
    tabs: List<TabController.Tab>,
    url: String,
    updateDrawerPercentage: (state: Float) -> Unit,
    updateUrl: (url: String) -> Unit,
    submitUrl: () -> Unit,
    goBack: () -> Unit,
    createTab: () -> Unit,
    selectTab: (index: Int) -> Unit,
    closeTab: (index: Int) -> Unit
) {
    val heightPx = with(LocalDensity.current) { 348.dp.toPx() }
    val anchoredDraggableState = remember { AnchoredDraggableState(
        initialValue = DragAnchors.Start,
        anchors = DraggableAnchors {
            DragAnchors.Start at heightPx
            DragAnchors.End at 0f
        }
    ) }
    LaunchedEffect(anchoredDraggableState) {
        snapshotFlow { (1 - (anchoredDraggableState.offset / heightPx)) }
            .distinctUntilChanged()
            .collect { state ->
                updateDrawerPercentage(state)
            }
    }
    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    0,
                    anchoredDraggableState.requireOffset().roundToInt()
                )
            }
            .anchoredDraggable(
                state = anchoredDraggableState,
                orientation = Orientation.Vertical
            )
            // TODO: rounded top corners once navbar hiding when scrolling is done
            .background(MaterialTheme.colorScheme.surface)
    ) {
        NavBar(url, updateUrl, submitUrl, goBack, createTab)
        Box(modifier = Modifier.height(348.dp).padding(start = 4.dp, end = 4.dp).graphicsLayer { alpha = ((1 - (anchoredDraggableState.offset / heightPx)).coerceAtMost(0.2f) / 0.2f) }) {
            LazyHorizontalGrid(rows = GridCells.Fixed(2)) {
                items(tabs.size) { index ->
                    val tab = tabs[index]
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        border = BorderStroke(1.dp, Color.Black),
                        modifier = Modifier.width(180.dp).padding(4.dp).clip(CardDefaults.shape).clickable {
                            selectTab(index)
                        }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.background(
                                    if(currentTabIndex == index)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                ).fillMaxWidth().padding(8.dp)
                            ) {
                                Text(
                                    text = if(tab.currentUrl == "") "Homepage" else tab.currentUrl,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        closeTab(index)
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
fun NavBar(url: String, updateUrl: (url: String) -> Unit, submitUrl: () -> Unit, goBack: () -> Unit, createTab: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(52.dp)
            .padding(4.dp)
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        val interactionSource = remember { MutableInteractionSource() }
        val focusManager = LocalFocusManager.current
        BasicTextField(
            value = url,
            onValueChange = {
                updateUrl(it)
            },
            modifier = Modifier
                .height(40.dp)
                .weight(1f),
            interactionSource = interactionSource,
            singleLine = true,
            keyboardActions = KeyboardActions(
                onGo = {
                    submitUrl()
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
                            goBack()
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
                        createTab()
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
fun Content(geckoSession: GeckoSession?, currentUrl: String) {
    when(currentUrl) {
        "" -> HomeScreen()
        else -> BrowserScreen(geckoSession)
    }
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(32.dp).rotate(45f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Paperclip",
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        ShortcutsRow()
        Spacer(modifier = Modifier.height(16.dp))
        Newsfeed()
    }
}

@Composable
fun Newsfeed() {
    Text(
        "News",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(3) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp).height(96.dp)
                ) {
                    val colorScheme = MaterialTheme.colorScheme
                    Canvas(
                        modifier = Modifier.size(96.dp).align(Alignment.CenterVertically)
                    ) {
                        drawRoundRect(
                            colorScheme.primaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            "Headline",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Description",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Row {
                            Icon(
                                Icons.Default.AddCircleOutline,
                                null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Today · 2 min read")
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.Flag,
                                null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutsRow() {
    Text(
        "Shortcuts",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val colorScheme = MaterialTheme.colorScheme
                Canvas(
                    modifier = Modifier.size(84.dp)
                ) {
                    drawCircle(
                        colorScheme.primaryContainer
                    )
                }
                Text(
                    "Example",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun BrowserScreen(geckoSession: GeckoSession?) {
    Box(modifier = Modifier.fillMaxSize()) {
        geckoSession?.let { geckoSession ->
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