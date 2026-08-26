package com.deliriousvoid.openvkmatcha

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.deliriousvoid.openvkmatcha.ui.util.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MarkdownParserTest {

    private val primaryColor = Color.Blue

    @Test
    fun testMarkdownBold() {
        val text = "**Bold Text**"
        val annotated = MarkdownParser.parse(text, primaryColor)
        assertEquals("Bold Text", annotated.text.trim())
    }

    @Test
    fun testColorTags() {
        val text = "<red>Red Text<red>"
        val annotated = MarkdownParser.parse(text, primaryColor)
        assertEquals("Red Text", annotated.text.trim())
        
        // Check if color span is applied
        val colorAnnotations = annotated.spanStyles.filter { it.item.color == Color(0xFFE64646) }
        assertEquals(1, colorAnnotations.size)
    }

    @Test
    fun testHexColorTags() {
        val text = "<00ff00>Green Text<00ff00>"
        val annotated = MarkdownParser.parse(text, primaryColor)
        assertEquals("Green Text", annotated.text.trim())
        
        val colorAnnotations = annotated.spanStyles.filter { it.item.color == Color(0xFF00FF00) }
        assertEquals(1, colorAnnotations.size)
    }

    @Test
    fun testNestedTags() {
        val text = "<red>**Bold Red**<red>"
        val annotated = MarkdownParser.parse(text, primaryColor)
        assertEquals("Bold Red", annotated.text.trim())
        
        // In the new implementation, styles should be present
        val colorAnnotations = annotated.spanStyles.filter { it.item.color == Color(0xFFE64646) }
        assertEquals(1, colorAnnotations.size)
        
        val boldAnnotations = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldAnnotations.size)
    }

    @Test
    fun testSplitColorTags() {
        // This simulates how Commonmark might split the text
        val text = "<red>text</red>"
        val annotated = MarkdownParser.parse(text, primaryColor)
        assertEquals("text", annotated.text.trim())
        
        val colorAnnotations = annotated.spanStyles.filter { it.item.color == Color(0xFFE64646) }
        assertEquals(1, colorAnnotations.size)
    }

    @Test
    fun testVkMentions() {
        val text = "[id1|Durov]"
        val annotated = MarkdownParser.parse(text, primaryColor)
        assertEquals("Durov", annotated.text.trim())
        
        val mentions = annotated.getStringAnnotations("MENTION", 0, 5)
        assertEquals(1, mentions.size)
        assertEquals("1", mentions[0].item)
    }
}
