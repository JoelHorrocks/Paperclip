package com.joelhorrocks.paperclip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.joelhorrocks.paperclip.screen.BrowserScreen
import com.joelhorrocks.paperclip.screen.NewsfeedScreen
import com.joelhorrocks.paperclip.screen.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val backStack = rememberNavBackStack(Screen.Home)

            // TODO: back button animation for loaded pages / different navigation setup for browser page?
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSceneSetupNavEntryDecorator(),
                    rememberSavedStateNavEntryDecorator(),
                    // TODO: scope to NavEntry or activity? Might depend on how I decide to implement
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    entry<Screen.Home> {
                        val browserViewModel: BrowserViewModel by viewModels()
                        BrowserScreen(browserViewModel) { screen ->
                            backStack.add(screen)
                        }
                    }
                    entry<Screen.Newsfeed> {
                        NewsfeedScreen(back = { backStack.removeLastOrNull() })
                    }
                    entry<Screen.Settings> {
                        SettingsScreen(back = { backStack.removeLastOrNull() })
                    }
                }
            )
        }
    }
}