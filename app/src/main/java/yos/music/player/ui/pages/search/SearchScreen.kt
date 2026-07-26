package yos.music.player.ui.pages.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.netease.api.NcmAlbum
import yos.music.player.data.netease.api.NcmArtist
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.NcmSong
import yos.music.player.data.netease.api.toNcmImageUrl
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.theme.withNight
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.SearchTextField
import yos.music.player.ui.widgets.basic.Title

private enum class SearchTab {
    All, Songs, Artists, Albums
}

@Composable
fun SearchScreen(navController: NavController) {
    val searchText = remember { mutableStateOf("") }
    val committed = remember { mutableStateOf(false) }
    val suggestions = remember { mutableStateOf<List<String>>(emptyList()) }
    val bundle = remember { mutableStateOf<NcmRepository.NcmSearchBundle?>(null) }
    val isLoading = remember { mutableStateOf(false) }
    val searchError = remember { mutableStateOf(false) }
    val hasSearched = remember { mutableStateOf(false) }
    val selectedTab = remember { mutableStateOf(SearchTab.All) }

    LaunchedEffect(searchText.value) {
        val keyword = searchText.value.trim()
        if (keyword.isEmpty()) {
            suggestions.value = emptyList()
            bundle.value = null
            isLoading.value = false
            searchError.value = false
            hasSearched.value = false
            selectedTab.value = SearchTab.All
        } else {
            isLoading.value = true
            searchError.value = false
            hasSearched.value = false
            delay(350)
            coroutineScope {
                val suggestionsDeferred = async { NcmRepository.getSearchSuggestions(keyword) }
                val bundleDeferred = async { NcmRepository.searchAll(keyword) }
                suggestions.value = suggestionsDeferred.await()
                val result = bundleDeferred.await()
                bundle.value = result
                searchError.value = result == null
            }
            hasSearched.value = true
            isLoading.value = false
        }
    }

    val commitSearch: (String?) -> Unit = { keyword ->
        if (keyword != null && keyword != searchText.value) {
            searchText.value = keyword
        }
        committed.value = true
        selectedTab.value = SearchTab.All
    }

    Title(title = stringResource(id = R.string.page_search_title), onBack = null) {
        item("search_field") {
            SearchTextField(
                text = searchText.value,
                placeholder = stringResource(id = R.string.search_hint),
                onValueChange = {
                    if (it != searchText.value) {
                        committed.value = false
                    }
                    searchText.value = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 5.dp),
                onSearch = { commitSearch(null) }
            )
        }

        val currentBundle = bundle.value
        val blank = searchText.value.isBlank()

        when {
            blank -> {
                item("empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .alpha(0.3f),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.search_hint),
                            modifier = Modifier.alpha(0.6f),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            isLoading.value -> {
                item("loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            searchError.value -> {
                item("search_status") {
                    SearchStatusText(text = stringResource(id = R.string.search_error))
                }
            }

            !committed.value -> {
                // 搜索建议：联想关键词 + 单曲、艺人与专辑的实体建议
                suggestionItems(
                    suggestions = suggestions.value,
                    bundle = currentBundle,
                    navController = navController,
                    onKeywordClick = { commitSearch(it) }
                )
            }

            else -> {
                // 搜索结果：Tab 切换 全部 / 单曲 / 艺人 / 专辑
                if (hasSearched.value && currentBundle != null &&
                    currentBundle.songs.isEmpty() && currentBundle.artists.isEmpty() && currentBundle.albums.isEmpty()
                ) {
                    item("no_results") {
                        SearchStatusText(text = stringResource(id = R.string.search_no_results))
                    }
                } else if (currentBundle != null) {
                    item("tabs") {
                        SearchTabRow(selectedTab)
                    }
                    resultItems(
                        tab = selectedTab.value,
                        bundle = currentBundle,
                        navController = navController
                    )
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.suggestionItems(
    suggestions: List<String>,
    bundle: NcmRepository.NcmSearchBundle?,
    navController: NavController,
    onKeywordClick: (String) -> Unit
) {
    val keywords = suggestions.take(6)
    itemsWithKeys(keywords, keyPrefix = "suggest_kw") { keyword, _ ->
        SuggestionKeywordRow(keyword = keyword) { onKeywordClick(keyword) }
    }

    if (bundle != null) {
        val entities = buildList<Any> {
            addAll(bundle.songs.take(2))
            addAll(bundle.artists.take(2))
            addAll(bundle.albums.take(2))
        }
        itemsWithKeys(entities, keyPrefix = "suggest_entity") { item, _ ->
            SearchEntityRowForItem(
                item = item,
                queueSongs = bundle.songs,
                navController = navController
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(
    tab: SearchTab,
    bundle: NcmRepository.NcmSearchBundle,
    navController: NavController
) {
    val list: List<Any> = when (tab) {
        SearchTab.All -> interleave(bundle.songs, bundle.artists, bundle.albums)
        SearchTab.Songs -> bundle.songs
        SearchTab.Artists -> bundle.artists
        SearchTab.Albums -> bundle.albums
    }

    if (list.isEmpty()) {
        item("tab_no_results") {
            SearchStatusText(text = stringResource(id = R.string.search_no_results))
        }
    } else {
        itemsWithKeys(list, keyPrefix = "result_${tab.name}") { item, _ ->
            SearchEntityRowForItem(
                item = item,
                queueSongs = bundle.songs,
                navController = navController
            )
        }
    }
}

private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsWithKeys(
    list: List<T>,
    keyPrefix: String,
    crossinline content: @Composable (T, Int) -> Unit
) {
    list.forEachIndexed { index, element ->
        item("${keyPrefix}_$index") {
            content(element, index)
        }
    }
}

private fun interleave(
    songs: List<NcmSong>,
    artists: List<NcmArtist>,
    albums: List<NcmAlbum>
): List<Any> {
    val result = mutableListOf<Any>()
    val maxSize = maxOf(songs.size, artists.size, albums.size)
    for (i in 0 until maxSize) {
        songs.getOrNull(i)?.let { result.add(it) }
        artists.getOrNull(i)?.let { result.add(it) }
        albums.getOrNull(i)?.let { result.add(it) }
    }
    return result
}

@Composable
private fun SearchEntityRowForItem(
    item: Any,
    queueSongs: List<NcmSong>,
    navController: NavController
) {
    when (item) {
        is NcmSong -> {
            SearchEntityRow(
                imageUrl = item.al?.picUrl,
                shape = RoundedCornerShape(6.dp),
                title = item.name,
                subtitle = stringResource(id = R.string.search_type_song) +
                        (item.ar?.takeIf { it.isNotEmpty() }
                            ?.joinToString("、") { it.name }
                            ?.let { " · $it" } ?: "")
            ) {
                val mediaItem = item.toYosMediaItem()
                val queue = queueSongs.map { it.toYosMediaItem() }
                    .ifEmpty { listOf(mediaItem) }
                CoroutineScope(Dispatchers.IO).launch {
                    MediaController.prepare(mediaItem, queue)
                }
            }
        }

        is NcmArtist -> {
            SearchEntityRow(
                imageUrl = item.picUrl,
                shape = CircleShape,
                title = item.name,
                subtitle = stringResource(id = R.string.search_type_artist)
            ) {
                navController.navigate("${UI.ArtistInfo}/${item.id}")
            }
        }

        is NcmAlbum -> {
            SearchEntityRow(
                imageUrl = item.picUrl,
                shape = RoundedCornerShape(6.dp),
                title = item.name,
                subtitle = stringResource(id = R.string.search_type_album) +
                        (item.artist?.name?.let { " · $it" } ?: "")
            ) {
                CoroutineScope(Dispatchers.IO).launch {
                    val songs = NcmRepository.getAlbumSongs(item.id).map { it.toYosMediaItem() }
                    LibraryObject.setTargetListWithTitle(item.name, songs)
                    withContext(Dispatchers.Main) { navController.toUI(UI.NormalMusic) }
                }
            }
        }
    }
}

@Composable
private fun SearchTabRow(selectedTab: MutableState<SearchTab>) {
    val labels = listOf(
        SearchTab.All to stringResource(id = R.string.search_tab_all),
        SearchTab.Songs to stringResource(id = R.string.search_tab_songs),
        SearchTab.Artists to stringResource(id = R.string.search_tab_artists),
        SearchTab.Albums to stringResource(id = R.string.search_tab_albums)
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            labels.forEach { (tab, label) ->
                val selected = selectedTab.value == tab
                Column(
                    modifier = Modifier
                        .clickable(
                            onClick = { selectedTab.value = tab },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            (Color.Black withNight Color.White).copy(alpha = 0.75f)
                        },
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            )
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background((Color.Black withNight Color.White).copy(alpha = 0.12f))
        )
    }
}

@Composable
private fun SuggestionKeywordRow(keyword: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = keyword,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SearchRowDivider(startPadding = 54.dp)
    }
}

@Composable
private fun SearchEntityRow(
    imageUrl: String?,
    shape: Shape,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .height(62.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl?.toNcmImageUrl(300),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background((Color.Black withNight Color.White).copy(alpha = 0.06f), shape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.55f)
                    )
                }
            }
        }
        SearchRowDivider(startPadding = 80.dp)
    }
}

@Composable
private fun SearchRowDivider(startPadding: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .height(0.5.dp)
            .background((Color.Black withNight Color.White).copy(alpha = 0.12f))
    )
}

@Composable
private fun SearchStatusText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            modifier = Modifier.alpha(0.6f),
            fontSize = 15.sp
        )
    }
}
