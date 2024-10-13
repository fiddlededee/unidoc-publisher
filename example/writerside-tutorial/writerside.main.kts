@file:DependsOn("ru.fiddlededee:unidoc-publisher:0.7.4")
@file:DependsOn("org.jsoup:jsoup:1.17.1")

import converter.FodtConverter
import model.*
import reader.UnknownTagProcessing
import writer.OdtStyle
import writer.OdtStyleList
import writer.paragraphProperties
import writer.textProperties
import java.io.File

fun String.toFile(name: String): String {
    File(name).writeText(this)
    return this
}

val scriptPath = __FILE__.parent

FodtConverter {
    html = File("${scriptPath}/export-to-pdf.html").readText()
    xpath = "//article"
    File("${scriptPath}/output/extracted-html.html").writeText(html!!)
    unknownTagProcessingRule = {
        if (setOf("figure", "section", "dl", "dt", "dd").contains(nodeName())) {
            UnknownTagProcessing.PROCESS
        } else if (
            nodeName() == "div" &&
            (classNames().isEmpty() ||
                    setOf("last-modified", "navigation-links").intersect(classNames()).isNotEmpty()
                    )
        ) {
            UnknownTagProcessing.PASS
        } else if (setOf("article").contains(nodeName())) {
            UnknownTagProcessing.PASS
        } else UnknownTagProcessing.UNDEFINDED
    }
    parse()
    ast().descendant { it.sourceTagName == "section" }.forEach {
        Paragraph().apply { it.insertBefore(this); roles("paragraph-before-section") }
        Paragraph().apply { it.insertAfter(this); roles("paragraph-after-section") }
    }
    ast().descendant { it is Text && it.parent()?.sourceTagName == "dt" }.forEach {
        Paragraph().apply {
            it.insertBefore(this)
            this.appendChild(it)
        }
    }
    ast().descendant { it.sourceTagName == "dd" }.forEach {
        Paragraph().apply {
            roles("paragraph-after-definition")
            it.insertAfter(this)
        }
    }
    File("${scriptPath}/output/ast.yaml").writeText(ast().toYamlString())
    ast().descendant { it is Image }.map { it as Image }
        .forEach { image ->
            if (image.ancestor { it is Paragraph }.isEmpty()) {
                Paragraph().apply {
                    image.insertBefore(this)
                    this.appendChild(image)
                    roles("figure-paragraph")
                }
            }
            image.src = "${scriptPath}/img/${image.src.substringAfterLast("/")}"
            fun Length?.toMm(): Length {
                return if (this == null) {
                    Length(170F, LengthUnit.mm)
                } else if (this.unit == LengthUnit.parrots) {
                    Length(this.value / 706F * 170F, LengthUnit.mm)
                } else Length(this.value, this.unit)
            }

            val imageWidth = image.width.toMm()
            val imageHeight = image.height.toMm()
            val maxWidth = if (image.ancestor { it is ListItem }.size == 1) 160F else 170F
            if (imageWidth.value > maxWidth) {
                imageHeight.value = imageHeight.value / imageWidth.value * maxWidth
                imageWidth.value = maxWidth
            }
            image.width = imageWidth
            image.height = imageHeight
        }
//    File("${scriptPath}/output/ast.yaml").writeText(ast().toYamlString())
    odtStyleList = OdtStyleList(
        OdtStyle { paragraph ->
            if (arrayOf("before", "after").map { "paragraph-$it-section" }
                    .intersect(paragraph.roles.toSet()).isEmpty()) return@OdtStyle
            attributes("text:style-name" to "Horizontal_20_Line")
        },
        OdtStyle { paragraph ->
            if (paragraph !is Paragraph) return@OdtStyle
            if (!paragraph.roles.contains("figure-paragraph")) return@OdtStyle
            paragraphProperties { attribute("fo:text-align", "center") }
        },
        OdtStyle { paragraph ->
            if (paragraph.ancestor { arrayOf("dd", "dt").contains(it.sourceTagName) }.isEmpty()) return@OdtStyle
            paragraphProperties {
                attributes(
                    "fo:padding" to "1.99mm",
                    "fo:border" to "0.06pt solid #cccccc",
                    "fo:background-color" to "#eeeeee"
                )
            }
        },
        OdtStyle { paragraph ->
            if (paragraph.ancestor { it.sourceTagName == "dt" }.isEmpty()) return@OdtStyle
            textProperties { attributes("fo:font-weight" to "bold") }
            paragraphProperties { attributes("fo:keep-with-next" to "always") }
        },
        OdtStyle { paragraph ->
            if (!paragraph.roles.contains("paragraph-after-definition")) return@OdtStyle
            paragraphProperties { attributes("fo:margin-bottom" to "0mm", "fo:line-height" to "0mm") }
        }
    )
    generatePre()
    template = File("approved/asciidoc/template-1.fodt").readText()
    generateFodt()
    File("${scriptPath}/output/export-to-pdf.fodt").writeText(fodt())
}

"Finished"