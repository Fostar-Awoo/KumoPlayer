package yos.music.player.data.objects

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import yos.music.player.data.netease.api.NcmApiClient
import yos.music.player.data.netease.api.NcmRepository

@Stable
object LikedSongsObject {
    val likedIds: MutableState<Set<Long>> = mutableStateOf(emptySet())

    @Volatile
    private var loaded = false

    fun isLiked(id: Long?): Boolean = id != null && likedIds.value.contains(id)

    suspend fun refresh(force: Boolean = false) {
        if (loaded && !force) return
        val uid = NcmApiClient.userId
        if (uid == 0L) return
        NcmRepository.getLikeList(uid)?.let { ids ->
            likedIds.value = ids.toSet()
            loaded = true
        }
    }

    suspend fun setLiked(id: Long, liked: Boolean): Boolean {
        val before = likedIds.value
        likedIds.value = if (liked) before + id else before - id
        val ok = NcmRepository.likeSong(id, liked).getOrNull()?.code == 200
        if (!ok) {
            likedIds.value = if (liked) likedIds.value - id else likedIds.value + id
        }
        return ok
    }
}
