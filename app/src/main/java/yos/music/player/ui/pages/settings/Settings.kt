package yos.music.player.ui.pages.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import yos.music.player.R
import yos.music.player.data.libraries.SettingsLibrary
import yos.music.player.data.netease.api.NcmApiClient
import yos.music.player.ui.UI
import yos.music.player.ui.toUI
import yos.music.player.ui.widgets.basic.RoundColumn
import yos.music.player.ui.widgets.basic.Title

@Composable
fun Settings(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.page_settings_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        ListHeader(stringResource(id = R.string.settings_netease))
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_netease_account),
                                desc = if (NcmApiClient.isLoggedIn) {
                                    NcmApiClient.nickname?.takeIf { it.isNotBlank() }
                                        ?: stringResource(id = R.string.settings_netease_logged_in)
                                } else {
                                    stringResource(id = R.string.settings_netease_guest)
                                }
                            ) {
                                navController.toUI(UI.Settings.Account)
                            }
                            Divider()
                            LabelItem(
                                title = stringResource(id = R.string.settings_netease_base_url),
                                desc = NcmApiClient.baseUrl.ifBlank {
                                    stringResource(id = R.string.settings_netease_not_configured)
                                }
                            ) {
                                navController.toUI(UI.Settings.Account)
                            }
                        }

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_performance))
                        RoundColumn {
                            LabelItem(title = stringResource(id = R.string.settings_performance_lyric_title)) {
                                navController.toUI(UI.Settings.LyricSetting)
                            }
                            Divider()
                            LabelItem(title = stringResource(id = R.string.settings_performance_ui_title)) {
                                navController.toUI(UI.Settings.UserInterfaceSetting)
                            }
                            Divider()
                            LabelItem(title = stringResource(id = R.string.settings_performance_notification_title)) {
                                navController.toUI(UI.Settings.NotificationSetting)
                            }
                        }

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_audio))
                        RoundColumn {
                            LabelItem(title = stringResource(id = R.string.settings_audio_exoplayer)) {
                                navController.toUI(UI.Settings.ExoplayerSetting)
                            }
                            Divider()
                            SwitchItem(
                                title = stringResource(id = R.string.settings_audio_fade_in_out),
                                // desc = stringResource(id = R.string.settings_audio_fade_in_out_desc),
                                onClick = { },
                                checkedLambda = { SettingsLibrary.FadePlay }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_audio_fade_in_out_desc))

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_extend))
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_extend_statusbarlyric),
                                // desc = stringResource(id = R.string.settings_extend_statusbarlyric_desc)
                            ) {
                                navController.toUI(UI.Settings.LyricGetter)
                            }
                        }

                        GroupSpacer()
                        ListHeader(stringResource(id = R.string.settings_others))
                        RoundColumn {
                            LabelItem(
                                title = stringResource(id = R.string.settings_others_about),
                                // desc = stringResource(id = R.string.settings_others_about_desc)
                            ) {
                                navController.toUI(UI.Settings.About)
                            }
                        }
                    }
                }

                /*item("blank") {
                    Spacer(modifier = Modifier.height(15.dp))
                }*/
            })
    }


fun safeStartActivity(context: Context, intent: Intent, options: Bundle?) {
    if (intent.resolveActivity(context.packageManager) != null) {
        ContextCompat.startActivity(context, intent, options)
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.tip_intent_resolve_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun startWeb(url: String, context: Context) {
    try {
        val uri: Uri =
            Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        safeStartActivity(context, intent, null)
    } catch (_: Exception) {
    }
}
