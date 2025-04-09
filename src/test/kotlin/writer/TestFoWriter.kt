package writer

import fodt.iterable
import fodt.parseStringAsXML
import fodt.serialize
import fodt.xpath
import model.*
import org.apache.fop.apps.FopFactory
import org.apache.fop.apps.MimeConstants
import org.approvaltests.Approvals
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXResult
import org.apache.fop.apps.Fop
import org.redundent.kotlin.xml.PrintOptions
import org.w3c.dom.Element
import java.io.FileOutputStream
import javax.xml.transform.Transformer
import javax.xml.transform.Source
import org.w3c.dom.Document as DomDocument
import javax.xml.transform.Result


class TestFoWriter {

    @Test
    fun simple() {
        val doc = Document().apply {
            p { +"Some paragraph" }
        }
        val foStyleList = FoStyleList()
        FoWriter(foStyleList = foStyleList)
            .apply { doc.write(this) }
            .preFoNode
            .toString()
            .apply { Approvals.verify(this) }
    }
}