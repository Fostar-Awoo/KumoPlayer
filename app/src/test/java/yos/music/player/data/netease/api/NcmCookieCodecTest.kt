package yos.music.player.data.netease.api

import org.junit.Assert.assertEquals
import org.junit.Test

class NcmCookieCodecTest {
    @Test
    fun parsesCookieValuesContainingEqualsSigns() {
        assertEquals(
            linkedMapOf("MUSIC_U" to "token==", "__csrf" to "csrf-token"),
            NcmCookieCodec.parse(" MUSIC_U=token==; __csrf=csrf-token ")
        )
    }

    @Test
    fun responseCookiesMergeWithoutRemovingLoginCookie() {
        assertEquals(
            "MUSIC_U=login-token; __csrf=new-csrf; NMTID=tracking-id",
            NcmCookieCodec.merge(
                current = "MUSIC_U=login-token; __csrf=old-csrf",
                incoming = "__csrf=new-csrf; NMTID=tracking-id"
            )
        )
    }

    @Test
    fun normalizationDropsMalformedEntriesAndDuplicateNames() {
        assertEquals(
            "MUSIC_U=new-token; __csrf=csrf-token",
            NcmCookieCodec.normalize(
                "invalid; MUSIC_U=old-token; __csrf=csrf-token; MUSIC_U=new-token"
            )
        )
    }
}