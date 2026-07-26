package yos.music.player.ui.pages.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.netease.api.NcmAlbum
import yos.music.player.data.netease.api.NcmArtist
import yos.music.player.data.netease.api.NcmPlaylist
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.NcmSong
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.netease.api.toNcmImageUrl
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
    val isLoading = remember { mutableStateOf(false) }
    val searchError = remember { mutableStateOf(false) }
    val hasSearched = remember { mutableStateOf(false) }

    LaunchedEffect(searchText.value) {
        val keyword = searchText.value.trim()
        if (keyword.isEmpty()) {
            results.value = emptyList()
            isLoading.value = false
            searchError.value = false
            hasSearched.value = false
        } else {
            isLoading.value = true
            searchError.value = false
            hasSearched.value = false
            delay(350)
            val result = NcmRepository.search(keyword)
            if (result == null) {
                results.value = emptyList()
                searchError.value = true
            } else {
                results.value = buildList {
                    result.songs?.let { addAll(it) }
                    result.artists?.let { addAll(it) }
                    result.albums?.let { addAll(it) }
                }
            }
            hasSearched.value = true
            isLoading.value = false
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

        if (isLoading.value) {
            item("loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
                }
            }
        } else if (searchText.value.isBlank()) {
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
        } else if (searchError.value || hasSearched.value && results.value.isEmpty()) {
            item("search_status") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchError.value) "搜索失败，请检查网络或 API 设置" else "没有找到相关结果",
                        modifier = Modifier.alpha(0.6f),
                        fontSize = 15.sp
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
            model = imageUrl?.toNcmImageUrl(300),
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
