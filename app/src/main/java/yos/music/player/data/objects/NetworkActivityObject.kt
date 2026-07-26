package yos.music.player.data.objects

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.atomic.AtomicInteger

/**
 * 记录正在进行的 API 请求数量，供全局网络活动指示器观察。
 */
@Stable
object NetworkActivityObject {
    private val activeRequests = AtomicInteger(0)

    val isBusy: MutableState<Boolean> = mutableStateOf(false)

    fun begin() {
        isBusy.value = activeRequests.incrementAndGet() > 0
    }

    fun end() {
        isBusy.value = activeRequests.decrementAndGet() > 0
    }
}
