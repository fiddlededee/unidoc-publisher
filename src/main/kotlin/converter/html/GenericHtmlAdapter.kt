package converter.html

import model.Node
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.HtmlStyleList

interface GenericHtmlAdapter {
    val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing
    fun basicStyle(): HtmlStyleList
    fun Node.normalizeAll() {}
}