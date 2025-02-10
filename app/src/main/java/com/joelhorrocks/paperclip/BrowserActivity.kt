package com.joelhorrocks.paperclip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults.ContainerBox
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelhorrocks.paperclip.ui.theme.PaperclipTheme
import dagger.hilt.android.AndroidEntryPoint
import org.mozilla.geckoview.GeckoView

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val browserViewModel: BrowserViewModel by viewModels()

        enableEdgeToEdge()
        setContent {
            PaperclipTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        NavBar(browserViewModel)
                        BrowserScreen(browserViewModel)
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
        modifier = Modifier.height(52.dp).padding(4.dp)
    ) {
        var tabMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(
                onClick = {
                    tabMenuExpanded = !tabMenuExpanded
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tab,
                    contentDescription = "Tabs"
                )
            }
            Badge(
                modifier = Modifier.align(Alignment.TopEnd),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    browserViewModel.uiState.collectAsStateWithLifecycle().value.tabs.size.toString(),
                )
            }
            DropdownMenu(
                expanded = tabMenuExpanded,
                onDismissRequest = { tabMenuExpanded = false }
            ) {
                browserViewModel.uiState.collectAsStateWithLifecycle().value.tabs.forEachIndexed { index, tab ->
                    DropdownMenuItem(
                        text = { Text(tab.currentUrl) },
                        onClick = {
                            browserViewModel.selectTab(index)
                            tabMenuExpanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tab,
                                contentDescription = "Tab"
                            )
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val url = browserViewModel.uiState.collectAsStateWithLifecycle().value.currentUrl
        val interactionSource = remember { MutableInteractionSource() }
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
                }
            ),
            keyboardOptions = KeyboardOptions(
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
                    ContainerBox(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = TextFieldDefaults.colors(),
                        shape = RoundedCornerShape(32.dp)
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