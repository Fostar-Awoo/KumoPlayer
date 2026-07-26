package yos.music.player.data.netease.api

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 网易云音乐 API 接口定义 (NeteaseCloudMusicApiEnhanced)
 * 文档: https://docs-neteasecloudmusicapi.focalors.ltd/
 */
interface NeteaseMusicApi {

    @GET("login/status")
    suspend fun loginStatus(): Response<NcmResponse<*>>

    @GET("login/qr/key")
    suspend fun qrKey(): Response<NcmResponse<NcmQrKey>>

    @GET("login/qr/create")
    suspend fun qrCreate(@Query("key") key: String, @Query("qrimg") qrimg: Boolean = true): Response<NcmResponse<NcmQrCreate>>

    @GET("login/qr/check")
    suspend fun qrCheck(@Query("key") key: String): Response<NcmQrCheck>

    @GET("logout")
    suspend fun logout(): Response<NcmResponse<*>>

    @GET("recommend/songs")
    suspend fun recommendSongs(): Response<NcmResponse<NcmRecommendSongsData>>

    @GET("recommend/resource")
    suspend fun recommendResource(): Response<NcmRecommendResourceResponse>

    @GET("playlist/detail")
    suspend fun playlistDetail(@Query("id") id: Long): Response<NcmPlaylistDetailResponse>

    @GET("playlist/track/all")
    suspend fun playlistTrackAll(@Query("id") id: Long): Response<NcmPlaylistTracksResponse>

    @GET("user/playlist")
    suspend fun userPlaylists(@Query("uid") uid: Long, @Query("limit") limit: Int = 1000): Response<NcmUserPlaylistsResponse>

    @GET("artist/sublist")
    suspend fun artistSublist(@Query("limit") limit: Int = 1000): Response<NcmArtistSublistResponse>

    @GET("album/sublist")
    suspend fun albumSublist(@Query("limit") limit: Int = 1000): Response<NcmAlbumSublistResponse>

    @GET("user/cloud")
    suspend fun userCloud(@Query("limit") limit: Int = 1000): Response<NcmResponse<NcmCloudData>>

    @GET("artist/detail")
    suspend fun artistDetail(@Query("id") id: Long): Response<NcmResponse<NcmArtistDetailData>>

    @GET("artist/songs")
    suspend fun artistSongs(@Query("id") id: Long, @Query("limit") limit: Int = 50): Response<NcmArtistSongsResponse>

    @GET("artist/album")
    suspend fun artistAlbums(@Query("id") id: Long, @Query("limit") limit: Int = 50): Response<NcmArtistAlbumsResponse>

    @GET("album")
    suspend fun albumDetail(@Query("id") id: Long): Response<NcmAlbumDetailResponse>

    @GET("cloudsearch")
    suspend fun cloudSearch(@Query("keywords") keywords: String, @Query("type") type: Int = 1, @Query("limit") limit: Int = 30): Response<NcmSearchResponse>

    @GET("search/suggest")
    suspend fun searchSuggestMobile(@Query("keywords") keywords: String, @Query("type") type: String = "mobile"): Response<NcmSearchSuggestMobileResponse>

    @GET("song/url/v1")
    suspend fun songUrl(@Query("id") id: String, @Query("level") level: String = "exhigh"): Response<NcmSongUrlResponse>

    @GET("lyric")
    suspend fun lyric(@Query("id") id: Long): Response<NcmLyricResponse>

    @GET("likelist")
    suspend fun likelist(@Query("uid") uid: Long): Response<NcmLikeListResponse>

    @GET("playlist/tracks")
    suspend fun playlistTracksOp(@Query("op") op: String, @Query("pid") pid: String, @Query("tracks") tracks: String): Response<NcmResponse<*>>

    @GET("like")
    suspend fun likeSong(@Query("id") id: Long, @Query("like") like: Boolean = true): Response<NcmResponse<*>>

    @GET("login/cellphone")
    suspend fun loginCellphone(@Query("phone") phone: String, @Query("password") password: String? = null, @Query("captcha") captcha: String? = null): Response<NcmResponse<*>>

    @GET("login")
    suspend fun loginEmail(@Query("email") email: String, @Query("password") password: String): Response<NcmResponse<*>>

    @GET("login/refresh")
    suspend fun loginRefresh(): Response<NcmResponse<*>>
}
