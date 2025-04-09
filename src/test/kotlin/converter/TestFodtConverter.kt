package converter

import converter.fodt.FodtConverter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import verify


class TestFodtConverter {
    @Language("XML")
    val testTemplate =
        """<office:document
                xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
                <office:automatic-styles>
                    <this-tag-should-be-left/>
                </office:automatic-styles>
                <office:body>
                    <office:text text:use-soft-page-breaks="true">
                        <this-tag-should-be-left/>
                            <text:p><text:variable-set text:name="include" office:value-type="string">all</text:variable-set></text:p>
                        <this-tag-should-be-left/>
                    </office:text>
                </office:body>
        </office:document>
            """



    @Test
    fun testFull() {
        FodtConverter {
            html = "<div><p>Some paragraph</p></div>"
            xpath = "/html/body/div"
            parse()
            generatePre()
            template = testTemplate
            generateFodt()
            fodt!!.verify()
        }
    }
}