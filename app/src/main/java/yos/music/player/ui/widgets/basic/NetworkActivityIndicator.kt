package yos.music.player.ui.widgets.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import yos.music.player.data.objects.NetworkActivityObject
import yos.music.player.ui.theme.withNight

/**
 * 全局网络活动指示器：有 API 请求进行中且超过一小段时间时，
 * 显示一个小型环形进度指示器，避免让用户误以为应用卡死。
 */
@Composable
fun NetworkActivityIndicator(modifier: Modifier = Modifier) {
    val busy = NetworkActivityObject.isBusy.value
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(busy) {
        if (busy) {
            // 短促请求不闪现指示器
            delay(400)
            visible.value = true
        } else {
            visible.value = false
        }
    }

    AnimatedVisibility(
        visible = visible.value,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .shadow(8.dp, CircleShape)
                .background(Color.White withNight Color(0xFF2C2C2E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
