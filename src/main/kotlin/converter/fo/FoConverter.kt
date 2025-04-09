package converter.fo

import converter.PreRegion
import fodt.*
import model.*
import org.redundent.kotlin.xml.PrintOptions
import org.w3c.dom.Element
import reader.GenericHtmlReader
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.FoStyleList
import writer.FoWriter

open class FoConverter(init: FoConverter.() -> Unit) {
    private var adapter: GenericFoAdapter? = null
    fun adaptWith(adapter: GenericFoAdapter) {
        unknownTagProcessingRule = adapter.unknownTagProcessingRule
        foStyleList = adapter.basicStyle()
        this.adapter = adapter
    }

    var html: String? = null
        set(value) {
            val fieldNotNullable = value ?: throw Exception("html value can't be null")
            field = fieldNotNullable; htmlNode = HtmlNode(fieldNotNullable)
        }

    var xpath: String = "/html/body"
    var unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing = { UnknownTagProcessing.UNDEFINDED }
    var customNodeProcessingRule: Node.(htmlNode: HtmlNode) -> Unit = {}
    var foStyleList = FoStyleList()
    var template: String? = null
    var ast: Node? = null
    var preList: ArrayList<PreRegion> = arrayListOf()
    var fo: String? = null
    private var htmlNode: HtmlNode? = null

    init {
        this.apply(init)
    }

    fun ast(): Node {
        return ast ?: throw Exception("AST is empty")
    }

    fun template(): String {
        return template ?: throw Exception("Template is empty")
    }

    fun preList(): ArrayList<PreRegion> {
        if (preList.isEmpty()) throw Exception("The list of pre FODT elements is empty")
        return preList
    }

    fun html(): String {
        return html ?: throw Exception("HTML is emplty")
    }

    fun fo(): String {
        return fo ?: throw Exception("Fo is empty")
    }

    fun parse() {
        val localHtmlNode = htmlNode?.selectAtXpath(xpath)
            ?: throw Exception("Please set html")
        ast = Document()
        val localAst = ast ?: throw Exception("Error: ast variable was mutated")
        newReaderInstance(
            localAst, localHtmlNode,
            unknownTagProcessingRule, customNodeProcessingRule
        )
            .apply { localAst.setBasics(localHtmlNode); iterateAll() }
        localAst.normalizeWhitespaces()
    }

    enum class ProcessingType { Trim, ConvertToSpace }


    fun generatePre() {
        val localAst = ast ?: throw Exception("Ast is null")
        adapter?.apply { localAst.normalizeAll() }
        (localAst.descendant { it.includeTags.isNotEmpty() } +
                localAst
                ).forEach { nodeToProcess ->
                val includeTags = nodeToProcess.includeTags
                    .let { if (it.isNotEmpty()) it.toSet() else setOf("all") }
                val pre = newFoWriterInstance(foStyleList = foStyleList)
                    .apply { (nodeToProcess.write(this)) }
                    .preFoNode.toString(PrintOptions(pretty = false))
                preList.add(PreRegion(includeTags, pre, nodeToProcess.isInline))
            }

    }

    fun generateFo() {

        val localTemplate = template ?: throw Exception("Please, set template variable")
        val templateDom = localTemplate.parseStringAsXML()
        if (preList.isEmpty()) throw Exception("Please set pre variable")
        if (preList.size >= 2) throw Exception("Fo converter doesn't support tagged regions for now")
        val contentPoint = templateDom.xpath("//unidoc:include[text()='all']")
            .iterable().firstOrNull() ?: throw Exception("Point to include contents not found")

        val excerpt = preList.first().pre.parseStringAsXML()

        excerpt.firstChild.childNodes.iterable().filterIsInstance<Element>().forEach {
            val importedNode = templateDom.importNode(it, true)
            contentPoint.parentNode.insertBefore(importedNode, contentPoint)
        }
        fo = templateDom.serialize()
    }

    fun convert() {
        parse(); generatePre(); generateFo()
    }

    fun ast2fo() {
        generatePre(); generateFo()
    }


    open fun newReaderInstance(
        ast: Node, htmlNode: HtmlNode,
        unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing,
        customNodeProcessing: Node.(htmlNode: HtmlNode) -> Unit
    ): GenericHtmlReader {
        return GenericHtmlReader(ast, htmlNode, unknownTagProcessingRule, customNodeProcessing)
    }

    open fun newFoWriterInstance(foStyleList: FoStyleList): FoWriter {
        return FoWriter(foStyleList = foStyleList)
    }
}