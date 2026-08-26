package com.deliriousvoid.openvkmatcha.ui.util

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.commonmark.node.*
import org.commonmark.parser.Parser

object MarkdownParser {
    private val parser = Parser.builder().build()

    private fun getColorMap(): Map<String, Color> {
        val lm = com.deliriousvoid.openvkmatcha.OpenVKMatchaApp.instance.languageManager
        val map = mutableMapOf(
            "red" to Color(0xFFE64646),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_red).lowercase() to Color(0xFFE64646),
            "orange" to Color(0xFFFF9800),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_orange).lowercase() to Color(0xFFFF9800),
            "yellow" to Color(0xFFFFEB3B),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_yellow).lowercase() to Color(0xFFFFEB3B),
            "green" to Color(0xFF4BB34B),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_green).lowercase() to Color(0xFF4BB34B),
            "blue" to Color(0xFF2196F3),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_blue).lowercase() to Color(0xFF2196F3),
            "purple" to Color(0xFF9C27B0),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_purple).lowercase() to Color(0xFF9C27B0),
            "gray" to Color(0xFF818C99),
            lm.getStringOrDefault(com.deliriousvoid.openvkmatcha.R.string.color_gray).lowercase() to Color(0xFF818C99)
        )
        // Add hardcoded Russian fallbacks for compatibility
        map["красный"] = Color(0xFFE64646)
        map["оранжевый"] = Color(0xFFFF9800)
        map["жёлтый"] = Color(0xFFFFEB3B)
        map["желтый"] = Color(0xFFFFEB3B)
        map["зелёный"] = Color(0xFF4BB34B)
        map["зеленый"] = Color(0xFF4BB34B)
        map["синий"] = Color(0xFF2196F3)
        map["фиолетовый"] = Color(0xFF9C27B0)
        map["серый"] = Color(0xFF818C99)
        return map
    }

    fun parse(text: String, primaryColor: Color): AnnotatedString {
        val root = parser.parse(text)
        return buildAnnotatedString {
            val visitor = MarkdownVisitor(this, primaryColor, getColorMap())
            root.accept(visitor)
            visitor.popAll()
        }
    }

    private class MarkdownVisitor(
        private val builder: AnnotatedString.Builder,
        private val primaryColor: Color,
        private val colorMap: Map<String, Color>
    ) : AbstractVisitor() {

        private val activeColorTags = mutableListOf<String>()

        fun popAll() {
            while (activeColorTags.isNotEmpty()) {
                builder.pop()
                activeColorTags.removeAt(activeColorTags.size - 1)
            }
        }

        private val tagRegex: Regex by lazy {
            val colorNames = colorMap.keys.joinToString("|") { Regex.escape(it) }
            Regex(
                "<(/)?($colorNames|[0-9a-fA-F]{6})>|" +
                "\\[(id|club|public)(\\d+)\\|(.*?)\\]|" +
                "\\[(.*?)\\|@([\\w\\d._-]+)\\]|" +
                "(?:https?://|www\\.)[\\w\\d.\\-]+[\\w\\d/\\-?%&=._~#+!]*|(?:[\\w\\d\\-]+\\.)+[a-z]{2,10}(?:/[\\w\\d/\\-?%&=._~#+!]*)?",
                RegexOption.IGNORE_CASE
            )
        }

        private fun processText(text: String) {
            var currentIndex = 0
            tagRegex.findAll(text).forEach { match ->
                builder.append(text.substring(currentIndex, match.range.first))

                val colorSlash = match.groupValues[1]
                val colorName = match.groupValues[2]?.lowercase()

                val vkIdType = match.groupValues[3]
                val vkId = match.groupValues[4]
                val vkIdName = match.groupValues[5]

                val vkShortName = match.groupValues[6]
                val vkShortId = match.groupValues[7]

                when {
                    !colorName.isNullOrEmpty() -> {
                        val isClosing = colorSlash == "/"
                        if (isClosing) {
                            if (activeColorTags.lastOrNull() == colorName) {
                                builder.pop()
                                activeColorTags.removeAt(activeColorTags.size - 1)
                            }
                        } else {
                            if (activeColorTags.lastOrNull() == colorName) {
                                builder.pop()
                                activeColorTags.removeAt(activeColorTags.size - 1)
                            } else {
                                val color = colorMap[colorName] ?: try {
                                    Color(android.graphics.Color.parseColor("#$colorName"))
                                } catch (_: Exception) { null }

                                if (color != null) {
                                    builder.pushStyle(SpanStyle(color = color))
                                    activeColorTags.add(colorName)
                                } else {
                                    builder.append(match.value)
                                }
                            }
                        }
                    }
                    !vkIdType.isNullOrEmpty() -> {
                        val annotation = if (vkIdType == "id") vkId else "-$vkId"
                        builder.pushStringAnnotation(tag = "MENTION", annotation = annotation)
                        builder.withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                            append(vkIdName)
                        }
                        builder.pop()
                    }
                    !vkShortId.isNullOrEmpty() -> {
                        builder.pushStringAnnotation(tag = "PROFILE", annotation = vkShortId)
                        builder.withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                            append(vkShortName)
                        }
                        builder.pop()
                    }
                    else -> {
                        handleUrl(match.value, match.value)
                    }
                }
                currentIndex = match.range.last + 1
            }
            builder.append(text.substring(currentIndex))
        }

        private fun handleUrl(urlText: String, displayText: String) {
            val url = if (!urlText.startsWith("http")) "https://$urlText" else urlText
            val uri = try { Uri.parse(url) } catch (_: Exception) { null }
            val path = uri?.path?.trim('/') ?: ""
            val host = uri?.host ?: ""

            val isOvk = host.contains("openvk.org") || host.contains("openvk.uk") || host.contains("openvk.su") || host.contains("ovk.to")

            when {
                isOvk && path.startsWith("id") && path.substring(2).all { it.isDigit() } -> {
                    val id = if (path == "id0") "0" else path.substring(2)
                    builder.pushStringAnnotation(tag = "PROFILE", annotation = id)
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
                isOvk && (path.startsWith("club") || path.startsWith("public")) && path.substring(if (path.startsWith("club")) 4 else 6).all { it.isDigit() } -> {
                    val id = if (path.startsWith("club")) path.substring(4) else path.substring(6)
                    builder.pushStringAnnotation(tag = "PROFILE", annotation = "-$id")
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
                isOvk && path.startsWith("playlist") -> {
                    val idStr = path.removePrefix("playlist").trimStart('/')
                    builder.pushStringAnnotation(tag = "PLAYLIST", annotation = idStr)
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
                isOvk && path.startsWith("wall") -> {
                    val idStr = path.substring(4)
                    builder.pushStringAnnotation(tag = "WALL", annotation = idStr)
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
                isOvk && path.startsWith("audios") -> {
                    val id = path.substring(6)
                    builder.pushStringAnnotation(tag = "MUSIC", annotation = id)
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
                isOvk && path.isNotBlank() && !path.contains("/") -> {
                    builder.pushStringAnnotation(tag = "PROFILE", annotation = path)
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
                else -> {
                    builder.pushStringAnnotation(tag = "URL", annotation = url)
                    builder.withStyle(SpanStyle(color = primaryColor)) { append(displayText) }
                    builder.pop()
                }
            }
        }

        override fun visit(text: Text) {
            processText(text.literal)
        }

        override fun visit(htmlInline: HtmlInline) {
            processText(htmlInline.literal)
        }

        override fun visit(htmlBlock: HtmlBlock) {
            processText(htmlBlock.literal)
        }

        override fun visit(emphasis: Emphasis) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                visitChildren(emphasis)
            }
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                visitChildren(strongEmphasis)
            }
        }

        override fun visit(code: Code) {
            builder.withStyle(SpanStyle(background = Color.LightGray.copy(alpha = 0.3f), fontWeight = FontWeight.Medium)) {
                append(code.literal)
            }
        }

        override fun visit(link: Link) {
            builder.pushStringAnnotation(tag = "URL", annotation = link.destination)
            builder.withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)) {
                visitChildren(link)
            }
            builder.pop()
        }

        override fun visit(paragraph: Paragraph) {
            visitChildren(paragraph)
            if (paragraph.next != null) builder.append("\n\n")
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append("\n")
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append("\n")
        }

        override fun visit(heading: Heading) {
            val fontSize = when (heading.level) {
                1 -> 24.sp
                2 -> 20.sp
                else -> 18.sp
            }
            builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize)) {
                visitChildren(heading)
            }
            if (heading.next != null) builder.append("\n\n")
        }

        override fun visit(blockQuote: BlockQuote) {
            builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                builder.append("> ")
                visitChildren(blockQuote)
            }
            if (blockQuote.next != null) builder.append("\n\n")
        }
    }
}
