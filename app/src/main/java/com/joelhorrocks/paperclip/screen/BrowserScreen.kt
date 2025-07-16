package com.joelhorrocks.paperclip.screen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.Container
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelhorrocks.paperclip.ArticleLoadingState
import com.joelhorrocks.paperclip.BrowserViewModel
import com.joelhorrocks.paperclip.HOME_URL
import com.joelhorrocks.paperclip.R
import com.joelhorrocks.paperclip.Screen
import com.joelhorrocks.paperclip.TabController
import com.joelhorrocks.paperclip.model.Prompt
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.news.Article
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import kotlin.math.roundToInt

enum class DragAnchors {
    Start,
    End,
}

@Composable
fun BrowserScreen(browserViewModel: BrowserViewModel, navigate: (screen: Screen) -> Unit) {
    PaperclipTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            val state by browserViewModel.uiState.collectAsStateWithLifecycle()
            val focusManager = LocalFocusManager.current
            val webPromptQueue = remember { mutableStateListOf<Prompt?>() }
            LaunchedEffect(Unit) {
                browserViewModel.prompts.collect {
                    webPromptQueue.add(it)
                }
            }
            LaunchedEffect(Unit) {
                browserViewModel.fetchArticles()
            }
            // TODO: cap number of max prompts at once
            // TODO: handle prompts from background tab? switch tab?
            for(prompt in webPromptQueue.reversed()) {
                when(prompt) {
                    is Prompt.Alert -> {
                        WebAlertPrompt(
                            { webPromptQueue.remove(prompt) },
                            // TODO: handle null
                            prompt.title!!,
                            prompt.message!!,
                            Icons.Default.Web
                        )
                    }
                    is Prompt.Button -> {
                        WebButtonPrompt(
                            { webPromptQueue.remove(prompt); prompt.onAction(it) },
                            prompt.title!!,
                            prompt.message!!,
                            Icons.Default.Web
                        )
                    }
                    else -> {}
                }
            }
            Box(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                        focusManager.clearFocus()
                    }
            ) {
                Box(
                    modifier = Modifier.padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 52.dp)
                            .fillMaxHeight()
                    ) {
                        Box {
                            val articleLoadingState = browserViewModel.uiState.collectAsStateWithLifecycle().value.articleLoadingState
                            val articleList = browserViewModel.uiState.collectAsStateWithLifecycle().value.articleList
                            when (state.currentUrl) {
                                HOME_URL -> HomeScreen(navigate, loadUrl = { browserViewModel.loadUrl(it) }, articleLoadingState, articleList)
                                else -> BrowserScreen(state.currentTab?.geckoSession)
                            }
                        }
                        BackHandler {
                            browserViewModel.goBack()
                        }
                    }
                }
                val layoutDirection = LocalLayoutDirection.current
                Box(
                    modifier = Modifier.padding(
                        bottom = innerPadding.calculateBottomPadding(),
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection)
                    )
                ) {
                    NavBarContainer(
                        state.currentTabIndex,
                        state.tabs,
                        state.navBarText,
                        state.showToolbarTooltip,
                        navigate,
                        setShowToolbarTooltip = { shown ->
                            browserViewModel.setShowToolbarTooltip(shown)
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

// TODO: move prompts to own file
@Composable
fun WebAlertPrompt(
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = { }
    )
}

@Composable
fun WebButtonPrompt(
    onAction: (confirm: Boolean) -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        // TODO: should we handle onDismissRequest and return a dismiss in GeckoView?
        onDismissRequest = { },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction(true)
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onAction(false)
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun NavBarContainer(
    currentTabIndex: Int?,
    tabs: List<Tab>,
    navBarText: String,
    showToolbarTooltip: Boolean,
    navigate: (screen: Screen) -> Unit,
    setShowToolbarTooltip: (shown: Boolean) -> Unit,
    updateUrl: (url: String) -> Unit,
    submitUrl: () -> Unit,
    goBack: () -> Unit,
    createTab: () -> Unit,
    selectTab: (index: Int) -> Unit,
    closeTab: (index: Int) -> Unit
) {
    // TODO: fix white gap when drawer open
    val heightPx = with(LocalDensity.current) { 348.dp.toPx() }
    val scope = rememberCoroutineScope()
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Start,
            anchors = DraggableAnchors {
                DragAnchors.Start at heightPx
                DragAnchors.End at 0f
            }
        )
    }
    if (showToolbarTooltip) {
        LaunchedEffect(anchoredDraggableState) {
            snapshotFlow { anchoredDraggableState.currentValue == DragAnchors.Start }
                .distinctUntilChanged()
                .collect {
                    setShowToolbarTooltip(it)
                }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (anchoredDraggableState.offset < heightPx) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1 - anchoredDraggableState.offset / heightPx) / 2))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            anchoredDraggableState.animateTo(DragAnchors.Start)
                        }
                    }
            )
        }
        Column(
            modifier = Modifier
                // TODO: rounded top corners once navbar hiding when scrolling is done
                .align(Alignment.BottomCenter)
                .offset {
                    IntOffset(
                        0,
                        anchoredDraggableState.requireOffset().roundToInt()
                    )
                }
        ) {
            // TODO: move this to be fixed on homepage?
            // TODO: only show on first swipe
            if (showToolbarTooltip) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha =
                                ((anchoredDraggableState.offset / heightPx) * 2 - 1).coerceAtLeast(
                                    0f
                                )
                        }
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ArrowDropUp, null)
                        // TODO: blocks navbar
                        Text(stringResource(R.string.swipe_up_on_toolbar_for_tabs))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Vertical
                    )
            ) {
                // TODO: swipe left and right to switch tab
                NavBar(navBarText, updateUrl, submitUrl, goBack, createTab, navigate, collapseDrawer = {
                    scope.launch {
                        anchoredDraggableState.animateTo(DragAnchors.Start)
                    }
                })
                Box(
                    modifier = Modifier
                        .height(348.dp)
                        .padding(start = 4.dp, end = 4.dp)
                        .graphicsLayer {
                            alpha =
                                ((1 - (anchoredDraggableState.offset / heightPx)).coerceAtMost(0.2f) / 0.2f)
                        }) {
                    // TODO: drag and drop tabs, tab folders
                    LazyHorizontalGrid(rows = GridCells.Fixed(2)) {
                        items(tabs.size) { index ->
                            val tab = tabs[index]
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                border = BorderStroke(1.dp, Color.Black),
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(4.dp)
                                    .clip(CardDefaults.shape)
                                    .clickable {
                                        selectTab(index)
                                        scope.launch {
                                            anchoredDraggableState.animateTo(DragAnchors.Start)
                                        }
                                    }
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .background(
                                                if (currentTabIndex == index)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else
                                                    MaterialTheme.colorScheme.surface
                                            )
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // TODO: page URL
                                        Text(
                                            text = if (tab.currentUrl == "about:home") stringResource(
                                                R.string.homepage
                                            ) else tab.currentUrl,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 16.sp,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                closeTab(index)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.close),
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavBar(
    navBarText: String,
    updateUrl: (url: String) -> Unit,
    submitUrl: () -> Unit,
    goBack: () -> Unit,
    createTab: () -> Unit,
    navigate: (screen: Screen) -> Unit,
    collapseDrawer: () -> Unit
) {
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
            value = navBarText,
            onValueChange = {
                // TODO: rename? 'url' was renamed to 'navBarText' as text in navbar may not be an URL and to avoid confusion with current tab URL
                updateUrl(it)
            },
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            interactionSource = interactionSource,
            singleLine = true,
            keyboardActions = KeyboardActions(
                onGo = {
                    submitUrl()
                    focusManager.clearFocus()
                    collapseDrawer()
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            )
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = navBarText,
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
                    Text(stringResource(R.string.search_or_enter_address))
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
                onClick = { moreMenuExpanded = !moreMenuExpanded }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.menu)
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
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_tab)) },
                    onClick = {
                        moreMenuExpanded = false
                        createTab()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_tab)
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        moreMenuExpanded = false
                        navigate(Screen.Settings)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(navigate: (screen: Screen) -> Unit, loadUrl: (url: String) -> Unit, articleLoadingState: ArticleLoadingState, articleList: List<Article>) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .rotate(45f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { }) {
                Icon(Icons.Default.DashboardCustomize, null)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Newsfeed(navigate, loadUrl, articleLoadingState, articleList)
        // TODO: landscape mode issue
        ShortcutsRow(loadUrl)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// TODO: horizontal scrolling? rethink layout for bottom stacked UI
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Newsfeed(navigate: (screen: Screen) -> Unit, loadUrl: (url: String) -> Unit, articleLoadingState: ArticleLoadingState, articleList: List<Article>) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            stringResource(R.string.news),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        when(articleLoadingState) {
            ArticleLoadingState.LOADING -> {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(156.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                    }
                }
            }
            ArticleLoadingState.SUCCESS -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(articleList) {
                        NewsCard(it, loadUrl)
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            // TODO: left-handed mode, consider LTR/RTL layout
                            TextButton(onClick = {
                                navigate(Screen.Newsfeed)
                            }) {
                                Text("See more")
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "",
                                )
                            }
                        }
                    }
                }
            }
            ArticleLoadingState.ERROR -> {}
        }
    }
}

@Composable
fun NewsCard(article: Article, loadUrl: (url: String) -> Unit) {
    OutlinedCard (
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .clickable {
                loadUrl(article.url)
            }
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(112.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                Row {
                    Icon(
                        Icons.Default.Newspaper,
                        null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        article.headline,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    article.description,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${stringResource(R.string.today)} · ${
                            stringResource(
                                R.string.read_time, article.readTimeMin
                            )
                        }"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            null
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { }) {
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

// TODO: database
@Composable
fun ShortcutsRow(loadUrl: (url: String) -> Unit) {
    Column {
        Text(
            stringResource(R.string.shortcuts),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(5) {
                Shortcut(Icons.Default.Web, stringResource(R.string.example)) {
                    loadUrl("about:buildconfig")
                }
            }
            item {
                Shortcut(Icons.Default.Add, "") {}
            }
        }
    }
}

@Composable
fun Shortcut(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val colorScheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .size(84.dp)
                .drawBehind {
                    drawCircle(
                        colorScheme.primaryContainer
                    )
                }
                .clip(CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
        )
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