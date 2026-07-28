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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import yos.music.player.R
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
    val context = LocalContext.current
    var baseUrl by remember { mutableStateOf(NcmApiClient.baseUrl) }
    var session by remember { mutableStateOf<NcmRepository.QrLoginSession?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun configureAndLoadQr() {
        val normalized = NcmApiClient.normalizeBaseUrl(baseUrl)
        if (normalized == null) {
            message = context.getString(R.string.ncm_account_error_invalid_url)
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
                onFailure = {
                    message = it.message ?: context.getString(R.string.ncm_account_error_connect)
                }
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
                            message = context.getString(R.string.ncm_account_qr_expired)
                            session = null
                        }
                        802 -> message = context.getString(R.string.ncm_account_qr_scanned)
                        803 -> {
                            val persisted = NcmApiClient.persistLogin(
                                loginCookie = status.cookie,
                                loginNickname = status.nickname,
                                loginAvatarUrl = status.avatarUrl
                            )
                            if (persisted) {
                                NcmRepository.checkLoginStatus()
                                onFinished()
                            } else {
                                message = context.getString(R.string.ncm_account_error_missing_cookie)
                                session = null
                            }
                        }
                    }
                },
                onFailure = {
                    message = context.getString(R.string.ncm_account_error_qr_status)
                }
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
                Text(stringResource(R.string.ncm_account_back))
            }
        }
        Text(
            if (onBack == null) "Kumo Music" else stringResource(R.string.ncm_account_title),
            fontSize = if (onBack == null) 36.sp else 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.ncm_account_subtitle),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
            fontSize = 17.sp
        )
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.ncm_account_base_url_label)) },
            placeholder = { Text("https://music-api.example.com/") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(12.dp),
            enabled = !loading
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.ncm_account_base_url_help),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .45f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(24.dp))

        PrimaryAction(
            label = stringResource(
                if (session == null) R.string.ncm_account_connect_qr
                else R.string.ncm_account_refresh_qr
            ),
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
                contentDescription = stringResource(R.string.ncm_account_qr_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ncm_account_scan_prompt), fontWeight = FontWeight.Medium)
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
                    stringResource(
                        R.string.ncm_account_current_account,
                        NcmApiClient.nickname?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.settings_netease_logged_in)
                    )
                } else {
                    stringResource(R.string.ncm_account_current_guest)
                },
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.ncm_account_continue_guest),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    val normalized = NcmApiClient.normalizeBaseUrl(baseUrl)
                    if (normalized == null) {
                        message = context.getString(R.string.ncm_account_error_url_required)
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
                stringResource(R.string.ncm_account_sign_out),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        NcmApiClient.clearLogin()
                        NcmApiClient.isGuest = true
                        session = null
                        message = context.getString(R.string.ncm_account_signed_out)
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