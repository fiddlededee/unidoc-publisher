package fodt

import converter.PreRegion
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.util.NoSuchElementException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

val styleNS = "urn:oasis:names:tc:opendocument:xmlns:style:1.0"
val foNS = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"

val xpathInstance: XPath = XPathFactory.newInstance().newXPath().apply {
    namespaceContext = OdtNameSpaceContext()
}


fun Document.serialize(): String {
    val transformerFactory = TransformerFactory.newInstance()
    val trans = transformerFactory.newTransformer()
    trans.setOutputProperty(OutputKeys.METHOD, "xml")
    val sw = StringWriter()
    val result = StreamResult(sw)
    val source = DOMSource(this.documentElement)
    trans.transform(source, result)
    return sw.toString()
}


fun String.parseStringAsXML(): Document {
    return DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(InputSource(StringReader(this)))
}


fun Node.xpath(xpath: String): NodeList {
    return xpathInstance.compile(xpath)
        .evaluate(this, XPathConstants.NODESET) as NodeList
}


fun NodeList.iterable(): Iterable<Node> = object : Iterable<Node> {
    override fun iterator(): Iterator<Node> = object : Iterator<Node> {
        private var index = 0
        override fun hasNext(): Boolean = index < this@iterable.length
        override fun next(): Node {
            if (!hasNext()) throw NoSuchElementException()
            return this@iterable.item(index++)
        }
    }
}

class FodtGenerator(val preFodtList: ArrayList<PreRegion>, template: String) {
    val enrichedTemplate = template.parseStringAsXML()
    private val automaticStyles: Document = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(InputSource(StringReader("<root/>")))


    fun serialize(): String {
        return enrichedTemplate.serialize()
    }

    enum class StyleFamily { PARAGRAPH, TEXT, TABLE, TABLECELL, TABLECOLUMN, TABLEROW, GRAPHIC }

    var styleNum = 0
    fun setStyles(xpathExpression: String, styleFamily: StyleFamily, preFodt: Document) {
        val nodesWithStyles = preFodt.xpath(xpathExpression)
        nodesWithStyles.iterable().forEach {
            val styleNamePrefix = when (styleFamily) {
                StyleFamily.PARAGRAPH -> "PAMC"
                StyleFamily.TEXT -> "TAMC"
                StyleFamily.TABLE -> "TABAMC"
                StyleFamily.TABLECELL -> "TABCELLAMC"
                StyleFamily.TABLECOLUMN -> "TABCOLAMC"
                StyleFamily.TABLEROW -> "TABROWAMC"
                StyleFamily.GRAPHIC -> "GAMC"
            }
            val styleFamilyName = when (styleFamily) {
                StyleFamily.PARAGRAPH -> "paragraph"
                StyleFamily.TEXT -> "text"
                StyleFamily.TABLE -> "table"
                StyleFamily.TABLECELL -> "table-cell"
                StyleFamily.TABLECOLUMN -> "table-column"
                StyleFamily.TABLEROW -> "table-row"
                StyleFamily.GRAPHIC -> "graphic"
            }
            styleNum += 1
            val generatedStyleName = "$styleNamePrefix$styleNum"
            val automaticStylesStyle =
                automaticStyles.createElementNS(styleNS, "style:style")
            automaticStylesStyle.apply {
                setAttributeNS(styleNS, "style:name", generatedStyleName)
                setAttributeNS(styleNS, "style:family", styleFamilyName)
            }
            automaticStyles.documentElement.appendChild(automaticStylesStyle)
            val textPropertiesNodes = it.childNodes.iterable().filter { node ->
                node is Element && node.nodeName == "style:text-properties"
            }
            if (textPropertiesNodes.isNotEmpty()) {
                val textPropertiesNode = textPropertiesNodes[0]
                automaticStyles.adoptNode(textPropertiesNode)
                automaticStylesStyle.appendChild(textPropertiesNode)
            }
            val paragraphPropertiesNodes = it.childNodes.iterable().filter { node ->
                node is Element && node.nodeName == "style:paragraph-properties"
            }
            if (paragraphPropertiesNodes.isNotEmpty()) {
                val paragraphPropertiesNode = paragraphPropertiesNodes[0]
                automaticStyles.adoptNode(paragraphPropertiesNode)
                automaticStylesStyle.appendChild(paragraphPropertiesNode)
            }
            val graphicPropertiesNodes = it.childNodes.iterable().filter { node ->
                node is Element && node.nodeName == "style:graphic-properties"
            }
            if (graphicPropertiesNodes.isNotEmpty()) {
                val graphicPropertiesNode = graphicPropertiesNodes[0]
                automaticStyles.adoptNode(graphicPropertiesNode)
                automaticStylesStyle.appendChild(graphicPropertiesNode)
            }
            val tablePropertiesNodes = it.childNodes.iterable().filter { node ->
                node is Element && (node.nodeName == "style:table-properties"
                        || node.nodeName == "style:table-column-properties"
                        || node.nodeName == "style:table-cell-properties"
                        || node.nodeName == "style:table-row-properties")
            }
            if (tablePropertiesNodes.isNotEmpty()) {
                val tablePropertiesNode = tablePropertiesNodes[0]
                automaticStyles.adoptNode(tablePropertiesNode)
                automaticStylesStyle.appendChild(tablePropertiesNode)
            }
            val styleMasterPageName = (it as Element).getAttributeNode("style:master-page-name")
            if (styleMasterPageName != null) {
                automaticStylesStyle.apply {
                    setAttributeNS(
                        styleNS,
                        "style:master-page-name",
                        styleMasterPageName.nodeValue.replace(" ", "_20_")
                    )
                }
            }
            if (arrayOf(StyleFamily.TABLE, StyleFamily.TABLECELL, StyleFamily.TABLECOLUMN, StyleFamily.TABLEROW)
                    .contains(styleFamily)
            ) {
                it.setAttribute("table:style-name", generatedStyleName)
            } else {
                val styleNameAttribute =
                    if (styleFamily == StyleFamily.GRAPHIC)
                        it.getAttributeNode("draw:style-name") else
                        it.getAttributeNode("text:style-name")
                if (styleNameAttribute != null) {
                    automaticStylesStyle.apply {
                        setAttributeNS(
                            styleNS,
                            "style:parent-style-name",
                            styleNameAttribute.nodeValue.replace(" ", "_20_")
                        )
                    }
                    styleNameAttribute.nodeValue = generatedStyleName
                } else {
                    if (styleFamily == StyleFamily.GRAPHIC)
                        it.setAttribute("draw:style-name", generatedStyleName) else
                        it.setAttribute("text:style-name", generatedStyleName)
                }
            }
        }
    }

