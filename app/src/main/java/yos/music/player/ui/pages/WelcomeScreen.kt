package yos.music.player.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import yos.music.player.data.netease.api.NcmApiClient
import yos.music.player.data.netease.api.NcmRepository

@Composable
fun WelcomeScreen(onFinished: () -> Unit) {
    NcmAccountScreen(onFinished = onFinished)
}

@Composable
fun NcmAccountScreen(
    onFinished: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var baseUrl by remember { mutableStateOf(NcmApiClient.baseUrl) }
    var session by remember { mutableStateOf<NcmRepository.QrLoginSession?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun configureAndLoadQr() {
        val normalized = NcmApiClient.normalizeBaseUrl(baseUrl)
        if (normalized == null) {
            message = "请输入有效的 HTTP 或 HTTPS API 地址"
            return
        }
        baseUrl = normalized
        NcmApiClient.baseUrl = normalized
        loading = true
        session = null
        message = null
        scope.launch {
            NcmRepository.createQrLogin().fold(
                onSuccess = { session = it },
                onFailure = { message = it.message ?: "无法连接到网易云 API" }
            )
            loading = false
        }
    }

    LaunchedEffect(session?.key) {
        val current = session ?: return@LaunchedEffect
        while (isActive && session?.key == current.key) {
            delay(2_000)
            NcmRepository.checkQrLogin(current.key).fold(
                onSuccess = { status ->
                    when (status.code) {
                        800 -> {
                            message = "二维码已过期，请重新获取"
                            session = null
                        }
                        802 -> message = "已扫码，请在网易云音乐中确认"
                        803 -> {
                            status.cookie?.let { NcmApiClient.cookie = it }
                            NcmApiClient.nickname = status.nickname
                            NcmApiClient.avatarUrl = status.avatarUrl
                            NcmApiClient.isLoggedIn = true
                            NcmApiClient.isGuest = false
                            NcmRepository.checkLoginStatus()
                            onFinished()
                        }
                    }
                },
                onFailure = { message = "二维码状态检查失败，正在重试" }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 54.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text("返回")
            }
        }
        Text(
            if (onBack == null) "Kumo Music" else "网易云音乐账号",
            fontSize = if (onBack == null) 36.sp else 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "连接你的网易云音乐服务",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
            fontSize = 17.sp
        )
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API 基础 URL") },
            placeholder = { Text("https://music-api.example.com/") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "请填写兼容 NeteaseCloudMusicApiEnhanced 的服务地址。地址仅保存在本机。",
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .45f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))

        PrimaryAction(
            label = if (session == null) "连接并获取登录二维码" else "重新获取二维码",
            enabled = !loading
        ) {
            if (!loading) configureAndLoadQr()
        }

        if (loading) {
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 3.dp)
        }

        session?.let { qr ->
            Spacer(Modifier.height(28.dp))
            AsyncImage(
                model = qr.image,
                contentDescription = "网易云音乐登录二维码",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("使用网易云音乐 App 扫码登录", fontWeight = FontWeight.Medium)
        }

        message?.let {
            Spacer(Modifier.height(18.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }

        if (onBack != null) {
            Spacer(Modifier.height(18.dp))
            Text(
                if (NcmApiClient.isLoggedIn) {
                    "当前账号：${NcmApiClient.nickname?.takeIf { it.isNotBlank() } ?: "已登录"}"
                } else {
                    "当前状态：游客"
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "以游客身份继续",
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    val normalized = NcmApiClient.normalizeBaseUrl(baseUrl)
                    if (normalized == null) {
                        message = "请先填写有效的 API 地址"
                    } else {
                        NcmApiClient.baseUrl = normalized
                        NcmApiClient.clearLogin()
                        NcmApiClient.isGuest = true
                        onFinished()
                    }
                }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        if (onBack != null && NcmApiClient.isLoggedIn) {
            Text(
                "退出登录",
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        NcmApiClient.clearLogin()
                        NcmApiClient.isGuest = true
                        session = null
                        message = "已退出登录"
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PrimaryAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
    }
}