package common

import model.Node
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.OdtStyleList

interface GenericAdapter {
    val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing
    fun basicStyle(): OdtStyleList
    fun Node.normalizeAll() {}
}