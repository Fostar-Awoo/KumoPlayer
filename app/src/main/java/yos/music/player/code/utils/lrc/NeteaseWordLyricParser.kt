package yos.music.player.code.utils.lrc

import com.google.gson.JsonParser
import kotlin.math.abs

/** 将网易云 YRC/KLyric 毫秒逐字歌词转换为歌词控件使用的数据结构。 */
object NeteaseWordLyricParser {
    private val linePattern = Regex("^\\[(\\d+),(\\d+)](.*)$")
    private val wordPattern = Regex("[<(](\\d+),(\\d+)(?:,[^)>]*)?[)>]")
    private val lrcTimePattern = Regex("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]")

    fun parse(
        wordLyric: String,
        translationLyric: String? = null
    ): List<List<Pair<Float, String>>> {
        val translations = parseTranslations(translationLyric.orEmpty())
        return wordLyric.lineSequence()
            .mapNotNull(::parseTimedLine)
            .sortedBy { it.first().first }
            .map { words ->
                val lineStart = words.first().first
                val translation = translations
                    .minByOrNull { abs(it.first - lineStart) }
                    ?.takeIf { abs(it.first - lineStart) <= TRANSLATION_TOLERANCE_MS }
                    ?.second
                    .orEmpty()
                // YosLyricView 将最后一个 Pair 作为翻译，其余 Pair 作为主歌词。
                words + (lineStart to translation)
            }
            .toList()
    }

    private fun parseTimedLine(line: String): List<Pair<Float, String>>? {
        val value = line.trim()
        if (value.isEmpty()) return null
        if (value.startsWith('{')) return parseJsonLine(value)

        val lineMatch = linePattern.matchEntire(value) ?: return null
        val lineStart = lineMatch.groupValues[1].toFloatOrNull() ?: return null
        val content = lineMatch.groupValues[3]
        val markers = wordPattern.findAll(content).toList()
        if (markers.isEmpty()) {
            return normalize(content).takeIf(String::isNotBlank)?.let { listOf(lineStart to it) }
        }

        val words = mutableListOf<Pair<Float, String>>()
        val prefix = normalize(content.substring(0, markers.first().range.first))
        if (prefix.isNotBlank()) words += lineStart to prefix

        markers.forEachIndexed { index, marker ->
            val wordStart = marker.groupValues[1].toFloatOrNull() ?: return@forEachIndexed
            val textStart = marker.range.last + 1
            val textEnd = markers.getOrNull(index + 1)?.range?.first ?: content.length
            val text = normalize(content.substring(textStart, textEnd))
            if (text.isNotEmpty()) words += wordStart to text
        }
        if (words.isEmpty()) return null
        // 行首可能早于第一个字；歌词控件使用第一个 Pair 的时间进行定位与跳转。
        if (words.first().first > lineStart) words.add(0, lineStart to "")
        return words
    }

    private fun parseJsonLine(line: String): List<Pair<Float, String>>? = runCatching {
        val root = JsonParser.parseString(line).asJsonObject
        val start = root.get("t")?.asFloat ?: return@runCatching null
        val text = root.getAsJsonArray("c")
            ?.joinToString(separator = "") { part ->
                part.asJsonObject.get("tx")?.asString.orEmpty()
            }
            .orEmpty()
        normalize(text).takeIf(String::isNotBlank)?.let { listOf(start to it) }
    }.getOrNull()

    private fun parseTranslations(lyric: String): List<Pair<Float, String>> = lyric
        .lineSequence()
        .flatMap { line ->
            val value = line.trim()
            when {
                value.startsWith('{') -> parseJsonLine(value)
                    ?.let { sequenceOf(it.first().first to it.joinToString("") { pair -> pair.second }) }
                    ?: emptySequence()

                linePattern.matches(value) -> parseTimedLine(value)
                    ?.let { sequenceOf(it.first().first to it.joinToString("") { pair -> pair.second }) }
                    ?: emptySequence()

                else -> {
                    val matches = lrcTimePattern.findAll(value).toList()
                    val text = normalize(value.substringAfterLast(']', ""))
                    if (matches.isEmpty() || text.isBlank()) emptySequence()
                    else matches.asSequence().mapNotNull { match ->
                        val minutes = match.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
                        val seconds = match.groupValues[2].toFloatOrNull() ?: return@mapNotNull null
                        ((minutes * 60f + seconds) * 1000f) to text
                    }
                }
            }
        }
        .toList()

    private fun normalize(text: String): String = text.replace(Regex("\\s+"), " ")

    private const val TRANSLATION_TOLERANCE_MS = 1_000f
}