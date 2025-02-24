package converter

import common.GenericAdapter
import fodt.FodtGenerator
import model.Document
import model.Node
import model.Text
import org.redundent.kotlin.xml.PrintOptions
import reader.GenericHtmlReader
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.OdWriter
import writer.OdtStyleList

open class FodtConverter(init: FodtConverter.() -> Unit) {
    private var adapter: GenericAdapter? = null
    fun adaptWith(adapter: GenericAdapter) {
        unknownTagProcessingRule = adapter.unknownTagProcessingRule
        odtStyleList = adapter.basicStyle()
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
    var odtStyleList = OdtStyleList()
    var template: String? = null
    var ast: Node? = null
    var preList: ArrayList<PreRegion> = arrayListOf()
    var fodtGenerator: FodtGenerator? = null
    var fodt: String? = null
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

    fun fodt(): String {
        return fodt ?: throw Exception("Fodt is empty")
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
        // Don't understand html rendering rules, hope I'm right about this
        localAst.descendant { it is Text && it.text.isBlank() }.forEach { blankTextNode ->
            if (!blankTextNode.hasPrevious() ||
                !blankTextNode.previous().isInline ||
                blankTextNode.previous().roles.contains("br") ||
                !blankTextNode.hasNext() ||
                !blankTextNode.next().isInline ||
                blankTextNode.next().roles.contains("br")
            )
                blankTextNode.remove()
        }
    }

    fun generatePre() {
        val localAst = ast ?: throw Exception("Ast is null")
        adapter?.apply { localAst.normalizeAll() }
        (localAst.descendant { it.includeTags.isNotEmpty() } +
                localAst
                ).forEach { nodeToProcess ->
                val includeTags = nodeToProcess.includeTags
                    .let { if (it.isNotEmpty()) it.toSet() else setOf("all") }
                val pre = newOdWriterInstance(odtStyleList = odtStyleList)
                    .apply { (nodeToProcess.write(this)) }
                    .preOdNode.toString(PrintOptions(pretty = false))
                preList.add(PreRegion(includeTags, pre, nodeToProcess.isInline))
            }

    }

    fun generateFodt() {
        val localTemplate = template ?: throw Exception("Please, set template variable")
        if (preList.isEmpty()) throw Exception("Please set pre variable")
        fodtGenerator = FodtGenerator(preList, localTemplate)
        serializeFodt()
    }

    fun convert() {
        parse(); generatePre(); generateFodt()
    }

    fun ast2fodt() {
        generatePre(); generateFodt()
    }

    fun serializeFodt() {
        fodt = fodtGenerator?.serialize()
    }

    open fun newReaderInstance(
        ast: Node, htmlNode: HtmlNode,
        unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing,
        customNodeProcessing: Node.(htmlNode: HtmlNode) -> Unit
    ): GenericHtmlReader {
        return GenericHtmlReader(ast, htmlNode, unknownTagProcessingRule, customNodeProcessing)
    }

    open fun newOdWriterInstance(odtStyleList: OdtStyleList): OdWriter {
        return OdWriter(odtStyleList = odtStyleList)
    }
}