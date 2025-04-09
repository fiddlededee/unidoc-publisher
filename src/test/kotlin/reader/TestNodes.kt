package reader

import converter.fodt.AsciidoctorOdAdapter
import common.asciidocAsHtml
import common.prettySerialize
import converter.fodt.FodtConverter
import fodt.parseStringAsXML
import model.Document
import org.junit.jupiter.api.Test
import verify


class TestNodes {

    @Test
    fun table() {
        FodtConverter {
            html = "<table><tr><td>A1</td><td>A2</td></tr></table>"
            parse()
            ast?.toYamlString()!!.verify()
        }
    }

    @Test
    fun image() {
        FodtConverter {
            html = "<img src='my-images/image.png' width='10pt'></img>"
            parse()
            ast?.toYamlString()!!.verify()
        }
    }

    @Test
    fun includeTags() {
        FodtConverter {
            html = "<p class='tag--content-1 tag--c2'><span class='tag--c3'>some text</span></p>"
            parse()
            ast?.toYamlString()!!.verify()
        }
    }

    @Test
    fun includeTagsAsciidoc() {
        FodtConverter {
            html = """
                [.tag--content-1]
                Some paragraph
            """.trimIndent().asciidocAsHtml()
            xpath = "/html/body"
            unknownTagProcessingRule = AsciidoctorOdAdapter.unknownTagProcessingRule
            println(html)
            parse()
            ast?.toYamlString()!!
                .replace(""".*Last update.*""".toRegex(), "")
                .verify()
        }

    }

    @Test
    fun IncludeTagsPre() {
        FodtConverter {
            ast = Document().apply {
                p { +"some text 1" }
                p {
                    includeTags = mutableSetOf("content-1")
                    +"some text 2"
                    span {
                        includeTags = mutableSetOf("content-2, content-3")
                    }
                }
            }
            generatePre()
            preList.joinToString("\n") {
                arrayOf(
                    it.includeTags.joinToString(","),
                    it.pre.parseStringAsXML().prettySerialize()
                ).joinToString("\n")
            }.verify()
        }
    }

}