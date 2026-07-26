package yos.music.player.data.netease.api

import android.net.Uri
import yos.music.player.data.libraries.YosMediaItem

fun String.toNcmImageUrl(size: Int = 1080): String {
    val httpsUrl = if (startsWith("http://")) "https://${removePrefix("http://")}" else this
    return if ("param=" in httpsUrl) httpsUrl else "$httpsUrl?param=${size}y$size"
}

fun NcmSong.toYosMediaItem(audioUrl: String? = null): YosMediaItem {
    val artists = this.ar?.joinToString(" / ") { it.name }
    val artistIds = this.ar?.map { it.id }
    val artworkUrl = this.al?.picUrl?.toNcmImageUrl()
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
        thumb = artworkUrl?.let(Uri::parse),
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
        coverUrl = artworkUrl,
        audioUrl = audioUrl,
        artistIds = artistIds
    )
}

fun List<NcmSong>.toYosMediaItems(): List<YosMediaItem> = map { it.toYosMediaItem() }
