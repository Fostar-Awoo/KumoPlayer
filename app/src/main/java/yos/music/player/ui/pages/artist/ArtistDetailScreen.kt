package yos.music.player.ui.pages.artist

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.netease.api.NcmAlbum
import yos.music.player.data.netease.api.NcmArtistDetail
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.pages.library.MusicList
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.Title

@Composable
fun ArtistDetailScreen(navController: NavController, artistId: Long?) {
    val id = artistId ?: 0L
    val artistDetail = remember { mutableStateOf<NcmArtistDetail?>(null) }
    val hotSongs = remember { mutableStateOf<List<YosMediaItem>>(emptyList()) }
    val albums = remember { mutableStateOf<List<NcmAlbum>>(emptyList()) }

    LaunchedEffect(id) {
        if (id != 0L) {
            artistDetail.value = NcmRepository.getArtistDetail(id)
            hotSongs.value = NcmRepository.getArtistSongs(id).map { it.toYosMediaItem() }
            albums.value = NcmRepository.getArtistAlbums(id)
        }
    }

    Title(title = artistDetail.value?.name ?: stringResource(id = R.string.page_library_artists), onBack = {
        navController.popBackStack()
    }) {
        item("header") { ArtistHeader(artistDetail.value) }

        item("hot_songs_title") {
            Text("Hot Songs", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
        }

        if (hotSongs.value.isEmpty()) {
            item("no_songs") {
                Text(stringResource(id = R.string.tip_no_song), modifier = Modifier.padding(horizontal = 20.dp), fontSize = 16.sp)
            }
        } else {
            itemsIndexed(hotSongs.value, key = { index, song -> song.neteaseId ?: index }) { _, song ->
                MusicList(song) {
                    CoroutineScope(Dispatchers.IO).launch { MediaController.prepare(song, hotSongs.value) }
                }
            }
        }

        item("albums_title") {
            Text("Albums", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
        }

        if (albums.value.isEmpty()) {
            item("no_albums") {
                Text("No albums", modifier = Modifier.padding(horizontal = 20.dp), fontSize = 16.sp)
            }
        } else {
            itemsIndexed(albums.value, key = { _, album -> album.id.toString() }) { _, album ->
                AlbumItem(album = album) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val songs = NcmRepository.getAlbumSongs(album.id).map { it.toYosMediaItem() }
                        LibraryObject.setTargetListWithTitle(album.name, songs)
                        withContext(Dispatchers.Main) { navController.toUI(UI.NormalMusic) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistHeader(artistDetail: NcmArtistDetail?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val shape = CircleShape
        val density = LocalDensity.current
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(data = artistDetail?.avatar ?: artistDetail?.cover).crossfade(true)
                .error(R.drawable.songcredits_monogram_person)
                .placeholder(R.drawable.songcredits_monogram_person)
                .fallback(R.drawable.songcredits_monogram_person)
                .allowHardware(true)
                .precision(Precision.INEXACT)
                .size(128)
                .build(),
            contentDescription = "Artist_Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(128.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = true
                    this.shape = shape
                }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        val outline = shape.createOutline(Size(size.width, size.height), LayoutDirection.Ltr, density)
                        drawOutline(outline = outline, color = Color.DarkGray.copy(alpha = 0.08f), style = Stroke(width = 6f))
                        drawOutline(outline = outline, color = Color.DarkGray.copy(alpha = 0.4f), style = Stroke(width = 6f), blendMode = BlendMode.Overlay)
                    }
                }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(artistDetail?.name ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        artistDetail?.briefDesc?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.alpha(0.6f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AlbumItem(album: NcmAlbum, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = album.picUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(album.name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(album.artist?.name ?: "", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alpha(0.6f))
        }
        Icon(painter = painterResource(id = R.drawable.ic_action_next), contentDescription = null,
            modifier = Modifier.height(12.dp).alpha(0.3f), tint = MaterialTheme.colorScheme.onBackground)
    }
}