    fun processTaggedRegion(preRegion: PreRegion) {
        val preFodt = preRegion.pre.parseStringAsXML()
        setStyles("(//text:p|//text:h)", StyleFamily.PARAGRAPH, preFodt)
        setStyles("(//text:span)", StyleFamily.TEXT, preFodt)
        setStyles("(//table:table)", StyleFamily.TABLE, preFodt)
        setStyles("(//table:table-cell)", StyleFamily.TABLECELL, preFodt)
        setStyles("(//table:table-column)", StyleFamily.TABLECOLUMN, preFodt)
        setStyles("(//table:table-row)", StyleFamily.TABLEROW, preFodt)
        setStyles("(//draw:frame)", StyleFamily.GRAPHIC, preFodt)
        val officeText = enrichedTemplate.xpath("//office:body/office:text")
        if (officeText.length == 1) {
            val officeTextElement = officeText.item(0)
            officeTextElement.xpath("//text:variable-set[@text:name = 'include']")
                .iterable().map { it as Element }.forEach { markedElement ->
                    val tagName = markedElement.textContent
                    if (!preRegion.includeTags.contains(tagName)) return@forEach
                    val elementToReplace = if (preRegion.inLine) markedElement else
                        markedElement.parentNode
                    preFodt.xpath("/root/*").iterable().forEach {
                        enrichedTemplate.adoptNode(it)
                        elementToReplace.parentNode.insertBefore(it, elementToReplace)
                    }
                    elementToReplace.parentNode.removeChild(elementToReplace)
                }
            officeTextElement.xpath("//text:variable-set[@text:name = 'process']")
                .iterable().map { it as Element }.forEach {
                    if (it.textContent != "end") return@forEach
                    var deleteFromNode = it.parentNode
                    while (deleteFromNode.nextSibling != null) {
                        val nextSibling = deleteFromNode.nextSibling
                        deleteFromNode.parentNode.removeChild(deleteFromNode)
                        deleteFromNode = nextSibling
                    }
                    deleteFromNode.parentNode.removeChild(deleteFromNode)
                }
        } else {
            throw Exception("office:body/office:text not found")
        }

    }

    init {
        preFodtList.forEach { preRegion ->
            processTaggedRegion(preRegion)
        }

        val officeAutomaticStyles = enrichedTemplate.xpath("//office:automatic-styles")
        if (officeAutomaticStyles.length == 1) {
            val officeAutomaticStylesElement = officeAutomaticStyles.item(0)
            automaticStyles.xpath("/root/*").iterable().forEach {
                enrichedTemplate.adoptNode(it)
                officeAutomaticStylesElement.appendChild(it)
            }
        } else {
            throw Exception("office:automatic-styles not found")
        }
    }

}

