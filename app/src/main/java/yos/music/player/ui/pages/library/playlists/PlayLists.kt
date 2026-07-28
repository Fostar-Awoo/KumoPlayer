package yos.music.player.ui.pages.library.playlists

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.data.netease.api.NcmApiClient
import yos.music.player.data.netease.api.NcmPlaylist
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.YosRoundedCornerShape
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper

@Composable
fun PlayLists(navController: NavController) {
    val playListsState = remember { mutableStateOf<List<NcmPlaylist>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    YosWrapper {
        LaunchedEffect(Unit) {
            if (playListsState.value.isEmpty()) {
                val uid = NcmApiClient.userId
                playListsState.value = NcmRepository.getUserPlaylists(uid)
            }
        }
    }

    val playLists = playListsState.value

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                scope.launch(Dispatchers.IO) {
                    val result = NcmRepository.createPlaylist(name)
                    result.getOrNull()?.let {
                        // Refresh playlist after creation
                        val uid = NcmApiClient.userId
                        playListsState.value = NcmRepository.getUserPlaylists(uid)
                    }
                }
            }
        )
    }

    Title(title = stringResource(id = R.string.page_library_playlists),
        onBack = {
            navController.popBackStack()
        },
        content = {
            item("AddList") {
                val targetTitle = context.getString(R.string.page_library_playlists_add_title)
                PlayListItem(playListType = PlayListType.Add, title = targetTitle) {
                    showCreateDialog = true
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 102.dp)
                        .alpha(0.15f)
                        .height(0.5.dp)
                        .background(Color.Black withNight Color.White)
                )
            }

            itemsIndexed(
                playLists,
                key = { _, playList -> playList.id.toString() }
            ) { index, playList ->
                NcmPlaylistItem(playlist = playList) {
                    scope.launch(Dispatchers.IO) {
                        val targetTitle = playList.name
                        val targetList = NcmRepository.getPlaylistTracks(playList.id)
                            .map { it.toYosMediaItem() }
                        LibraryObject.setTargetListWithTitle(targetTitle, targetList)
                        withContext(Dispatchers.Main) {
                            navController.toUI(UI.NormalMusic)
                        }
                    }
                }
                key(index) {
                    val needDivider = index < playLists.size - 1
                    if (needDivider) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 102.dp)
                                .alpha(0.15f)
                                .height(0.5.dp)
                                .background(Color.Black withNight Color.White)
                        )
                    }
                }
            }
        }
    )
}

@Stable
private enum class PlayListType {
    Add
}

@Composable
private fun LazyItemScope.NcmPlaylistItem(playlist: NcmPlaylist, itemClick: () -> Unit) {
    Row(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .height(80.dp)
            .fillMaxWidth()
            .clickable { itemClick() }
            .padding(start = 22.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = YosRoundedCornerShape(4.dp)
        val density = LocalDensity.current

        AsyncImage(
            model = playlist.coverImgUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = true
                    this.shape = shape
                }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        val outline = shape.createOutline(
                            Size(size.width, size.height),
                            LayoutDirection.Ltr,
                            density
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.Gray.copy(alpha = 0.1f),
                            style = Stroke(width = 8f)
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.Gray.copy(alpha = 0.5f),
                            style = Stroke(width = 8f),
                            blendMode = BlendMode.Overlay
                        )
                    }
                }
        )

        Column(
            Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = playlist.name,
                modifier = Modifier.padding(bottom = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                lineHeight = 16.sp,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_action_next), contentDescription = null,
            modifier = Modifier
                .height(40.dp)
                .alpha(0.3f), tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun LazyItemScope.PlayListItem(playListType: PlayListType, title: String, itemClick: () -> Unit) {
    Row(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .height(80.dp)
            .fillMaxWidth()
            .clickable {
                itemClick()
            }
            .padding(start = 22.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        println("重组：播放列表 ${playListType.name}")

        val shape = YosRoundedCornerShape(4.dp)
        val density = LocalDensity.current

        Image(painter = painterResource(id = R.drawable.placeholder_playlist_new),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = true
                    this.shape = shape
                }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        val outline = shape.createOutline(
                            Size(size.width, size.height),
                            LayoutDirection.Ltr,
                            density
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.Gray.copy(alpha = 0.1f),
                            style = Stroke(width = 8f)
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.Gray.copy(alpha = 0.5f),
                            style = Stroke(width = 8f),
                            blendMode = BlendMode.Overlay
                        )
                    }
                })

        Column(
            Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                lineHeight = 16.sp,
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_action_next), contentDescription = null,
            modifier = Modifier
                .height(40.dp)
                .alpha(0.3f), tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_create_dialog_title)) },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text(stringResource(R.string.playlist_create_dialog_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (playlistName.isNotBlank()) onCreate(playlistName.trim()) },
                enabled = playlistName.isNotBlank()
            ) {
                Text(stringResource(R.string.playlist_create_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_create_dialog_cancel))
            }
        }
    )
}
