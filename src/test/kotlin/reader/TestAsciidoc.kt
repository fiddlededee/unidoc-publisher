package reader

import common.asciidocAsHtml
import common.htmlMarkupAsHtmlNode
import common.parseAsciidoc
import model.Document
import model.Node
import model.Text
import org.approvaltests.Approvals
import org.junit.jupiter.api.Test

class TestAsciidoc {
    @Test
    fun testSimpleAsciidoc() {
        """
            = Sample Asciidoc 
           
            == Heading 1
            
            [.unnecessary]
            This paragraph should not appear in result
            
            This paragraph should appear in result
            
            == Heading 2
            
            Some more paragraph
        """
            .trimIndent()
            .asciidocAsHtml()
            .apply { println(this) }
            .htmlMarkupAsHtmlNode()
            .selectAtXpath("/html/body")!!
            .parseAsciidoc().apply {
                this.descendant { it.roles.contains("unnecessary") }
                    .forEach { it.remove() }
                this.descendant{it.id == "footer"}
                    .forEach { it.remove() }
            }
            .output()
            .apply { Approvals.verify(this) }
    }
}



fun Document.output(): String {
    val astAsText = StringBuilder()
    fun outputNode(node: Node, level: Int): String {
        if (node is Text) {
            astAsText.appendLine("${" ".repeat(level * 2)}text: [${node.text}]")
        } else {
            astAsText.appendLine(
                "${" ".repeat(level * 2)}${node::class.java.simpleName} id=${node.id}" +
                        " roles=${node.roles.joinToString(",")}"
            )
            node.children().forEach {
                outputNode(it, level + 1)
            }
        }
        return astAsText.toString()
    }
    outputNode(this, 0)
    return astAsText.toString()
}
