package fodt

import common.prettySerialize
import converter.PreRegion
import org.approvaltests.Approvals
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class TestFodtOutput {
    @Test
    fun testSimpleFodtOutput() {
        val preFodt =
            """
                <root xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                      xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                      xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
                    <text:h text:style-name="Heading 1">
                        <style:text-properties fo:font-size="14pt"/>
                        Heading 1
                    </text:h>
                    <text:p text:style-name="Body Text">
                        Some text
                    </text:p>
                    <text:h text:style-name="Heading 1">
                        <style:text-properties fo:font-size="14pt"/>
                        Heading 1
                    </text:h>
                </root>
        """

        val template =
            """
                <office:document
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

        val preFodtList = arrayListOf<PreRegion>(
            PreRegion(setOf("all"), preFodt, false)
        )

        FodtGenerator(preFodtList, template)
            .enrichedTemplate
            .prettySerialize()
            .apply {
                Approvals.verify(this)
            }
    }

}

