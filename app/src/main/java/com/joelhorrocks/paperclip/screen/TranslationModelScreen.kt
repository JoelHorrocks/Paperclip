package com.joelhorrocks.paperclip.screen

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelhorrocks.paperclip.ml.TranslationModelDownloadStatus
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import com.joelhorrocks.paperclip.vm.TranslationModelLoadingState
import com.joelhorrocks.paperclip.vm.TranslationModelViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TranslationModelScreen(translationModelViewModel: TranslationModelViewModel, back: () -> Unit) {
    val state by translationModelViewModel.uiState.collectAsStateWithLifecycle()
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
                        Text("Translation models")
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
            // TODO: animate model moving between downloaded / remote
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        if(state.modelList.any { it.downloadStatus is TranslationModelDownloadStatus.Downloaded }) {
                            Text(
                                text = "Downloaded",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    items(state.modelList.filter { it.downloadStatus is TranslationModelDownloadStatus.Downloaded }) { model ->
                        ModelCard(
                            model.name,
                            model.fromLanguage.name,
                            model.toLanguage.name,
                            model.size.toString(),
                            {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    null
                                )
                            }
                        ) {
                            translationModelViewModel.deleteModel(model.id)
                        }
                    }
                    item {
                        if(state.modelList.any { it.downloadStatus is TranslationModelDownloadStatus.Available }) {
                            Text(
                                text = "Available",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    items(state.modelList.filter { it.downloadStatus is TranslationModelDownloadStatus.Available }) { model ->
                        ModelCard(
                            model.name,
                            model.fromLanguage.name,
                            model.toLanguage.name,
                            model.size.toString(),
                            {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    null
                                )
                            }
                        ) {
                            translationModelViewModel.markDownloaded(model.id)
                        }
                    }
                    if(state.translationModelLoadingState == TranslationModelLoadingState.LOADING) {
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

@Composable
fun ModelCard(name: String, fromLanguage: String, toLanguage: String, size: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
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
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(name)
                Text(
                    "${fromLanguage}-${toLanguage} • ${size}B",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(
                onClick = { onClick() }
            ) {
                icon()
            }
        }
    }
}