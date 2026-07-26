package yos.music.player.data.netease.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NcmRepository {

    private const val OUTER_SONG_URL = "https://music.163.com/song/media/outer/url?id=%d.mp3"

    data class QrLoginSession(val key: String, val image: ByteArray)

    private fun api(): NeteaseMusicApi? = NcmApiClient.createService(NeteaseMusicApi::class.java)

    suspend fun <T> safeCall(block: suspend NeteaseMusicApi.() -> retrofit2.Response<T>): Result<T?> =
        withContext(Dispatchers.IO) {
            try {
                val api = api() ?: return@withContext Result.failure(IllegalStateException("API not configured"))
                val resp = block(api)
                if (resp.isSuccessful) {
                    Result.success(resp.body())
                } else {
                    Result.failure(Exception("HTTP ${resp.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getRecommendSongs(): List<NcmSong> {
        val res = safeCall { recommendSongs() }
        return res.getOrNull()?.data?.dailySongs ?: emptyList()
    }

    suspend fun getRecommendPlaylists(): List<NcmPlaylist> {
        val res = safeCall { recommendResource() }
        return res.getOrNull()?.recommend ?: emptyList()
    }

    suspend fun getUserPlaylists(uid: Long): List<NcmPlaylist> {
        val res = safeCall { userPlaylists(uid) }
        return res.getOrNull()?.playlist ?: emptyList()
    }

    suspend fun getFollowedArtists(): List<NcmArtist> {
        return getFollowedArtistsResult().getOrDefault(emptyList())
    }

    suspend fun getSubscribedAlbums(): List<NcmAlbum> {
        return getSubscribedAlbumsResult().getOrDefault(emptyList())
    }

    suspend fun getFollowedArtistsResult(): Result<List<NcmArtist>> {
        return safeCall { artistSublist() }.mapCatching { response ->
            requireNotNull(response) { "关注艺人响应为空" }
            check(response.code == 200) { "关注艺人请求失败（${response.code}）" }
            response.data.orEmpty()
        }
    }

    suspend fun getSubscribedAlbumsResult(): Result<List<NcmAlbum>> {
        return safeCall { albumSublist() }.mapCatching { response ->
            requireNotNull(response) { "收藏专辑响应为空" }
            check(response.code == 200) { "收藏专辑请求失败（${response.code}）" }
            response.data.orEmpty()
        }
    }

    suspend fun getCloudSongs(): List<NcmSong> {
        val res = safeCall { userCloud() }
        return res.getOrNull()?.data?.data?.mapNotNull { it.simpleSong } ?: emptyList()
    }

    suspend fun getPlaylistDetail(id: Long): NcmPlaylist? {
        val res = safeCall { playlistDetail(id) }
        return res.getOrNull()?.playlist
    }

    suspend fun getPlaylistTracks(id: Long): List<NcmSong> {
        val res = safeCall { playlistTrackAll(id) }
        return res.getOrNull()?.songs ?: emptyList()
    }

    /** 优先通过增强 API 获取实际 CDN 地址，API 不可达时使用网易云歌曲外链。 */
    suspend fun getSongUrl(id: Long): String {
        return getSongUrls(listOf(id)).getValue(id)
    }

    suspend fun getSongUrls(ids: List<Long>): Map<Long, String> {
        val distinctIds = ids.distinct()
        if (distinctIds.isEmpty()) return emptyMap()

        // outer/url 在部分地区会重定向到 /404。优先采用用户配置的增强 API
        // 在服务端解析出的实际 CDN 地址；API 不可用时仍保留 outer/url 兜底。
        var apiResponded = false
        val apiUrls = distinctIds.chunked(50).fold(mutableMapOf<Long, String>()) { result, chunk ->
            safeCall { songUrl(chunk.joinToString(",")) }.getOrNull()?.let { response ->
                apiResponded = true
                response.data.orEmpty()
                    .mapNotNull { item -> item.url?.let { item.id to it } }
                    .forEach { (id, url) -> result[id] = url }
            }
            result
        }
        return if (apiResponded) apiUrls else distinctIds.associateWith { OUTER_SONG_URL.format(it) }
    }

    suspend fun getLyric(id: Long): NcmLyricData? {
        val res = safeCall { lyric(id) }
        return res.getOrNull()?.let { NcmLyricData(it.lrc, it.tlyric, it.romalrc) }
    }

    suspend fun getArtistDetail(id: Long): NcmArtistDetail? {
        val res = safeCall { artistDetail(id) }
        return res.getOrNull()?.data?.artist
    }

    suspend fun getArtistSongs(id: Long): List<NcmSong> {
        val res = safeCall { artistSongs(id) }
        return res.getOrNull()?.songs ?: emptyList()
    }

    suspend fun getArtistAlbums(id: Long): List<NcmAlbum> {
        val res = safeCall { artistAlbums(id) }
        return res.getOrNull()?.hotAlbums ?: emptyList()
    }

    suspend fun getAlbumSongs(id: Long): List<NcmSong> {
        val res = safeCall { albumDetail(id) }
        return res.getOrNull()?.songs ?: emptyList()
    }

    suspend fun search(keyword: String, type: Int = 1): NcmSearchResult? {
        val res = safeCall { cloudSearch(keyword, type) }
        return res.getOrNull()?.result
    }

    suspend fun getLikeList(uid: Long): List<Long>? {
        val res = safeCall { likelist(uid) }
        return res.getOrNull()?.ids
    }

    suspend fun addSongToPlaylist(pid: String, trackId: Long): Result<NcmResponse<*>?> {
        return safeCall { playlistTracksOp("add", pid, trackId.toString()) }
    }

    suspend fun likeSong(id: Long, like: Boolean = true): Result<NcmResponse<*>?> {
        return safeCall { likeSong(id, like) }
    }

    suspend fun createQrLogin(): Result<QrLoginSession> {
        val keyResponse = safeCall { qrKey() }.getOrElse { return Result.failure(it) }
        val key = keyResponse?.data?.unikey
            ?: return Result.failure(IllegalStateException("API 未返回二维码登录密钥"))
        val qrResponse = safeCall { qrCreate(key) }.getOrElse { return Result.failure(it) }
        val encodedImage = qrResponse?.data?.qrimg
            ?: return Result.failure(IllegalStateException("API 未返回二维码图片，请确认服务支持 qrimg"))
        val image = runCatching {
            val base64 = encodedImage.substringAfter("base64,", encodedImage).trim()
            Base64.decode(base64, Base64.DEFAULT)
        }.getOrElse {
            return Result.failure(IllegalStateException("API 返回的二维码图片格式无效", it))
        }
        if (image.isEmpty()) {
            return Result.failure(IllegalStateException("API 返回的二维码图片为空"))
        }
        return Result.success(QrLoginSession(key, image))
    }

    suspend fun checkQrLogin(key: String): Result<NcmQrCheck> {
        val result = safeCall { qrCheck(key) }
        return result.fold(
            onSuccess = { response ->
                response?.let(Result.Companion::success)
                    ?: Result.failure(IllegalStateException("二维码状态响应为空"))
            },
            onFailure = Result.Companion::failure
        )
    }

    suspend fun checkLoginStatus(): Boolean {
        return try {
            val api = api() ?: return false
            val resp = withContext(Dispatchers.IO) { api.loginStatus() }
            if (resp.isSuccessful) {
                val profile = resp.body()?.data
                if (profile != null) {
                    @Suppress("UNCHECKED_CAST")
                    val map = profile as? Map<String, Any>
                    val account = map?.get("account") as? Map<String, Any>
                    (account?.get("id") as? Number)?.toLong()?.let { NcmApiClient.userId = it }
                }
                NcmApiClient.userId != 0L
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
