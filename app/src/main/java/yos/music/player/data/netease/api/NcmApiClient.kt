package yos.music.player.data.netease.api

import com.tencent.mmkv.MMKV
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NcmApiClient {
    private const val KEY_BASE_URL = "ncm_base_url"
    private const val KEY_COOKIE = "ncm_cookie"
    private val mmkv = MMKV.defaultMMKV()

    class NcmCookieJar : CookieJar {
        private var storedCookies: MutableList<Cookie> = mutableListOf()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            storedCookies = cookies.toMutableList()
            mmkv.encode(KEY_COOKIE, cookies.joinToString("; ") { "${it.name}=${it.value}" })
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return storedCookies
        }
    }

    private val cookieJar = NcmCookieJar()

    init {
        val cookieStr = mmkv.decodeString(KEY_COOKIE, "")
        if (!cookieStr.isNullOrEmpty()) {
            val parsed = mutableListOf<Cookie>()
            cookieStr.split(";").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    val kv = trimmed.split("=", limit = 2)
                    if (kv.size == 2 && kv[0].isNotEmpty()) {
                        parsed.add(
                            Cookie.Builder()
                                .name(kv[0])
                                .value(kv[1])
                                .domain("music.163.com")
                                .build()
                        )
                    }
                }
            }
            cookieJar.saveFromResponse("https://music.163.com".toHttpUrl(), parsed)
        }
    }

    var baseUrl: String
        get() = mmkv.decodeString(KEY_BASE_URL, "") ?: ""
        set(value) {
            mmkv.encode(KEY_BASE_URL, normalizeBaseUrl(value) ?: "")
        }

    fun normalizeBaseUrl(value: String): String? {
        val normalized = "${value.trim().trimEnd('/')}/"
        val parsed = normalized.toHttpUrlOrNull() ?: return null
        if (parsed.scheme !in setOf("http", "https")) return null
        return parsed.toString()
    }

    var cookie: String
        get() = mmkv.decodeString(KEY_COOKIE, "") ?: ""
        set(value) {
            mmkv.encode(KEY_COOKIE, value)
            val parsed = mutableListOf<Cookie>()
            value.split(";").forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    val kv = trimmed.split("=", limit = 2)
                    if (kv.size == 2) {
                        parsed.add(
                            Cookie.Builder()
                                .name(kv[0])
                                .value(kv[1])
                                .domain("music.163.com")
                                .build()
                        )
                    }
                }
            }
            cookieJar.saveFromResponse("https://music.163.com".toHttpUrl(), parsed)
        }

    var userId: Long
        get() = mmkv.decodeLong("ncm_user_id", 0L)
        set(value) {
            mmkv.encode("ncm_user_id", value)
        }

    var nickname: String?
        get() = mmkv.decodeString("ncm_nickname", "")
        set(value) {
            mmkv.encode("ncm_nickname", value ?: "")
        }

    var avatarUrl: String?
        get() = mmkv.decodeString("ncm_avatar_url", "")
        set(value) {
            mmkv.encode("ncm_avatar_url", value ?: "")
        }

    var isLoggedIn: Boolean
        get() = mmkv.decodeBool("ncm_logged_in", false)
        set(value) {
            mmkv.encode("ncm_logged_in", value)
        }

    var isGuest: Boolean
        get() = mmkv.decodeBool("ncm_guest", false)
        set(value) {
            mmkv.encode("ncm_guest", value)
        }

    fun isConfigured(): Boolean = baseUrl.isNotEmpty()

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private fun createRetrofit(): Retrofit? {
        val url = baseUrl
        if (url.isEmpty()) return null
        return Retrofit.Builder()
            .baseUrl(url)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T? {
        return createRetrofit()?.create(serviceClass)
    }

    inline fun <reified T> service(): T? {
        return createService(T::class.java)
    }

    fun clearLogin() {
        isLoggedIn = false
        userId = 0L
        nickname = ""
        avatarUrl = ""
        cookie = ""
    }
}
