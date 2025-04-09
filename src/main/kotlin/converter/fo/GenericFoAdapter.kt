package converter.fo

import model.Node
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.FoStyleList

interface GenericFoAdapter {
    val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing
    fun basicStyle(): FoStyleList
    fun Node.normalizeAll() {}
}