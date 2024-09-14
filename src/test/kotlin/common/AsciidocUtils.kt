package common

import converter.FodtConverter
import de.redsix.pdfcompare.CompareResultImpl
import de.redsix.pdfcompare.PdfComparator
import model.*
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.asciidoctor.SafeMode
import reader.GenericHtmlReader
import reader.HtmlNode
import reader.UnknownTagProcessing
import java.io.File
import java.nio.file.Paths

object AsciidocHtmlFactory {
    private val factory: Asciidoctor = Asciidoctor.Factory.create()
    fun getHtml(string: String): String {
        return factory.convert(
            string, Options.builder().backend("html5")
                .safe(SafeMode.UNSAFE).sourcemap(true).toFile(false).standalone(true).build()
        )
    }
}

fun String.asciidoc2PdfApprove(key: String, tune: FodtConverter.() -> Unit = {}) {
    val asciidocMarkup = this
    FodtConverter {
        adaptWith(AsciidoctorAdapter)
        html = asciidocMarkup
            .trimIndent()
            .asciidocAsHtml()
//        File("temp/html.html").writeText(html!!)
        template = File("approved/asciidoc/template-1.fodt").readText()
        tune.invoke(this)
        if (ast == null) parse()
//        File("temp/ast.yaml").writeText(ast().toYamlString())
        ast().descendant { it is Image }.map { it as Image }.forEach { image ->
            val base64Regex = """^data:image/(.*);base64,(.*)$""".toRegex()
            if (!base64Regex.matches(image.src)) image.src =
                "approved/asciidoc/${image.src}"
        }
        ast
        if (preList.isEmpty()) generatePre()
        if (fodt == null) generateFodt()
//        File("temp/ast-transformed.yaml").writeText(ast().toYamlString())
//        File("temp/fodt.fodt").writeText(fodt())
        File("approved/asciidoc/${key}.received.fodt").writeText(fodt())
        Lo.fodtToPdf("approved/asciidoc/${key}.received.fodt")
        if (!File("approved/asciidoc/${key}.approved.pdf").exists())
            File("approved/asciidoc/${key}.received.pdf").copyTo(
                File("approved/asciidoc/${key}.approved.pdf")
            )
    }
    PdfComparator<CompareResultImpl>(
        "approved/asciidoc/${key}.received.pdf",
        "approved/asciidoc/${key}.approved.pdf"
    )
        .compare()
        .apply {
            if (isNotEqual) {
                writeTo("approved/asciidoc/${key}.diff")
                val currentPath = Paths.get("").toAbsolutePath().toString()
                throw Exception(
                    "Output differs" +
                            "\nreceived: " +
                            "file://${currentPath}/approved/asciidoc/${key}.received.pdf" +
                            "\napproved: " +
                            "file://${currentPath}/approved/asciidoc/${key}.approved.pdf" +
                            "\ndiff:     " +
                            "file://${currentPath}/approved/asciidoc/${key}.diff.pdf"
                )
            } else {
                arrayOf("pdf", "fodt").forEach {
                    File("approved/asciidoc/${key}.received.$it").delete()
                }
                File("approved/asciidoc/${key}.diff.pdf").apply { if (exists()) delete() }
            }
        }
}

fun String.asciidocAsHtml(): String {
    return AsciidocHtmlFactory.getHtml(this)
}

fun HtmlNode.parseAsciidoc(): Document {
    val document = Document()
    GenericHtmlReader(document, this,
        {
            arrayOf(
                setOf("unnecessary") to UnknownTagProcessing.PROCESS,
                setOf("sect1", "sectionbody", "paragraph") to UnknownTagProcessing.PASS
            ).firstOrNull { classNames().intersect(it.first).isNotEmpty() }?.second
                ?: UnknownTagProcessing.UNDEFINDED
        }
    ).apply { iterateAll() }
    return document
}


fun String.htmlMarkupAsHtmlNode(): HtmlNode {
    return HtmlNode(this)
}
