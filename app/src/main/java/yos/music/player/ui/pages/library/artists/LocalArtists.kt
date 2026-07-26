package yos.music.player.ui.pages.library.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import yos.music.player.R
import yos.music.player.data.netease.api.NcmArtist
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.ui.UI
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.Title
import yos.music.player.ui.widgets.basic.YosWrapper

private enum class ArtistsLoadState { Loading, Success, Error }

@Composable
fun LocalArtists(navController: NavController) {
    var artistsList by remember { mutableStateOf<List<NcmArtist>>(emptyList()) }
    var loadState by remember { mutableStateOf(ArtistsLoadState.Loading) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loadState = ArtistsLoadState.Loading
        NcmRepository.getFollowedArtistsResult().fold(
            onSuccess = {
                artistsList = it
                loadState = ArtistsLoadState.Success
            },
            onFailure = { loadState = ArtistsLoadState.Error }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
    ) {
        val searchText = remember("LocalArtists_searchText") {
            mutableStateOf("")
        }

        if (loadState != ArtistsLoadState.Success || artistsList.isEmpty()) {
            Title(
                title = stringResource(id = R.string.page_library_artists), onBack = {
                    navController.popBackStack()
                }
            ) {
                item("artist_status") {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (loadState) {
                            ArtistsLoadState.Loading -> CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 3.dp
                            )
                            ArtistsLoadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            ArtistsLoadState.Success -> Text(
                                text = stringResource(id = R.string.page_library_no_followed_artists),
                                fontSize = 18.sp,
                                modifier = Modifier.alpha(0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            val query = searchText.value.trim()
            val filteredArtists = if (query.isEmpty()) {
                artistsList
            } else {
                artistsList.filter { artist ->
                    artist.name.contains(query, ignoreCase = true)
                }
            }

            Title(
                title = stringResource(id = R.string.page_library_artists), onBack = {
                    navController.popBackStack()
                }
            ) {
                item("SearchField") {
                    val keyboardController = LocalSoftwareKeyboardController.current

                    SearchTextField(
                        text = searchText.value,
                        placeholder = stringResource(id = R.string.page_library_search_artists),
                        onValueChange = {
                            searchText.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(top = 5.dp, bottom = 12.dp),
                        onSearch = {
                            if (searchText.value.isNotEmpty()) {
                                keyboardController?.hide()
                            }
                        })
                }

                itemsIndexed(
                    filteredArtists,
                    key = { _, artist -> artist.id.toString() }
                ) { index, artist ->
                    ArtistItem(artistName = artist.name, artistImageUrl = artist.picUrl) {
                        navController.navigate("${UI.ArtistInfo}/${artist.id}")
                    }

                    key(index) {
                        val needDivider = index < filteredArtists.size - 1
                        if (needDivider) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 81.dp)
                                    .alpha(0.15f)
                                    .height(0.5.dp)
                                    .background(Color.Black withNight Color.White)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.ArtistItem(
    modifier: Modifier = Modifier,
    artistName: String,
    artistImageUrl: String?,
    onClick: () -> Unit
) =
    Row(
        modifier = Modifier
            .animateItem(fadeInSpec = null, fadeOutSpec = null)
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            YosWrapper {
                val shape = CircleShape

                    val density = LocalDensity.current
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(data = artistImageUrl).crossfade(true)
                            .error(R.drawable.songcredits_monogram_person)
                            .placeholder(R.drawable.songcredits_monogram_person)
                            .fallback(R.drawable.songcredits_monogram_person)
                            .allowHardware(true)
                            .precision(Precision.INEXACT)
                            .size(128)
                            .build(),
                        contentDescription = "Artist_Image",
                        contentScale = ContentScale.Crop,
                        modifier = modifier
                            .size(48.dp)
                            .aspectRatio(1f)
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
                                        color = Color.DarkGray.copy(alpha = 0.08f),
                                        style = Stroke(width = 6f)
                                    )
                                    drawOutline(
                                        outline = outline,
                                        color = Color.DarkGray.copy(alpha = 0.4f),
                                        style = Stroke(width = 6f),
                                        blendMode = BlendMode.Overlay
                                    )
                                }
                            }
                    )
            }
        }
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(
                text = artistName,
                fontSize = 16.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_action_next), contentDescription = null,
            modifier = Modifier
                .height(12.dp).padding(end = 8.dp)
                .alpha(0.3f), tint = MaterialTheme.colorScheme.onBackground
        )
    }