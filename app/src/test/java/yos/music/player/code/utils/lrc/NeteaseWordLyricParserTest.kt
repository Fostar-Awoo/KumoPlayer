package yos.music.player.code.utils.lrc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeteaseWordLyricParserTest {

    @Test
    fun parsesYrcWordsAndMergesLrcTranslation() {
        val entries = NeteaseWordLyricParser.parse(
            wordLyric = "[28480,11820](28480,160,0)我(28640,420,0)带(29060,230,0)着",
            translationLyric = "[00:28.50]I carry it with me"
        )

        assertEquals(1, entries.size)
        assertEquals(listOf("我", "带", "着"), entries.single().dropLast(1).map { it.second })
        assertEquals(listOf(28480f, 28640f, 29060f), entries.single().dropLast(1).map { it.first })
        assertEquals("I carry it with me", entries.single().last().second)
    }

    @Test
    fun parsesJsonCreditLinesReturnedByLyricNew() {
        val entries = NeteaseWordLyricParser.parse(
            """{"t":1000,"c":[{"tx":"作曲: "},{"tx":"赵雷"}]}"""
        )

        assertEquals(1, entries.size)
        assertEquals(1000f, entries.single().first().first)
        assertEquals("作曲: 赵雷", entries.single().dropLast(1).joinToString("") { it.second })
        assertTrue(entries.single().last().second.isEmpty())
    }

    @Test
    fun supportsAngleBracketKLyricMarkersAndKeepsLineStart() {
        val entries = NeteaseWordLyricParser.parse(
            "[1000,1000]<1100,300,0>Hello <1400,400,0>world"
        )

        assertEquals(1000f, entries.single().first().first)
        assertEquals("", entries.single().first().second)
        assertEquals("Hello world", entries.single().dropLast(1).joinToString("") { it.second })
    }

    @Test
    fun ignoresMetadataAndMalformedLines() {
        val entries = NeteaseWordLyricParser.parse(
            """
            [ti:Song]
            malformed
            [2000,500](2000,500,0)有效
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("有效", entries.single().dropLast(1).single().second)
    }
}