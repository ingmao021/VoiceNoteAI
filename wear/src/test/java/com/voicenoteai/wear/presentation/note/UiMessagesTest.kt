package com.voicenoteai.wear.presentation.note

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Los textos viven en strings.xml: se leen del fichero porque las pruebas de JVM no tienen Context. */
class UiMessagesTest {
    private val strings: Map<String, String> by lazy {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    @Test
    fun `capture error messages have at most three words`() {
        val messages = strings.filterKeys { it.startsWith("error_capture_") }
        assertTrue(messages.isNotEmpty())
        messages.forEach { (name, text) ->
            assertTrue("$name: «$text»", text.split(" ").size <= 3)
        }
    }
}
