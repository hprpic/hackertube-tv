package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.AppViewModel
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.data.Video
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.paging.SearchNote
import biz.prpic.hackertubetv.paging.SearchSession
import biz.prpic.hackertubetv.paging.searchNoteText
import biz.prpic.hackertubetv.ui.AppColors
import biz.prpic.hackertubetv.ui.DEFAULT_EMPTY_TEXT
import biz.prpic.hackertubetv.ui.LocalFocusMoves
import biz.prpic.hackertubetv.ui.VideoActionsDialog
import biz.prpic.hackertubetv.ui.VideoGrid

private val FieldShape = RoundedCornerShape(10.dp)

/**
 * A search field and, under it, the results of the last submitted query in the same card
 * grid as the other screens. OK on the field opens the TV keyboard; its search action
 * runs the search and moves focus to the first result. The site is asked once per
 * submitted query, never while typing.
 */
@Composable
fun SearchScreen(viewModel: AppViewModel, library: UserLibrary) {
    val screen = Screen.Search
    val session = viewModel.search
    // Starts as the restored query, so a search rerun after process death shows it in the field.
    var text by rememberSaveable { mutableStateOf(session?.query.orEmpty()) }
    // True from OK on the field until it loses focus; only then can the text field take focus.
    var editing by remember { mutableStateOf(false) }
    // Bumped to put focus back on the field's own surface after a search was submitted.
    var refocusField by remember { mutableIntStateOf(0) }
    val fieldRequester = remember { FocusRequester() }
    val textRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusMoves = LocalFocusMoves.current
    var menuFor by remember { mutableStateOf<Video?>(null) }

    LaunchedEffect(editing) {
        if (editing) {
            textRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(refocusField) {
        if (refocusField > 0) fieldRequester.requestFocus()
    }
    // Like every screen, Search takes focus when it appears: the field when there is no
    // query yet; with results, the grid restores focus to its card instead.
    LaunchedEffect(Unit) {
        if (session == null) fieldRequester.requestFocus()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeading("Search")
        Surface(
            onClick = {
                // Typing is a move of its own, so a first result still on its way cannot
                // take focus from the keyboard.
                focusMoves.record()
                editing = true
            },
            modifier = Modifier
                .padding(start = 8.dp, bottom = 12.dp)
                .width(560.dp)
                .focusRequester(fieldRequester),
            shape = ClickableSurfaceDefaults.shape(FieldShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AppColors.Raised,
                contentColor = AppColors.Text,
                focusedContainerColor = AppColors.Raised,
                focusedContentColor = AppColors.Text,
                pressedContainerColor = AppColors.Raised,
                pressedContentColor = AppColors.Text,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    BorderStroke(3.dp, AppColors.FocusFrame),
                    shape = FieldShape,
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .width(528.dp)
                        .focusRequester(textRequester)
                        // The field itself takes focus only while the viewer is typing, so the
                        // keyboard never opens just because focus passed over the field.
                        .focusProperties { canFocus = editing }
                        .onFocusChanged { if (!it.isFocused) editing = false },
                    singleLine = true,
                    textStyle = TextStyle(color = AppColors.Text, fontSize = 18.sp),
                    cursorBrush = SolidColor(AppColors.Text),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboard?.hide()
                            editing = false
                            viewModel.submitSearch(text)
                            refocusField++
                        },
                    ),
                )
            }
        }
        if (session != null) {
            key(session) {
                SearchResults(
                    viewModel = viewModel,
                    session = session,
                    library = library,
                    screen = screen,
                    onLongPress = { menuFor = it },
                )
            }
        }
    }
    menuFor?.let { video ->
        VideoActionsDialog(
            video = video,
            library = library,
            onDismiss = { menuFor = null },
            onToggleLike = { viewModel.toggleLike(video.vid) },
            onToggleLater = { viewModel.toggleLater(video.vid) },
            onToggleDislike = { viewModel.toggleDislike(video.vid) },
        )
    }
}

@Composable
private fun SearchResults(
    viewModel: AppViewModel,
    session: SearchSession,
    library: UserLibrary,
    screen: Screen,
    onLongPress: (Video) -> Unit,
) {
    val note by session.note.collectAsStateWithLifecycle()
    val noteText = searchNoteText(note, session.query)
    val initialFocus = remember { viewModel.focusIndex(screen) }
    Column(Modifier.fillMaxSize()) {
        if (note == SearchNote.WEAK && noteText != null) {
            Text(
                text = noteText,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                color = AppColors.Secondary,
                fontSize = 14.sp,
            )
        }
        VideoGrid(
            pager = session.pager,
            library = library,
            initialFocus = initialFocus,
            onFocusChange = { viewModel.setFocusIndex(screen, it) },
            onPlay = { viewModel.play(it.vid) },
            onLongPress = onLongPress,
            emptyText = noteText ?: DEFAULT_EMPTY_TEXT,
        )
    }
}
