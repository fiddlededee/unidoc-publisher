package writer

import model.Document
import model.Heading
import model.Paragraph
import org.approvaltests.Approvals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import verify

class TestFodtWriter {

    @Test
    fun testCustomWriterStyle() {
        val doc = Document().apply {
            p { +"Paragraph before" }
            h(1) { roles("foo"); +" (Some more text)" }
            p { +"Paragraph after" }
            p {
                span {
                    +"before"
                    span { roles("bar"); +" (Some more text)" }
                    +"after"
                }
            }
        }
        val odtStyleList = OdtStyleList(
            OdtCustomWriter {
                if (it.roles.contains("foo"))
                    preOdNode.apply {
                        "text:p" {
                            -"Some text"
                            process(it)
                        }
                    }
            },
            OdtCustomWriter {
                if (it.roles.contains("bar"))
                    preOdNode.apply {
                        "text:span" {
                            -"Some text"
                            process(it)
                        }
                    }
            }
        )
        OdWriter(odtStyleList = odtStyleList)
            .apply { doc.write(this) }
            .preOdNode
            .toString()
            .apply { Approvals.verify(this) }
    }


    @Test
    fun testTestStyle() {
        val node1 = xml("root")
        node1.textProperties { attribute("foo", "bar") }
        val node2 = xml("root") { "style:text-properties" {} }
        node2.textProperties { attribute("foo", "bar") }

        println(node1)
        assertEquals(node1.toString(), node2.toString())
    }

    @Test
    fun testSimpleModel() {
        val basicOdtStyle = OdtStyleList(
            OdtStyle {
                if (it !is Heading) return@OdtStyle
                attribute("text:style-name", "Heading ${it.level}")
                "style:text-properties" {
                    attribute("fo:font-size", "${15 - it.level}pt")
                }
            },
            OdtStyle {
                if (it !is Paragraph) return@OdtStyle
                attribute("text:style-name", "Body Text")
            }
        )
        val doc = Document().apply {
            h(1) { +"Heading 1" }
            p { +"Some text" }
            h(1) { +"Heading 1" }
        }

        OdWriter(
            odtStyleList = basicOdtStyle
        ).apply { doc.write(this) }
            .preOdNode
            .toString()
            .apply { Approvals.verify(this) }
    }
}