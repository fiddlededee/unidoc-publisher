package writer

import model.*
import org.redundent.kotlin.xml.Namespace
import org.redundent.kotlin.xml.xml
import org.redundent.kotlin.xml.Node as XmlNode

open class FoWriter(
    preFoNode: XmlNode? = null, val foStyleList: FoStyleList
) : BackendWriter {
    fun XmlNode.process(node: Node, key: String? = null) {
        foStyleList.applyStyle(node, this, key)
        node.children().forEach {
            if (it.includeTags.contains("hide")) return@forEach
            val customProcessed = foStyleList.applyCustomWriter(it, newInstance(this, foStyleList))
            if (!customProcessed) it.write(newInstance(this, foStyleList))
        }
    }

    val preFoNode = preFoNode ?: xml("fo:root") {
        namespace(Namespace("fo", "http://www.w3.org/1999/XSL/Format"))
    }

    open fun newInstance(xmlNode: XmlNode, foStyleList: FoStyleList): FoWriter {
        return FoWriter(xmlNode, foStyleList)
    }

    override fun write(openBlock: OpenBlock) {
        preFoNode.apply {
            "fo:block" {
                process(openBlock)
            }
        }
    }

    override fun write(image: Image) {
        preFoNode.apply {
            "fo:external-graphic" {
                val base64Regex = """^data:image/(.*);base64,(.*)$""".toRegex()
                if (!base64Regex.matches(image.src))
                    attributes("src" to "${image.src}") else
                    attributes("src" to image.src)
                arrayOf(
                    "width" to image.width, "height" to image.height
                ).forEach {
                    val length = it.second
                    if (length != null) {
                        if (length.unit == LengthUnit.parrots
                            || length.unit == LengthUnit.perc
                        ) throw Exception(
                            "Don't understand ${length.unit} as a length unit" + " see https://en.wikipedia.org/wiki/38_Parrots for details"
                        )
                        if (length.unit == LengthUnit.cmm || length.unit == LengthUnit.mmm)
                            attribute(it.first, "${length.value / 100}mm") else
                            attribute(it.first, "${length.value}${length.unit}")
                    }
                }
                process(image)
            }
        }
    }

    override fun write(table: Table) {
        preFoNode.apply {
            "fo:table" {
                process(table)
            }
        }
    }

    override fun write(tableRowGroup: TableRowGroup) {
        preFoNode.apply {
            when (tableRowGroup.type) {
                TRG.head -> "fo:table-header" { process(tableRowGroup) }
                TRG.body -> "fo:table-body" { process(tableRowGroup) }
                TRG.foot -> "fo:table-footer" { process(tableRowGroup) }
            }
        }
    }

    override fun write(tr: TableRow) {
        preFoNode.apply {
            "fo:table-row" {
                process(tr)
            }
        }
    }

    override fun write(td: TableCell) {
        preFoNode.apply {
            "fo:table-cell" {
                attributes(
                    "number-columns-spanned" to td.colspan,
                    "number-rows-spanned" to td.rowspan
                )
                process(td)
            }
        }
    }

    override fun write(colGroup: ColGroup) {
        preFoNode.apply {
            process(colGroup)
        }
    }

    override fun write(col: Col) {
        preFoNode.apply {
            "fo:table-column" {
                if (col.width.unit != LengthUnit.perc) throw Exception("Only relative width for columns supported")
                val localParent = col.parent() ?: throw Exception("Col definition should have a parent")
                val relColumnWidth = col.width.value /
                        localParent.children().filterIsInstance<Col>()
                            .map { it.width.value }
                            .reduce { acc, next -> acc + next }
                attribute("column-width", "${relColumnWidth * 100F}%")
            }
        }
    }

    override fun write(ol: OrderedList) {
        preFoNode.apply {
            "fo:list-block" {
                process(ol)
            }
        }
    }

    override fun write(ul: UnorderedList) {
        preFoNode.apply {
            "fo:list-block" {
                process(ul)
            }
        }
    }

    override fun write(li: ListItem) {
        preFoNode.apply {
            "fo:list-item" {
                "fo:list-item-label" {
                    //TODO
                    "fo:block" {
                        val localLiLabel = li.label
                        if (localLiLabel != null) -localLiLabel else {
                            if (li.ancestor { it is OrderedList || it is UnorderedList }
                                    .first() is UnorderedList) {
                                -"•"
                            } else {
                                -"${(li.previousSibling { it is ListItem }.size + 1)}."
                            }

                        }
                        foStyleList.applyStyle(li, this, "list-item-label")
                    }
                }
                "fo:list-item-body" {
                    process(li, "list-item-body")
                }
                foStyleList.applyStyle(li, this)
            }
        }
    }

    override fun write(h: Heading) {
        preFoNode.apply {
            "fo:block" {
                val localId = h.id
                if (localId != null) attributes("id" to localId)
                process(h)
            }
        }
    }

    override fun write(p: Paragraph) {
        preFoNode.apply {
            "fo:block" {
                val localId = p.id
                if (localId != null) attributes("id" to localId)
                process(p)
            }
        }
    }

    override fun write(doc: Document) {
        preFoNode.apply {
            process(doc)
        }
    }

    override fun write(text: Text) {
        val textToAppend =
            if (text.text.trim('\n', '\r').isEmpty()) "${text.text} " else text.text
        preFoNode.apply { -textToAppend }
    }

    override fun write(span: Span) {
        preFoNode.apply {
            "fo:inline" {
                if (span.roles.contains("em") or span.roles.contains("i")) {
                    attribute("font-style", "italic")
                }
                if (span.roles.contains("strong") or span.roles.contains("b")) {
                    attribute("font-weight", "bold")
                }
                if (span.roles.contains("mark")) {
                    attribute("background-color", "#fef0c2")
                }
                arrayOf("sup", "sub").forEach { role ->
                    if (span.roles.contains(role))
                        attributes("vertical-align" to role, "font-size" to "58%")
                    if (span.roles.contains("sup")) attributes("baseline-shift" to "58%")
                }
                process(span)
            }
        }
    }

    override fun write(a: Anchor) {
        preFoNode.apply {
            "fo:basic-link" {
                if (a.href.startsWith("#"))
                    attributes("internal-destination" to "${a.href.substring(1)}") else
                    attributes("external-destination" to "url('${a.href}')")
                process(a)
            }
        }
    }

    override fun write(toc: Toc) {
        preFoNode.apply {
            "fo:block" {
                -"TODO: TOC"
            }
        }
    }

    override fun write(textFrame: TextFrame) {
        TODO("Not yet implemented")
    }

    override fun write(footnote: Footnote) {
        TODO("Not yet implemented")
    }

    override fun write(footnoteRef: FootnoteRef) {
        TODO("Not yet implemented")
    }

    override fun write(dummyNode: Node) {
        TODO("Not yet implemented")
    }
}