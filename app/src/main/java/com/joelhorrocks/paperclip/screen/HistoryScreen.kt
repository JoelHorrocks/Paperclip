package com.joelhorrocks.paperclip.screen

import android.text.format.DateFormat.getDateFormat
import android.text.format.DateFormat.getTimeFormat
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.appBarWithSearchColors
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelhorrocks.paperclip.R
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import com.joelhorrocks.paperclip.vm.HistoryLoadingState
import com.joelhorrocks.paperclip.vm.HistoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryScreen(historyViewModel: HistoryViewModel, back: () -> Unit) {
    var clearDialogOpen by remember { mutableStateOf(false) }
    val state by historyViewModel.uiState.collectAsStateWithLifecycle()
    PaperclipTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                var showSearch by remember { mutableStateOf(false) }
                // TODO: animation issues: status bar color animates less slowly and search bar is slightly taller
                Crossfade(
                    targetState = showSearch
                ) { searchVisible ->
                    if (!searchVisible) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            title = {
                                Text(stringResource(R.string.history))
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    back()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        showSearch = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clearDialogOpen = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    } else {
                        val searchBarState = rememberSearchBarState()
                        AppBarWithSearch(
                            state = searchBarState,
                            inputField = {
                                InputField(
                                    searchBarState = searchBarState,
                                    searchText = state.searchQuery,
                                    setSearchText = {
                                        historyViewModel.searchHistory(it)
                                    },
                                    closeSearchBar = {
                                        showSearch = false
                                        historyViewModel.searchHistory("")
                                    }
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { back() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.back)
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        clearDialogOpen = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        // TODO: string resource
                                        contentDescription = null
                                    )
                                }
                            },
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (clearDialogOpen) {
                ClearHistoryConfirm(
                    onDismissRequest = { clearDialogOpen = false },
                    onConfirmation = {
                        clearDialogOpen = false
                        historyViewModel.clearHistory()
                    }
                )
            }
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                // TODO: swipe refresh, FAB to return to top
                val context = LocalContext.current
                LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.filteredHistoryList) { historyEntry ->
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Web,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = historyEntry.title?.ifBlank { stringResource(R.string.no_title) } ?: stringResource(R.string.no_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = historyEntry.url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = getDateFormat(context).format(historyEntry.timestamp) + " " +
                                                getTimeFormat(context).format(historyEntry.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                    if(state.historyLoadingState == HistoryLoadingState.LOADING) {
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
                    }
                }
            }
        }
    }
}

// TODO: share generic dialog composable?
@Composable
fun ClearHistoryConfirm(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Delete, contentDescription = null)
        },
        title = {
            // TODO: string resource
            Text(text = "Clear browsing history?")
        },
        text = {
            Text(text = "Are you sure you want to clear your browsing history? This action cannot be undone.")
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputField(
    searchBarState: SearchBarState,
    searchText: String,
    setSearchText: (String) -> Unit = { },
    closeSearchBar: () -> Unit = { }
) {
    // TODO: search content type (treated as normal text not an URL)
    val scope = rememberCoroutineScope()
    SearchBarDefaults.InputField(
        query = searchText,
        onQueryChange = {
            setSearchText(it)
        },
        //searchBarState = searchBarState,
        expanded = searchBarState.currentValue == SearchBarValue.Expanded,
        onExpandedChange = {
            scope.launch {
                if (it) {
                    searchBarState.animateToExpanded()
                } else {
                    searchBarState.animateToCollapsed()
                }
            }
        },
        colors = appBarWithSearchColors().searchBarColors.inputFieldColors,
        onSearch = { },
        trailingIcon = {
            IconButton(
                onClick = { closeSearchBar() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
        },
        placeholder = {
            Text(modifier = Modifier.clearAndSetSemantics {}, text = "Search")
        }
    )
}