package converter.fodt

import model.Node
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.OdtStyleList

interface GenericFodtAdapter {
    val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing
    fun basicStyle(): OdtStyleList
    fun Node.normalizeAll() {}
}