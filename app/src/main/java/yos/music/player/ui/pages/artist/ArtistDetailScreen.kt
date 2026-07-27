package yos.music.player.ui.pages.artist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.google.accompanist.insets.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cormor.overscroll.core.overScrollVertical
import com.cormor.overscroll.core.rememberOverscrollFlingBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.code.utils.others.Vibrator
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.libraries.artistsName
import yos.music.player.data.netease.api.NcmAlbum
import yos.music.player.data.netease.api.NcmArtistDetail
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

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

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val collapseThresholdPx = remember(density, configuration) {
        with(density) { (configuration.screenWidthDp.dp - 96.dp).toPx() }
    }
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }

    val playAll = playAll@{
        val songs = hotSongs.value
        if (songs.isEmpty()) return@playAll
        CoroutineScope(Dispatchers.IO).launch {
            MediaController.prepare(songs.first(), songs)
        }
        Unit
    }

    val openAlbum: (NcmAlbum) -> Unit = { album ->
        CoroutineScope(Dispatchers.IO).launch {
            val songs = NcmRepository.getAlbumSongs(album.id).map { it.toYosMediaItem() }
            LibraryObject.setTargetListWithTitle(album.name, songs)
            withContext(Dispatchers.Main) { navController.toUI(UI.NormalMusic) }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White withNight Color.Black)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical(),
            flingBehavior = rememberOverscrollFlingBehavior { listState }
        ) {
            item("hero") {
                ArtistHero(
                    artistDetail = artistDetail.value,
                    playEnabled = hotSongs.value.isNotEmpty(),
                    onPlay = playAll
                )
            }

            val latest = albums.value.firstOrNull()
            if (latest != null) {
                item("latest_release") {
                    LatestReleaseCard(album = latest) { openAlbum(latest) }
                }
            }

            if (hotSongs.value.isNotEmpty()) {
                item("top_songs") {
                    Column {
                        SectionHeader(title = stringResource(id = R.string.artist_top_songs))
                        TopSongsPager(songs = hotSongs.value)
                    }
                }
            }

            val fullAlbums = albums.value.filter { (it.size ?: Int.MAX_VALUE) > 3 }
            val singles = albums.value.filter { (it.size ?: Int.MAX_VALUE) <= 3 }

            if (fullAlbums.isNotEmpty()) {
                item("albums") {
                    Column {
                        SectionHeader(title = stringResource(id = R.string.page_library_albums))
                        AlbumCardRow(albums = fullAlbums, onClick = openAlbum)
                    }
                }
            }

            if (singles.isNotEmpty()) {
                item("singles_eps") {
                    Column {
                        SectionHeader(title = stringResource(id = R.string.artist_singles_eps))
                        AlbumCardRow(albums = singles, onClick = openAlbum)
                    }
                }
            }

            item("bottom_padding") {
                Spacer(modifier = Modifier.height(150.dp))
            }
        }

        ArtistTopBar(
            title = artistDetail.value?.name ?: "",
            collapsed = collapsed,
            onBack = { navController.popBackStack() }
        )
    }
}

