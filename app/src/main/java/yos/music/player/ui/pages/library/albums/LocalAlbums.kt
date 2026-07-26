package yos.music.player.ui.pages.library.albums

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.data.netease.api.NcmAlbum
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.ImageQuality
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.ShadowImage
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.TitleWithLazyVerticalGrid

private enum class AlbumsLoadState { Loading, Success, Error }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LocalAlbums(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    var albumsList by remember { mutableStateOf<List<NcmAlbum>>(emptyList()) }
    var loadState by remember { mutableStateOf(AlbumsLoadState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadKey) {
        loadState = AlbumsLoadState.Loading
        NcmRepository.getSubscribedAlbumsResult().fold(
            onSuccess = {
                albumsList = it
                loadState = AlbumsLoadState.Success
            },
            onFailure = { loadState = AlbumsLoadState.Error }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
    ) {
        val searchText = remember("LocalAlbums_searchText") {
            mutableStateOf("")
        }

        if (loadState != AlbumsLoadState.Success || albumsList.isEmpty()) {
            Title(
                title = stringResource(id = R.string.page_library_albums), onBack = {
                    navController.popBackStack()
                }
            ) {
                item("album_status") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (loadState) {
                            AlbumsLoadState.Loading -> CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 3.dp
                            )
                            AlbumsLoadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(id = R.string.page_library_load_failed),
                                    modifier = Modifier.padding(horizontal = 20.dp).alpha(0.6f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = stringResource(id = R.string.page_library_retry),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { reloadKey++ }.padding(16.dp)
                                )
                            }
                            AlbumsLoadState.Success -> Text(
                                text = stringResource(id = R.string.page_library_no_subscribed_albums),
                                fontSize = 18.sp,
                                modifier = Modifier.alpha(0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            val query = searchText.value.trim()
            val filteredAlbums = if (query.isEmpty()) {
                albumsList
            } else {
                albumsList.filter { album ->
                    album.name.contains(query, ignoreCase = true) ||
                        album.artist?.name?.contains(query, ignoreCase = true) == true
                }
            }

            TitleWithLazyVerticalGrid(
                title = stringResource(id = R.string.page_library_albums), onBack = {
                    navController.popBackStack()
                }
            ) {
                item("SearchField", span = { GridItemSpan(2) }) {
                    val keyboardController = LocalSoftwareKeyboardController.current

                    SearchTextField(
                        text = searchText.value,
                        placeholder = stringResource(id = R.string.page_library_search_album),
                        onValueChange = {
                            searchText.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        onSearch = {
                            if (searchText.value.isNotEmpty()) {
                                keyboardController?.hide()
                            }
                        })
                }
                itemsIndexed(
                    filteredAlbums,
                    key = { _, album -> album.id.toString() }
                ) { _, album ->
                    AlbumItems(
                        album = album,
                        animatedContentScope = animatedContentScope,
                        sharedTransitionScope = sharedTransitionScope
                    ) {
                        scope.launch {
                            val songs = NcmRepository.getAlbumSongs(album.id).map { it.toYosMediaItem() }
                            LibraryObject.setTargetListWithTitle(album.name, songs)
                            navController.toUI(UI.NormalMusic)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LazyGridItemScope.AlbumItems(
    album: NcmAlbum,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        ShadowImage(
            dataLambda = { album.picUrl?.let { android.net.Uri.parse(it) } },
            contentDescription = "Album",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            shadowAlpha = 0f,
            cornerRadius = 7.dp,
            imageQuality = ImageQuality.HIGH
        )
        Text(
            text = album.name,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(0.9f)
        )

        Text(
            text = album.artist?.name ?: "",
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(0.6f),
            lineHeight = 17.sp
        )
    }
}