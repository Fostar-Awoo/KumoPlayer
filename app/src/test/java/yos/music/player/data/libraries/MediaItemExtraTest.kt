package yos.music.player.data.libraries

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaItemExtraTest {
    @Test
    fun splitsArtistsUsingChineseAndAsciiPunctuation() {
        assertEquals(
            listOf("艺人甲", "艺人乙", "艺人丙", "艺人丁"),
            "艺人甲，艺人乙, 艺人丙、艺人丁".toMultipleArtists()
        )
    }

    @Test
    fun trimsArtistsAndDropsEmptySegments() {
        assertEquals(
            listOf("Artist A", "Artist B"),
            " Artist A ，,、 Artist B ".toMultipleArtists()
        )
    }

    @Test
    fun retainsPreviouslySupportedArtistSeparators() {
        assertEquals(
            listOf("Artist A", "Artist B", "Artist C", "Artist D"),
            "Artist A / Artist B & Artist C；Artist D".toMultipleArtists()
        )
    }
}