@Composable
private fun ArtistHero(
    artistDetail: NcmArtistDetail?,
    playEnabled: Boolean,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(data = artistDetail?.cover ?: artistDetail?.avatar)
                .crossfade(true)
                .error(R.drawable.songcredits_monogram_person)
                .fallback(R.drawable.songcredits_monogram_person)
                .allowHardware(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFE8E8E8) withNight Color(0xFF1C1C1E))
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.42f)
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = artistDetail?.name ?: "",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (playEnabled) {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = {
                            Vibrator.click(context)
                            onPlay()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(id = R.drawable.button_icon_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestReleaseCard(album: NcmAlbum, onClick: () -> Unit) {
    val dateText = remember(album.publishTime) {
        album.publishTime?.let {
            DateFormat.getDateInstance(DateFormat.LONG).format(Date(it))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        ArtworkImage(
            model = album.picUrl,
            size = 118.dp,
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (dateText != null) {
                Text(
                    text = dateText,
                    fontSize = 13.sp,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(3.dp))
            } else {
                Text(
                    text = stringResource(id = R.string.artist_latest_release),
                    fontSize = 13.sp,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
            Text(
                text = album.name,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black withNight Color.White
            )
            album.size?.let { count ->
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(id = R.string.page_library_album_desc, count),
                    fontSize = 15.sp,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black withNight Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_action_next),
            contentDescription = null,
            tint = (Color.Black withNight Color.White).copy(alpha = 0.3f),
            modifier = Modifier.height(13.dp)
        )
    }
}

@Composable
private fun TopSongsPager(songs: List<YosMediaItem>) {
    val pages = remember(songs) { songs.take(16).chunked(4) }
    val rowState = rememberLazyListState()
    val pageWidth = LocalConfiguration.current.screenWidthDp.dp - 56.dp

    LazyRow(
        state = rowState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = rowState),
        contentPadding = PaddingValues(start = 20.dp, end = 36.dp)
    ) {
        items(pages) { page ->
            Column(modifier = Modifier.width(pageWidth)) {
                page.forEachIndexed { index, song ->
                    TopSongRow(
                        song = song,
                        showDivider = index != page.lastIndex
                    ) {
                        CoroutineScope(Dispatchers.IO).launch {
                            MediaController.prepare(song, songs)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSongRow(song: YosMediaItem, showDivider: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        Vibrator.click(context)
                        onClick()
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .height(64.dp)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkImage(
                model = song.coverUrl ?: song.thumb,
                size = 48.dp,
                cornerRadius = 6.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "",
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black withNight Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artistsName ?: "",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = (Color.Black withNight Color.White).copy(alpha = 0.55f)
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp)
                    .height(0.5.dp)
                    .background((Color.Black withNight Color.White).copy(alpha = 0.12f))
            )
        }
    }
}

@Composable
private fun AlbumCardRow(albums: List<NcmAlbum>, onClick: (NcmAlbum) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album = album, onClick = { onClick(album) })
            Spacer(modifier = Modifier.width(14.dp))
        }
    }
}

@Composable
private fun AlbumCard(album: NcmAlbum, onClick: () -> Unit) {
    val context = LocalContext.current
    val year = remember(album.publishTime) {
        album.publishTime?.let {
            Calendar.getInstance().apply { time = Date(it) }.get(Calendar.YEAR).toString()
        }
    }
    Column(
        modifier = Modifier
            .width(168.dp)
            .clickable(
                onClick = {
                    Vibrator.click(context)
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        ArtworkImage(
            model = album.picUrl,
            size = 168.dp,
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black withNight Color.White
        )
        if (year != null) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = year,
                fontSize = 14.sp,
                color = (Color.Black withNight Color.White).copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun ArtworkImage(model: Any?, size: androidx.compose.ui.unit.Dp, cornerRadius: androidx.compose.ui.unit.Dp) {
    val shape = RoundedCornerShape(cornerRadius)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(data = model)
            .crossfade(true)
            .allowHardware(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background((Color.Black withNight Color.White).copy(alpha = 0.06f), shape)
            .border(
                0.5.dp,
                (Color.Black withNight Color.White).copy(alpha = 0.1f),
                shape
            )
    )
}

@Composable
private fun ArtistTopBar(title: String, collapsed: Boolean, onBack: () -> Unit) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(220),
        label = "artist_topbar_alpha"
    )
    val contentColor = Color.Black withNight Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background((Color.White withNight Color.Black).copy(alpha = bgAlpha))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp)
                    .alpha(bgAlpha)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f * (1f - bgAlpha)))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                    tint = lerp(Color.White, MaterialTheme.colorScheme.primary, bgAlpha),
                    modifier = Modifier
                        .width(10.dp)
                        .height(17.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(contentColor.copy(alpha = 0.15f * bgAlpha))
        )
    }
}
