package com.joelhorrocks.paperclip.screen

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.More
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.Container
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joelhorrocks.paperclip.R
import com.joelhorrocks.paperclip.Screen
import com.joelhorrocks.paperclip.model.Tab
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(complete: () -> Unit) {
    // TODO: life logic to a viewmodel for this screen
    PaperclipTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            val pagerState = rememberPagerState(pageCount = {
                3
            })
            val coroutineScope = rememberCoroutineScope()
            // TODO: page indicator, allow scrolling unless tutorial has not been finished
            HorizontalPager(state = pagerState, modifier = Modifier.padding(innerPadding), userScrollEnabled = false) { page ->
                when(page) {
                    0 -> Intro(nextPage = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }, skipSetup = {
                        // TODO: set setup complete
                        // TODO: clear back, check home back override
                        complete()
                    })
                    // TODO: add swipe left/right on toolbar tutorial once added
                    1 -> Tutorial(nextPage = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                        lastPage = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        })
                    2 -> Customize(lastPage = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }, complete)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Intro(nextPage: () -> Unit, skipSetup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Icon(Icons.Default.AttachFile, null, modifier = Modifier
            .size(92.dp)
            .rotate(45f))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("A web browser designed for how you actually use your phone. Navigate with one hand, organize tabs effortlessly, and browse the way mobile was meant to be.")
        Spacer(modifier = Modifier.weight(1f))
        Row {
            val size = ButtonDefaults.MediumContainerHeight
            FilledTonalButton(
                onClick = {
                    skipSetup()
                },
                content = { Text("Skip setup", style = ButtonDefaults.textStyleFor(size)) },
                contentPadding = ButtonDefaults.contentPaddingFor(size),
                modifier = Modifier.heightIn(size)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    nextPage()
                },
                content = { Text("Next", style = ButtonDefaults.textStyleFor(size)) },
                contentPadding = ButtonDefaults.contentPaddingFor(size),
                modifier = Modifier.heightIn(size)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Tutorial(nextPage: () -> Unit, lastPage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            val size = ButtonDefaults.MediumContainerHeight
            FilledTonalButton(
                onClick = {
                    lastPage()
                },
                content = { Text("Back", style = ButtonDefaults.textStyleFor(size)) },
                contentPadding = ButtonDefaults.contentPaddingFor(size),
                modifier = Modifier.heightIn(size)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Paperclip uses gesture controls. Try swiping up on the toolbar to access your tab drawer.",
            modifier = Modifier
                .padding(horizontal = 8.dp))
        NavBarContainerTutorial(nextPage)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavBarContainerTutorial(nextPage: () -> Unit) {
    val showToolbarTooltip = true
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
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (anchoredDraggableState.offset < heightPx) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                // TODO: tab reorder/folders once added
                // TODO: note when returning to this page having done the tutorial
                var tabOpened by remember { mutableStateOf(false) }
                NavBarTutorial(anchoredDraggableState.currentValue == DragAnchors.End, tabOpened) {
                    tabOpened = true
                }
                Box(
                    modifier = Modifier
                        .height(348.dp)
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp)
                        .graphicsLayer {
                            alpha =
                                ((1 - (anchoredDraggableState.offset / heightPx)).coerceAtMost(0.2f) / 0.2f)
                        },
                    contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if(tabOpened) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(72.dp))
                            Text("Perfect!", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            val size = ButtonDefaults.MediumContainerHeight
                            Button(
                                onClick = {
                                    nextPage()
                                },
                                content = {
                                    Text(
                                        "Next",
                                        style = ButtonDefaults.textStyleFor(size)
                                    )
                                },
                                contentPadding = ButtonDefaults.contentPaddingFor(size),
                                modifier = Modifier.heightIn(size)
                            )
                        } else {
                            Icon(Icons.Default.Tab, null, modifier = Modifier.size(72.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Open a tab using the more button", style = MaterialTheme.typography.titleLarge)
                                Icon(Icons.Default.MoreVert, null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavBarTutorial(drawerOpen: Boolean, tabOpened: Boolean, createTab: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(52.dp)
            .padding(4.dp)
    ) {
        Spacer(modifier = Modifier.width(4.dp))
        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = "",
            onValueChange = { },
            enabled = false,
            modifier = Modifier
                .height(48.dp)
                .weight(1f),
            interactionSource = interactionSource,
            singleLine = true,
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = "",
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
        val infiniteTransition = rememberInfiniteTransition()
        val surfaceColor = MaterialTheme.colorScheme.primaryContainer
        Box {
            val animatedSize = infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.8f,
                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
            )
            IconButton(onClick = { moreMenuExpanded = !moreMenuExpanded }, modifier = Modifier.drawBehind {
                if(drawerOpen && !moreMenuExpanded && !tabOpened) drawCircle(surfaceColor, radius = (size.minDimension / 2.0f) * animatedSize.value)
            }) {
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
                val animatedColor = infiniteTransition.animateColor(
                    initialValue = surfaceColor,
                    targetValue = surfaceColor.copy(alpha = 0f),
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.new_tab)) },
                    onClick = {
                        moreMenuExpanded = false
                        createTab()
                    },
                    modifier = Modifier.drawBehind {
                        if(!tabOpened) drawRoundRect(animatedColor.value)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Customize(lastPage: () -> Unit, complete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Customization settings")
        Spacer(modifier = Modifier.weight(1f))
        Row {
            val size = ButtonDefaults.MediumContainerHeight
            FilledTonalButton(
                onClick = {
                    lastPage()
                },
                content = { Text("Back", style = ButtonDefaults.textStyleFor(size)) },
                contentPadding = ButtonDefaults.contentPaddingFor(size),
                modifier = Modifier.heightIn(size)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    complete()
                },
                content = { Text("Get started", style = ButtonDefaults.textStyleFor(size)) },
                contentPadding = ButtonDefaults.contentPaddingFor(size),
                modifier = Modifier.heightIn(size)
            )
        }
    }
}