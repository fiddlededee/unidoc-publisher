package common

import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.w3c.dom.Document
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


fun Document.prettySerialize(): String {
    val format: OutputFormat = OutputFormat.createPrettyPrint()
    format.setIndentSize(2)
    format.setSuppressDeclaration(true)
    format.encoding = "UTF-8"
    this.normalize()
    val document: org.dom4j.Document = DocumentHelper.parseText(this.serialize())
    val sw = StringWriter()
    val writer: XMLWriter = XMLWriter(sw, format)
    writer.write(document)
        return sw.toString()
            .replace(""" xmlns:.+=".*"""".toRegex(), "")
            .replace(""" ([a-z-]+[:][a-z-]+=")""".toRegex(), "\n${" ".repeat(20)}$1")
            .trim()
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
