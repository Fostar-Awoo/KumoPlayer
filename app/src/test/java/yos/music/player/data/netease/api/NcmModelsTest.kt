package yos.music.player.data.netease.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NcmModelsTest {

    @Test
    fun neteaseImageUrlUsesHttpsAndRequestsBoundedArtwork() {
        assertEquals(
            "https://p1.music.126.net/cover.jpg?param=1080y1080",
            "http://p1.music.126.net/cover.jpg".toNcmImageUrl()
        )
    }

    @Test
    fun recommendSongsGenericPayloadDeserializesToConcreteModel() {
        val responseType = object : TypeToken<NcmResponse<NcmRecommendSongsData>>() {}.type
        val response: NcmResponse<NcmRecommendSongsData> = Gson().fromJson(
            """
            {
              "code": 200,
              "data": {
                "dailySongs": [
                  {
                    "id": 123,
                    "name": "Test song",
                    "ar": [],
                    "al": {"id": 456, "name": "Test album", "picUrl": null},
                    "alia": [],
                    "dt": 1000
                  }
                ]
              }
            }
            """.trimIndent(),
            responseType
        )

        assertTrue(response.data is NcmRecommendSongsData)
        assertEquals(123L, response.data?.dailySongs?.single()?.id)
    }
}
