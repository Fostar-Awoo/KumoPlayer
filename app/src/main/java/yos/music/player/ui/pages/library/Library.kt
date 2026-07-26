package yos.music.player.ui.pages.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.PersonCropCircle
import yos.music.player.R
import yos.music.player.data.objects.LibraryObject
import yos.music.player.ui.UI
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.Title

@Composable
fun Library(navController: NavController) =
    Column(
        Modifier
            .fillMaxSize()
        /*.statusBarsPadding()*/
    ) {
        Title(
            title = stringResource(id = R.string.page_library_title),
            rightIcon = CupertinoIcons.Default.PersonCropCircle,
            onRightIcon = {
                navController.toUI(UI.Settings.Main)
            }
        ) {
            item("Library") {
                Column(
                    Modifier
                        .fillMaxSize(),
                ) {
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_playlists),
                        label = stringResource(
                            id = R.string.page_library_playlists
                        )
                    ) {
                        navController.toUI(UI.PlayLists)
                    }
                    LibraryDivider()
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_artists),
                        label = stringResource(
                            id = R.string.page_library_artists
                        )
                    ) {
                        navController.toUI(UI.LocalArtists)
                    }
                    LibraryDivider()
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_album),
                        label = stringResource(
                            id = R.string.page_library_albums
                        )
                    ) {
                        navController.toUI(UI.LocalAlbums)
                    }
                    LibraryDivider()
                    SmallLabelItem(
                        icon = painterResource(id = R.drawable.ic_library_link_icon_songs),
                        label = stringResource(
                            id = R.string.page_library_cloud_disk
                        )
                    ) {
                        navController.toUI(UI.CloudDisk)
                    }
                    LibraryDivider()
                }
            }
        }
    }