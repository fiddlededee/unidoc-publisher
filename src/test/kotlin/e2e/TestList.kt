package e2e

import converter.fodt.AsciidoctorOdAdapter
import common.asciidoc2PdfApprove
import model.ListItem
import model.TextFrame
import org.junit.jupiter.api.Test
import writer.OdtStyle
import writer.OdtStyleList
import java.io.File


class TestList {

    @Test
    fun tuneListComplexContent() {
        File("approved/asciidoc/list-complex-2.adoc")
            .readText()
            .asciidoc2PdfApprove("list-complex-2") {
                odtStyleList = AsciidoctorOdAdapter.basicStyle()
                odtStyleList.add(
                    OdtStyleList(
                        OdtStyle("frame") { frame ->
                            if (frame !is TextFrame) return@OdtStyle
                            val level = frame.ancestor { it is ListItem }.size
                            if (level == 0) return@OdtStyle
                            val indent = 10 + (level - 1) * 6
                            attributes(
                                "svg:width" to "${170 - indent}mm",
                                "text:anchor-type" to "as-char",
                                "style:rel-width" to ""
                            )
                        }
                    )
                )
            }
    }
}

