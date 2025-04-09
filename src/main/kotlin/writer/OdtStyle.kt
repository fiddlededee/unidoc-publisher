package writer

import model.Node
import org.redundent.kotlin.xml.Node as XmlNode


interface OdtStyleAny

class OdtCustomWriter(
    val writer: OdWriter.(node: Node) -> Unit,
) : OdtStyleAny

@Deprecated("Use OdtCustomWriter")
typealias CustomWriter = OdtCustomWriter

class OdtStyle(
    val key: String? = null,
    val styler: XmlNode.(node: Node) -> Unit,
) : OdtStyleAny

class OdtStyleList(
    private vararg val style: OdtStyleAny
) {
    private var styleList = style.toMutableList()
    val size
        get() = styleList.size
    fun isEmpty(): Boolean {
        return style.isEmpty()
    }

    fun applyStyle(node: Node, xmlNode: XmlNode, key: String? = null) {
        styleList.filterIsInstance<OdtStyle>()
            .filter { it.key == key }
            .forEach {
                xmlNode.apply { it.styler.invoke(this, node) }
            }
    }

    fun applyCustomWriter(node: Node, odWriter: OdWriter): Boolean {
        return run breaking@{
            styleList.filterIsInstance<OdtCustomWriter>().forEach {
                val xmlNodeInitialSize = odWriter.preOdNode.children.size
                odWriter.preOdNode.apply {
                    it.writer.invoke(odWriter, node)
                }
                if (odWriter.preOdNode.children.size != xmlNodeInitialSize) return@breaking true
//                return false
            }
            false
        }
    }

    fun add(odtStyleList: OdtStyleList): OdtStyleList {
        styleList += odtStyleList.styleList
        return this
    }
}
