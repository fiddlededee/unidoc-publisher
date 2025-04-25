package writer

import model.*
import org.redundent.kotlin.xml.Namespace
import org.redundent.kotlin.xml.xml
import org.redundent.kotlin.xml.Node as XmlNode

open class HtmlWriter(
    preHtmlNode: XmlNode? = null, val htmlStyleList: HtmlStyleList
) : BackendWriter {
    fun XmlNode.process(node: Node, key: String? = null) {
        htmlStyleList.applyStyle(node, this, key)
        val currentClass = (attributes["class"]?.toString() ?: "").trim()
        val newClass = "$currentClass ${node.roles.joinToString(" ")}"
        if (newClass.isNotBlank()) attributes("class" to newClass.trim())
        node.children().forEach {
            if (it.includeTags.contains("hide")) return@forEach
            val customProcessed = htmlStyleList.applyCustomWriter(it, newInstance(this, htmlStyleList))
            if (!customProcessed) it.write(newInstance(this, htmlStyleList))
        }
    }

    val preHtmlNode = preHtmlNode ?: xml("html-root") {
    }

    open fun newInstance(xmlNode: XmlNode, htmlStyleList: HtmlStyleList): HtmlWriter {
        return HtmlWriter(xmlNode, htmlStyleList)
    }

    override fun write(openBlock: OpenBlock) {
        preHtmlNode.apply {
            "div" {
                process(openBlock)
            }
        }
    }

    override fun write(image: Image) {
        preHtmlNode.apply {
            "img" {
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
        preHtmlNode.apply {
            "table" {
                process(table)
            }
        }
    }

    override fun write(tableRowGroup: TableRowGroup) {
        preHtmlNode.apply {
            when (tableRowGroup.type) {
                TRG.head -> "thead" { process(tableRowGroup) }
                TRG.body -> "tbody" { process(tableRowGroup) }
                TRG.foot -> "tfoot" { process(tableRowGroup) }
            }
        }
    }

    override fun write(tr: TableRow) {
        preHtmlNode.apply {
            "tr" {
                process(tr)
            }
        }
    }

    override fun write(td: TableCell) {
        preHtmlNode.apply {
            "td" {
                attributes(
                    "colspan" to td.colspan,
                    "rowspan" to td.rowspan
                )
                process(td)
            }
        }
    }

    override fun write(colGroup: ColGroup) {
        preHtmlNode.apply {
            "colgroup" {
                process(colGroup)
            }
        }
    }

    override fun write(col: Col) {
        preHtmlNode.apply {
            "col" {
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
        preHtmlNode.apply {
            "ol" { process(ol) }
        }
    }

    override fun write(ul: UnorderedList) {
        preHtmlNode.apply {
            "ul" { process(ul) }
        }
    }

    override fun write(li: ListItem) {
        preHtmlNode.apply {
            "li" { process(li) }
        }
    }

    override fun write(h: Heading) {
        preHtmlNode.apply {
            "h${h.level}" {
                val localId = h.id
                if (localId != null) attributes("id" to localId)
                process(h)
            }
        }
    }

    override fun write(p: Paragraph) {
        preHtmlNode.apply {
            val outputTagName = if (p.roles.contains("pre")) "pre" else "p"
            outputTagName {
                val localId = p.id
                if (localId != null) attributes("id" to localId)
                process(p)
            }
        }
    }

    override fun write(doc: Document) {
        preHtmlNode.apply {
            process(doc)
        }
    }

    override fun write(text: Text) {
        val textToAppend =
            if (text.text.trim('\n', '\r').isEmpty()) "${text.text} " else text.text
        preHtmlNode.apply { -textToAppend }
    }

    override fun write(span: Span) {
        preHtmlNode.apply {
            if (span.roles.contains("em") or span.roles.contains("i")) {
                "em" { process(span) }
            } else if (span.roles.contains("strong") or span.roles.contains("b")) {
                "strong" { process(span) }
            } else if (span.roles.contains("mark")) {
                "mark" { process(span) }
            } else if (span.roles.contains("sup")) {
                "sup" { process(span) }
            } else if (span.roles.contains("sub")) {
                "sub" { process(span) }
            } else if (span.roles.contains("code")) {
                "code" { process(span) }
            } else "span" { process(span) }
        }
    }

    override fun write(a: Anchor) {
        preHtmlNode.apply {
            "a" {
                attributes("href" to a.href)
                process(a)
            }
        }
    }

    override fun write(toc: Toc) {
        TODO("Not yet implemented")
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