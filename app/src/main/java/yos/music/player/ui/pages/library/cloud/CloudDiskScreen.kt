package yos.music.player.ui.pages.library.cloud

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yos.music.player.R
import yos.music.player.code.MediaController
import yos.music.player.data.libraries.YosMediaItem
import yos.music.player.data.netease.api.NcmRepository
import yos.music.player.data.netease.api.toYosMediaItem
import yos.music.player.ui.pages.library.MusicList
import yos.music.player.ui.widgets.basic.Title

@Composable
fun CloudDiskScreen(navController: NavController) {
    val cloudState = remember { mutableStateOf<List<YosMediaItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (cloudState.value.isEmpty()) {
            cloudState.value = NcmRepository.getCloudSongs().map { it.toYosMediaItem() }
        }
    }

    val targetList = cloudState.value

    Column(Modifier.fillMaxSize()) {
        Title(title = stringResource(id = R.string.page_library_cloud_disk), onBack = {
            navController.popBackStack()
        }) {
            if (targetList.isEmpty()) {
                item("empty") {
                    androidx.compose.material3.Text(
                        text = stringResource(id = R.string.tip_no_song),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                itemsIndexed(targetList, key = { index, item -> item.neteaseId ?: index }) { _, music ->
                    MusicList(music) {
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            MediaController.prepare(music, targetList)
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 88.dp)
                            .alpha(0.15f)
                            .height(0.5.dp)
                            .background(Color.Black.copy(alpha = 0.15f))
                    )
                }
            }
        }
    }
}
