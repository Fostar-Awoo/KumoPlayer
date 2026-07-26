package yos.music.player.data.netease.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// region Generic Response
data class NcmResponse<T>(
    val code: Int,
    val data: T?,
    val message: String? = null
)

data class NcmRecommendResourceResponse(val code: Int, val recommend: List<NcmPlaylist>?)
data class NcmPlaylistDetailResponse(val code: Int, val playlist: NcmPlaylist?)
data class NcmPlaylistTracksResponse(val code: Int, val songs: List<NcmSong>?)
data class NcmUserPlaylistsResponse(val code: Int, val playlist: List<NcmPlaylist>?)
data class NcmArtistSublistResponse(val code: Int, val data: List<NcmArtist>?)
data class NcmAlbumSublistResponse(val code: Int, val data: List<NcmAlbum>?)
data class NcmArtistSongsResponse(val code: Int, val songs: List<NcmSong>?)
data class NcmArtistAlbumsResponse(val code: Int, val hotAlbums: List<NcmAlbum>?)
data class NcmSearchResponse(val code: Int, val result: NcmSearchResult?)
data class NcmSongUrlResponse(val code: Int, val data: List<NcmSongUrl>?)
data class NcmLyricResponse(
    val code: Int,
    val lrc: NcmLrc?,
    val tlyric: NcmLrc?,
    val romalrc: NcmLrc?
)
data class NcmLikeListResponse(val code: Int, val ids: List<Long>?)
data class NcmAlbumDetailResponse(val code: Int, val album: NcmAlbum?, val songs: List<NcmSong>?)
// endregion

// region Login
@Parcelize
data class NcmQrKey(val unikey: String) : Parcelable

@Parcelize
data class NcmQrCreate(val qrurl: String?, val qrimg: String?) : Parcelable

@Parcelize
data class NcmQrCheck(
    val code: Int,
    val message: String?,
    val cookie: String?,
    val avatarUrl: String?,
    val nickname: String?
) : Parcelable

@Parcelize
data class NcmAccount(
    val id: Long,
    val nickName: String?,
    val avatarUrl: String?
) : Parcelable

@Parcelize
data class NcmProfile(
    val userId: Long,
    val nickname: String?,
    val avatarUrl: String?
) : Parcelable
// endregion

// region Playlist
@Parcelize
data class NcmPlaylist(
    val id: Long,
    val name: String,
    val coverImgUrl: String?,
    val creator: NcmCreator?,
    val trackCount: Int?
) : Parcelable

@Parcelize
data class NcmCreator(val userId: Long, val nickname: String?, val avatarUrl: String?) : Parcelable

@Parcelize
data class NcmRecommendResourceData(val recommend: List<NcmPlaylist>?) : Parcelable

@Parcelize
data class NcmPlaylistDetailData(val playlist: NcmPlaylist?) : Parcelable

@Parcelize
data class NcmPlaylistTrackAllData(val songs: List<NcmSong>?) : Parcelable
// endregion

// region Song
@Parcelize
data class NcmSong(
    val id: Long,
    val name: String,
    val ar: List<NcmArtist>?,
    val al: NcmAlbumInfo?,
    val alia: List<String>?,
    val dt: Long?
) : Parcelable

@Parcelize
data class NcmAlbumInfo(
    val id: Long,
    val name: String?,
    val picUrl: String?
) : Parcelable

@Parcelize
data class NcmRecommendSongsData(val dailySongs: List<NcmSong>?) : Parcelable

@Parcelize
data class NcmSongUrl(val id: Long, val url: String?, val type: String?, val br: Long?) : Parcelable

@Parcelize
data class NcmLyricData(
    val lrc: NcmLrc?,
    val tlyric: NcmLrc?,
    val romalrc: NcmLrc?
) : Parcelable

@Parcelize
data class NcmLrc(val lyric: String?) : Parcelable
// endregion

// region Artist
@Parcelize
data class NcmArtist(
    val id: Long,
    val name: String,
    val picUrl: String?
) : Parcelable

@Parcelize
data class NcmArtistDetailData(val artist: NcmArtistDetail?) : Parcelable

@Parcelize
data class NcmArtistDetail(
    val id: Long,
    val name: String?,
    val cover: String?,
    val avatar: String?,
    val briefDesc: String?,
    val musicSize: Int?,
    val albumSize: Int?
) : Parcelable

@Parcelize
data class NcmArtistSongsData(val songs: List<NcmSong>?) : Parcelable
// endregion

// region Album
@Parcelize
data class NcmAlbum(
    val id: Long,
    val name: String,
    val picUrl: String?,
    val artist: NcmArtist?,
    val publishTime: Long? = null,
    val size: Int? = null
) : Parcelable

@Parcelize
data class NcmArtistAlbumsData(val hotAlbums: List<NcmAlbum>?) : Parcelable

@Parcelize
data class NcmAlbumSublistData(val data: List<NcmAlbum>?) : Parcelable

@Parcelize
data class NcmAlbumDetailData(val album: NcmAlbum?, val songs: List<NcmSong>?) : Parcelable
// endregion

// region Cloud
@Parcelize
data class NcmCloudData(val data: List<NcmCloudSong>?) : Parcelable

@Parcelize
data class NcmCloudSong(
    val simpleSong: NcmSong?,
    val fileName: String?
) : Parcelable
// endregion

// region Search
@Parcelize
data class NcmSearchData(val result: NcmSearchResult?) : Parcelable

@Parcelize
data class NcmSearchResult(
    val songs: List<NcmSong>?,
    val artists: List<NcmArtist>?,
    val albums: List<NcmAlbum>?,
    val playlists: List<NcmPlaylist>?
) : Parcelable
// endregion

// region Like list
@Parcelize
data class NcmLikeListData(val ids: List<Long>?) : Parcelable
// endregion
