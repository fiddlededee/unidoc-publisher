package e2e

import com.helger.css.reader.CSSReader
import converter.fodt.AsciidoctorOdAdapter
import common.asciidoc2PdfApprove
import model.Paragraph
import model.Span
import org.junit.jupiter.api.Test
import writer.OdtStyle
import writer.OdtStyleList
import writer.textProperties
import java.io.File
import java.nio.charset.StandardCharsets
import com.helger.css.ECSSVersion


class TestListing {

    @Test
    fun listingHighlight() {
        File("approved/asciidoc/listing-highlight-1.adoc")
            .readText()
            .asciidoc2PdfApprove("listing-highlight-1") {
                odtStyleList = AsciidoctorOdAdapter.basicStyle()
                odtStyleList.add(getRougeStyles())
            }
    }

    fun getRougeStyles(): OdtStyleList {
        val css = CSSReader.readFromFile(
            File("approved/asciidoc/syntax.css"), StandardCharsets.UTF_8, ECSSVersion.CSS30
        )

        val rougeStyles = css?.allStyleRules?.flatMap { styleRule ->
            styleRule.allSelectors.flatMap { selector ->
                styleRule.allDeclarations.map { declaration ->
                    selector.allMembers.map { it.asCSSString } to
                            (declaration.property to declaration.expressionAsCSSString)
                }
            }
        }?.filter {
            it.first.size == 3 && it.first[0] == ".highlight" &&
                    it.first[2][0] == "."[0] && it.first[2].length <= 3 &&
                    arrayOf("color", "background-color", "font-weight", "font-style")
                        .contains(it.second.first)
        }?.map { it.first[2].substring(1) to it.second }
            ?.groupBy { it.first }
            ?.map { it.key to it.value.associate { pair -> pair.second.first to pair.second.second } }
            ?.toMap() ?: mapOf()

        return OdtStyleList(
            OdtStyle { span ->
                val condition = (span is Span) &&
                        (span.ancestor { it is Paragraph && it.sourceTagName == "pre" }.isNotEmpty())
                if (!condition) return@OdtStyle
                rougeStyles.filter { span.roles.contains(it.key) }.forEach { style ->
                    textProperties {
                        arrayOf("color", "background-color", "font-weight", "font-style").forEach {
                            style.value[it]
                                ?.let { value ->
                                    attribute("fo:$it", value)
                                }
                        }
                    }
                }
            }
        )

    }
}

