package yos.music.player.data.netease.api

import android.net.Uri
import yos.music.player.data.libraries.YosMediaItem

fun NcmSong.toYosMediaItem(audioUrl: String? = null): YosMediaItem {
    val artists = this.ar?.joinToString(" / ") { it.name }
    val artistIds = this.ar?.map { it.id }
    return YosMediaItem(
        uri = audioUrl?.let { Uri.parse(it) },
        mediaId = this.id.toString(),
        mimeType = "audio/mpeg",
        title = this.name,
        writer = null,
        compilation = null,
        composer = null,
        artists = artists,
        album = this.al?.name,
        albumArtists = artists,
        thumb = this.al?.picUrl?.let { Uri.parse(it) },
        trackNumber = null,
        discNumber = null,
        genre = null,
        recordingDay = null,
        recordingMonth = null,
        recordingYear = null,
        releaseYear = null,
        artistId = this.ar?.firstOrNull()?.id,
        albumId = this.al?.id,
        genreId = null,
        author = null,
        addDate = null,
        duration = this.dt ?: 0L,
        modifiedDate = null,
        cdTrackNumber = null,
        neteaseId = this.id,
        coverUrl = this.al?.picUrl,
        audioUrl = audioUrl,
        artistIds = artistIds
    )
}

fun List<NcmSong>.toYosMediaItems(): List<YosMediaItem> = map { it.toYosMediaItem() }
