#!/usr/bin/env kotlin
@file:DependsOn("ru.fiddlededee:unidoc-publisher:0.7.9")
@file:DependsOn("org.redundent:kotlin-xml-builder:1.9.0")

import org.redundent.kotlin.xml.Node as XmlNode
import converter.FodtConverter
import fodt.parseStringAsXML
import model.*
import reader.GenericHtmlReader
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.OdWriter
import writer.OdtStyleList
import java.io.File

val htmlToConvert = """
        <p>Some paragraph text</p>
        <my-tag my-attribute='attribute-value'>Some my tag text</my-tag>
    """.trimIndent()

interface MyBackendWriter : BackendWriter {
    fun write(myNode: MyNode)
}

class MyNode(var myAttribute: String, override val isInline: Boolean = false) : Node() {
    private fun write(bw: MyBackendWriter) {
        bw.write(this)
    }

    override fun write(bw: BackendWriter) {
        this.write(bw as MyBackendWriter)
    }
}

class MyHtmlReader(
    node: Node, htmlNode: HtmlNode,
    unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing = { UnknownTagProcessing.UNDEFINDED },
) : GenericHtmlReader(node, htmlNode, unknownTagProcessingRule) {
    override val detectors = arrayOf(::detectMyTag) + super.detectors

    private fun detectMyTag() {
        detectByExpression({ it.nodeName() == "my-tag" }) { confirmedNode ->
            val myNode = MyNode(confirmedNode.attr("my-attribute"))
            parseNode(addToAST(myNode), confirmedNode)
        }
    }
}

class MyOdWriter(preOdNode: XmlNode? = null, odtStyleList: OdtStyleList) : OdWriter(preOdNode, odtStyleList),
    MyBackendWriter {
    override fun newInstance(xmlNode: XmlNode, odtStyleList: OdtStyleList): OdWriter {
        return MyOdWriter(xmlNode, odtStyleList)
    }

    override fun write(myNode: MyNode) {
        preOdNode.apply {
            "text:p" {
                attribute("text:style-name", "Text body")
                process(myNode)
                -" (${myNode.myAttribute})"
            }
        }
    }
}

class MyFodtConverter(init: FodtConverter.() -> Unit) : FodtConverter(init) {
    override fun newReaderInstance(
        ast: Node, htmlNode: HtmlNode,
        unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing,
        customNodeProcessingRule: Node.(htmlNode: HtmlNode) -> Unit

    ): GenericHtmlReader {
        return MyHtmlReader(ast, htmlNode, unknownTagProcessingRule)
    }

    override fun newOdWriterInstance(odtStyleList: OdtStyleList): OdWriter {
        return MyOdWriter(odtStyleList = odtStyleList)
    }
}

MyFodtConverter {
    html = htmlToConvert
    template = File("${__FILE__.parent}/template.fodt").readText()
    convert()
    File("${__FILE__.parent}/output/customized-output.fodt")
        .writeText(fodt())
}.preList


