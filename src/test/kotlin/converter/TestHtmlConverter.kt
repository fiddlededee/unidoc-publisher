package converter

import common.AsciidocHtmlFactory
import converter.html.AsciidoctorHtmlAdapter
import converter.html.HtmlConverter
import model.*
import org.junit.jupiter.api.Test
import java.io.File


class TestHtmlConverter {

    @Test
    fun buildDoc() {
        HtmlConverter {
            adaptWith(AsciidoctorHtmlAdapter)
            html = AsciidocHtmlFactory
                .getHtmlFromFile(File("doc/pages/unidoc-publisher-doc.adoc"))
            parse()
            ast().descendant { it.id == "footer" }.forEach { it.remove() }
            ast().appendFirstChild(Paragraph().apply {
                roles("header-image")
                img("./doc/images/unidoc-processor-symbol.svg")
                    .apply { width = Length(25F, LengthUnit.mm) }
            })
//            File("temp/ast.yaml").writeText(ast().toYamlString())
            generatePre()
//            File("temp/html.html").writeText(preList().first().pre)
            template = File("src/test/data/simple-html.html").readText()
            generateHtml()
            File("build/unidoc-publisher-doc-html.html").writeText(outHtml())
        }
    }

}