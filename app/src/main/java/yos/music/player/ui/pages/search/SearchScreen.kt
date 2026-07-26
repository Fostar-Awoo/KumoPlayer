package yos.music.player.ui.pages.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.netease.api.NcmAlbum
import yos.music.player.data.netease.api.NcmArtist
import yos.music.player.data.netease.api.NcmPlaylist
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.NcmSong
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.toUI
import yos.music.player.ui.pages.library.MusicList
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.Title

@Composable
fun SearchScreen(navController: NavController) {
    val searchText = remember { mutableStateOf("") }
    val results = remember { mutableStateOf<List<Any>>(emptyList()) }

    LaunchedEffect(searchText.value) {
        if (searchText.value.isEmpty()) {
            results.value = emptyList()
        } else {
            val keyword = searchText.value
            NcmRepository.search(keyword)?.let { result ->
                results.value = buildList {
                    result.songs?.let { addAll(it) }
                    result.artists?.let { addAll(it) }
                    result.albums?.let { addAll(it) }
                }
            }
        }
    }

    Title(title = stringResource(id = R.string.page_search_title), onBack = null) {
        item("search_field") {
            SearchTextField(
                text = searchText.value,
                placeholder = stringResource(id = R.string.page_library_search_songs),
                onValueChange = { searchText.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 5.dp),
                onSearch = { }
            )
        }

        if (searchText.value.isEmpty()) {
            item("empty") {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).alpha(0.3f),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Search for songs, artists, albums",
                        modifier = Modifier.alpha(0.6f),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            itemsIndexed(results.value, key = { index, _ -> index }) { _, item ->
                when (item) {
                    is NcmSong -> {
                        val mediaItem = remember(item.id) { item.toYosMediaItem() }
                        MusicList(mediaItem) {
                            val list = results.value.filterIsInstance<NcmSong>().map { it.toYosMediaItem() }
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                MediaController.prepare(mediaItem, list)
                            }
                        }
                    }
                    is NcmArtist -> SearchResultItem(title = item.name, subtitle = "Artist", imageUrl = item.picUrl) {
                        navController.navigate("${UI.ArtistInfo}/${item.id}")
                    }
                    is NcmAlbum -> SearchResultItem(title = item.name, subtitle = item.artist?.name, imageUrl = item.picUrl) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            val songs = NcmRepository.getAlbumSongs(item.id).map { it.toYosMediaItem() }
                            LibraryObject.setTargetListWithTitle(item.name, songs)
                            withContext(Dispatchers.Main) { navController.toUI(UI.NormalMusic) }
                        }
                    }
                    is NcmPlaylist -> SearchResultItem(title = item.name, subtitle = "Playlist", imageUrl = item.coverImgUrl) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            val songs = NcmRepository.getPlaylistTracks(item.id).map { it.toYosMediaItem() }
                            LibraryObject.setTargetListWithTitle(item.name, songs)
                            withContext(Dispatchers.Main) { navController.toUI(UI.NormalMusic) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(title: String, subtitle: String?, imageUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle ?: "", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alpha(0.6f))
        }
    }
}
