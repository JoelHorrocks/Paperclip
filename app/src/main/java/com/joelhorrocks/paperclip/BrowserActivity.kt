package com.joelhorrocks.paperclip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.joelhorrocks.paperclip.screen.BrowserScreen
import com.joelhorrocks.paperclip.vm.BrowserViewModel
import com.joelhorrocks.paperclip.screen.HistoryScreen
import com.joelhorrocks.paperclip.vm.HistoryViewModel
import com.joelhorrocks.paperclip.screen.NewsfeedScreen
import com.joelhorrocks.paperclip.vm.NewsfeedViewModel
import com.joelhorrocks.paperclip.screen.SettingsScreen
import com.joelhorrocks.paperclip.screen.SetupScreen
import com.joelhorrocks.paperclip.screen.TranslationModelScreen
import com.joelhorrocks.paperclip.vm.TranslationModelViewModel
import com.joelhorrocks.paperclip.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity() : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val backStack = rememberNavBackStack(Screen.Home)

            fun back() {
                backStack.removeLastOrNull()
            }

            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                settingsRepository.showSetup.collect {
                    if(it) {
                        backStack.clear()
                        backStack.add(Screen.Setup)
                    }
                }
            }

            // TODO: back button animation for loaded pages / different navigation setup for browser page?
            NavDisplay(
                backStack = backStack,
                onBack = { back() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<Screen.Setup> {
                        SetupScreen {
                            backStack.add(Screen.Home)
                            backStack.remove(Screen.Setup)
                            scope.launch {
                                settingsRepository.setShowSetup(false)
                            }
                        }
                    }
                    entry<Screen.Home> {
                        val browserViewModel: BrowserViewModel = hiltViewModel()
                        BrowserScreen(browserViewModel) {
                            backStack.add(it)
                        }
                    }
                    entry<Screen.Newsfeed> {
                        val newsfeedViewModel: NewsfeedViewModel = hiltViewModel()
                        NewsfeedScreen(newsfeedViewModel, back = { back() })
                    }
                    entry<Screen.History> {
                        val historyViewModel: HistoryViewModel = hiltViewModel()
                        HistoryScreen(historyViewModel, back = { back() })
                    }
                    entry<Screen.Settings> {
                        SettingsScreen(back = { back() }) {
                            backStack.add(it)
                        }
                    }
                    entry<Screen.TranslationModel> {
                        val translationModelViewModel: TranslationModelViewModel = hiltViewModel()
                        TranslationModelScreen(translationModelViewModel, back = { back() })
                    }
                }
            )
        }
    }
}