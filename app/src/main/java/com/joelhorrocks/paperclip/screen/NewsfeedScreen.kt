package com.joelhorrocks.paperclip.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.joelhorrocks.paperclip.R
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import com.joelhorrocks.paperclip.vm.ArticleLoadingState
import com.joelhorrocks.paperclip.vm.NewsfeedViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewsfeedScreen(newsfeedViewModel: NewsfeedViewModel, back: () -> Unit) {
    PaperclipTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("Newsfeed")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            back()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                )
            }
        ) { innerPadding ->
            val state by newsfeedViewModel.uiState.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                // TODO: swipe refresh, FAB to return to top
                val listState = rememberLazyListState()
                val offset = 1
                val triggerLoad by remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisibleItem?.index != 0 && lastVisibleItem?.index == listState.layoutInfo.totalItemsCount - offset
                    }
                }
                // TODO: error handling
                LazyColumn(state = listState, contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.articleList) { article ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardDefaults.shape)
                                .height(300.dp)
                                .clickable {
                                    //TODO: load URL
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                ) {
                                    AsyncImage(
                                        modifier = Modifier.fillMaxWidth(),
                                        model = article.imageUrl,
                                        contentScale = ContentScale.Crop,
                                        contentDescription = null,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .clip(RoundedCornerShape(32.dp))
                                                .background(Color.White.copy(alpha = 0.7f))
                                        ) {
                                            IconButton(onClick = { }) {
                                                Icon(
                                                    Icons.Default.BookmarkBorder,
                                                    null
                                                )
                                            }
                                            IconButton(onClick = { }) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    null
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDefaults.outlinedCardColors().containerColor)
                                ) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        article.headline,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Newspaper,
                                            null,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(16.dp)
                                        )
                                        Text(
                                            article.publisher,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                    if(state.articleLoadingState == ArticleLoadingState.LOADING) {
                        item {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(242.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ContainedLoadingIndicator(modifier = Modifier.size(56.dp))
                                }
                            }
                        }
                    } else if(state.articleLoadingState == ArticleLoadingState.END) {
                        item {
                            Box (
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(242.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No more articles")
                            }
                        }
                    }
                }
                LaunchedEffect(triggerLoad) {
                    if(triggerLoad && state.articleLoadingState == ArticleLoadingState.SUCCESS) newsfeedViewModel.fetchArticleBatch()
                }
            }
        }
    }
}