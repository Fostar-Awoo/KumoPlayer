package yos.music.player.data.netease.api

import com.google.gson.Gson
import kotlin.coroutines.Continuation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.ParameterizedType

class NeteaseMusicApiTest {

    @Test
    fun suspendMethodsRetainParameterizedContinuationSignatures() {
        val apiMethods = NeteaseMusicApi::class.java.declaredMethods
            .filterNot { it.isSynthetic }

        assertTrue("NeteaseMusicApi must declare at least one endpoint", apiMethods.isNotEmpty())
        apiMethods.forEach { method ->
            val continuationType = method.genericParameterTypes.last()
            assertTrue(
                "${method.name} lost its Continuation generic signature",
                continuationType is ParameterizedType
            )
            assertEquals(
                "${method.name} must be compiled as a suspend function",
                Continuation::class.java,
                (continuationType as ParameterizedType).rawType
            )
        }
    }

    @Test
    fun retrofitCanEagerlyParseEveryEndpoint() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .validateEagerly(true)
            .build()

        retrofit.create(NeteaseMusicApi::class.java)
    }
}
