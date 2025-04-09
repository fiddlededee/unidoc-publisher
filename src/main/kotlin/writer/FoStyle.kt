package writer

import model.Node
import org.redundent.kotlin.xml.Node as XmlNode


interface FoStyleAny

class FoCustomWriter(
    val writer: FoWriter.(node: Node) -> Unit,
) : FoStyleAny

class FoStyle(
    val key: String? = null,
    val styler: XmlNode.(node: Node) -> Unit,
) : FoStyleAny

class FoStyleList(
    private vararg val style: FoStyleAny
) {
    private var styleList = style.toMutableList()
    val size
        get() = styleList.size
    fun isEmpty(): Boolean {
        return style.isEmpty()
    }

    fun applyStyle(node: Node, xmlNode: XmlNode, key: String? = null) {
        styleList.filterIsInstance<FoStyle>()
            .filter { it.key == key }
            .forEach {
                xmlNode.apply { it.styler.invoke(this, node) }
            }
    }

    fun applyCustomWriter(node: Node, foWriter: FoWriter): Boolean {
        return run breaking@{
            styleList.filterIsInstance<FoCustomWriter>().forEach {
                val xmlNodeInitialSize = foWriter.preFoNode.children.size
                foWriter.preFoNode.apply {
                    it.writer.invoke(foWriter, node)
                }
                if (foWriter.preFoNode.children.size != xmlNodeInitialSize) return@breaking true
            }
            false
        }
    }

    fun add(foStyleList: FoStyleList): FoStyleList {
        styleList += foStyleList.styleList
        return this
    }
}
