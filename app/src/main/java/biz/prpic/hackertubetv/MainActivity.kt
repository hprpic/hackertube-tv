package biz.prpic.hackertubetv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import biz.prpic.hackertubetv.nav.BackAction
import biz.prpic.hackertubetv.nav.RailItem
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.ui.AppColors
import biz.prpic.hackertubetv.ui.FocusMoves
import biz.prpic.hackertubetv.ui.HackerTubeTheme
import biz.prpic.hackertubetv.ui.LocalFocusMoves
import biz.prpic.hackertubetv.ui.NavRail
import biz.prpic.hackertubetv.ui.appImageLoader
import biz.prpic.hackertubetv.ui.keepUpAndDownInside
import biz.prpic.hackertubetv.ui.screens.AreaScreen
import biz.prpic.hackertubetv.ui.screens.BrowseScreen
import biz.prpic.hackertubetv.ui.screens.CategoriesScreen
import biz.prpic.hackertubetv.ui.screens.CategoryScreen
import biz.prpic.hackertubetv.ui.screens.HistoryScreen
import biz.prpic.hackertubetv.ui.screens.HomeScreen
import biz.prpic.hackertubetv.ui.screens.LikedScreen
import biz.prpic.hackertubetv.ui.screens.LongsScreen
import biz.prpic.hackertubetv.ui.screens.PlaylistScreen
import biz.prpic.hackertubetv.ui.screens.PlaylistsScreen
import biz.prpic.hackertubetv.ui.screens.SearchScreen
import biz.prpic.hackertubetv.ui.screens.SettingsScreen
import biz.prpic.hackertubetv.ui.screens.WatchLaterScreen
import coil3.SingletonImageLoader
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels { AppViewModel.Factory }

    private val focusMoves = FocusMoves()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SingletonImageLoader.setSafe { context -> appImageLoader(context, AppGraph.httpClient) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playRequests.collect(::startYouTube)
            }
        }
        setContent {
            HackerTubeTheme {
                CompositionLocalProvider(LocalFocusMoves provides focusMoves) {
                    AppContent(viewModel, onLeaveApp = ::finish)
                }
            }
        }
    }

    // Every key of the window passes here first, even while nothing has focus yet, as at a
    // cold start before the first cards arrive.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        focusMoves.recordKey(event)
        return super.dispatchKeyEvent(event)
    }

    private fun startYouTube(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Nothing on this TV opens YouTube links; the card stays focused.
            Log.w(TAG, "No app opens ${intent.data}", e)
        }
    }

    private companion object {
        const val TAG = "HackerTube"
    }
}

/** The menu on the left and the current screen next to it, with the app's Back rules. */
@Composable
private fun AppContent(viewModel: AppViewModel, onLeaveApp: () -> Unit) {
    val nav = viewModel.backStack
    val library by viewModel.library.collectAsStateWithLifecycle()
    val railRequester = remember { FocusRequester() }
    val contentRequester = remember { FocusRequester() }
    var focusInRail by remember { mutableStateOf(false) }
    // Bumped to focus the menu's current item once the screen chosen with Back is shown.
    var railFocusRequests by remember { mutableIntStateOf(0) }
    val focusMoves = LocalFocusMoves.current
    val focusRail: () -> Unit = { railRequester.requestFocus() }

    LaunchedEffect(railFocusRequests) {
        if (railFocusRequests == 0) return@LaunchedEffect
        // Counted as a move, so the screen that just came up leaves focus in the menu.
        focusMoves.record()
        focusRail()
    }

    BackHandler {
        // Back moves focus too, so a grid still restoring its card leaves it alone.
        focusMoves.record()
        when (viewModel.onBack(focusInRail)) {
            // The screen that comes up puts focus on its own content.
            BackAction.POP -> Unit
            // Home comes up with focus kept in the menu, now on Home, so the next Back exits.
            BackAction.GO_HOME -> railFocusRequests++
            BackAction.FOCUS_RAIL -> focusRail()
            BackAction.EXIT -> onLeaveApp()
        }
    }

    Row(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        NavRail(
            current = nav.railItem,
            currentItemRequester = railRequester,
            onSelect = { item ->
                // OK on the item of the screen already shown moves into that screen, as Right does.
                if (viewModel.backStack.isShowing(item)) {
                    contentRequester.requestFocus(FocusDirection.Right)
                } else {
                    viewModel.selectRail(item)
                }
            },
            modifier = Modifier.onFocusChanged { focusInRail = it.hasFocus },
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(contentRequester)
                // Up or Down at the top or bottom of a screen does not jump into the menu.
                .focusProperties { onExit = { keepUpAndDownInside() } }
                .focusGroup(),
        ) {
            // Keyed by screen: each screen starts with fresh UI state and takes focus when it appears.
            key(nav.current) {
                when (val screen = nav.current) {
                    Screen.Home -> HomeScreen(viewModel, library)
                    Screen.Longs -> LongsScreen(viewModel, library)
                    Screen.Liked -> LikedScreen(viewModel, library)
                    Screen.History -> HistoryScreen(viewModel, library, focusMenu = focusRail)
                    Screen.WatchLater -> WatchLaterScreen(viewModel, library)
                    Screen.Categories -> CategoriesScreen(
                        viewModel,
                        onOpen = viewModel::open,
                        onOpenLongs = { viewModel.selectRail(RailItem.LONGS) },
                    )
                    is Screen.Area -> AreaScreen(viewModel, library, screen)
                    is Screen.Category -> CategoryScreen(viewModel, library, screen)
                    is Screen.Browse -> BrowseScreen(viewModel, library, screen)
                    Screen.Playlists -> PlaylistsScreen(viewModel, library, onOpen = viewModel::open)
                    is Screen.PlaylistDetail -> PlaylistScreen(viewModel, library, screen)
                    Screen.Search -> SearchScreen(viewModel, library)
                    Screen.Settings -> SettingsScreen(library, onToggle = viewModel::toggleShowFrames)
                }
            }
        }
    }
}
