package writer

import model.Node
import org.redundent.kotlin.xml.Node as XmlNode


interface HtmlStyleAny

class HtmlCustomWriter(
    val writer: HtmlWriter.(node: Node) -> Unit,
) : HtmlStyleAny

class HtmlStyle(
    val key: String? = null,
    val styler: XmlNode.(node: Node) -> Unit,
) : FoStyleAny

class HtmlStyleList(
    private vararg val style: HtmlStyleAny
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

    fun applyCustomWriter(node: Node, htmlWriter: HtmlWriter): Boolean {
        return run breaking@{
            styleList.filterIsInstance<HtmlCustomWriter>().forEach {
                val xmlNodeInitialSize = htmlWriter.preHtmlNode.children.size
                htmlWriter.preHtmlNode.apply {
                    it.writer.invoke(htmlWriter, node)
                }
                if (htmlWriter.preHtmlNode.children.size != xmlNodeInitialSize) return@breaking true
            }
            false
        }
    }

    fun add(htmlStyleList: HtmlStyleList): HtmlStyleList {
        styleList += htmlStyleList.styleList
        return this
    }
}
