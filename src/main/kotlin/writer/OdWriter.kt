package writer

import model.*
import org.redundent.kotlin.xml.Node as XmlNode
import org.redundent.kotlin.xml.Namespace
import org.redundent.kotlin.xml.xml
import java.io.File
import java.util.Base64
import kotlin.math.roundToInt

open class OdWriter(
    preOdNode: XmlNode? = null, val odtStyleList: OdtStyleList
) : BackendWriter {
    fun XmlNode.process(node: Node, key: String? = null) {
        odtStyleList.applyStyle(node, this, key)
        node.children().forEach {
            if (it.includeTags.contains("hide")) return@forEach
            val customProcessed = odtStyleList.applyCustomWriter(it, newInstance(this, odtStyleList))
            if (!customProcessed) it.write(newInstance(this, odtStyleList))
        }
    }

    val preOdNode = preOdNode ?: xml("root") {
        namespace(Namespace("text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0"))
        namespace(Namespace("style", "urn:oasis:names:tc:opendocument:xmlns:style:1.0"))
        namespace(Namespace("fo", "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"))
        namespace(Namespace("xlink", "http://www.w3.org/1999/xlink"))
        namespace(Namespace("table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0"))
        namespace(Namespace("svg", "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"))
        namespace(Namespace("draw", "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"))
        namespace(Namespace("office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0"))
        namespace(Namespace("graphic", "urn:oasis:names:tc:opendocument:xmlns:office:1.0"))
    }

    open fun newInstance(xmlNode: XmlNode, odtStyleList: OdtStyleList): OdWriter {
        return OdWriter(xmlNode, odtStyleList)
    }

    override fun write(openBlock: OpenBlock) {
        preOdNode.apply {
            process(openBlock)
        }
    }

    override fun write(image: Image) {
        preOdNode.apply {
            "draw:frame" {
                arrayOf(
                    "svg:width" to image.width, "svg:height" to image.height
                ).forEach {
                    val length = it.second
                    if (length != null) {
                        if (length.unit == LengthUnit.parrots
                            || length.unit == LengthUnit.perc
                        ) throw Exception(
                            "Don't understand ${length.unit} as a length unit" + " see https://en.wikipedia.org/wiki/38_Parrots for details"
                        )
                        if (length.unit == LengthUnit.mmm)
                            attribute(it.first, "${length.value / 100}mm") else
                            attribute(it.first, "${length.value}${length.unit}")
                    }

                }
                val odAnchorType = when (image.anchorType) {
                    AnchorType.ASCHAR -> "as-char"
                }
                attribute("text:anchor-type", odAnchorType)
                val (base64EncodedImage: String, imageType: String) = run {
                    val base64Regex = """^data:image/(.*);base64,(.*)$""".toRegex()
                    val matchResult = base64Regex.matchEntire(image.src)
                    if (matchResult == null) {
                        arrayOf(
                            Base64.getEncoder().encodeToString(File(image.src).readBytes()),
                            File(image.src).extension
                        )
                    } else {
                        arrayOf(
                            matchResult.groupValues[2],
                            matchResult.groupValues[1]
                        )
                    }
                }
                graphicProperties {
                    attribute("style:vertical-pos", "middle")
                    attribute("style:vertical-rel", "text")
                }
                "draw:image" {
                    attribute("draw:mime-type", "image/${imageType}")
                    "office:binary-data" {
                        -base64EncodedImage
                    }
                }
                process(image)
            }
        }
    }

    override fun write(table: Table) {
        preOdNode.apply {
            "table:table" {
                process(table)
            }
        }
    }

    override fun write(tableRowGroup: TableRowGroup) {
        preOdNode.apply {
            if (tableRowGroup.type == TRG.head) {
                "table:table-header-rows" {
                    process(tableRowGroup)
                }
            } else process(tableRowGroup)
        }
    }

    override fun write(tr: TableRow) {
        preOdNode.apply {
            "table:table-row" {
                process(tr)
            }
        }
    }

    override fun write(td: TableCell) {
        preOdNode.apply {
            "table:table-cell" {
                attributes(
                    "table:number-columns-spanned" to td.colspan,
                    "table:number-rows-spanned" to td.rowspan
                )
                process(td)
            }
        }
    }

    override fun write(colGroup: ColGroup) {
        preOdNode.apply {
            process(colGroup)
        }
    }

    override fun write(col: Col) {
        preOdNode.apply {
            "table:table-column" {
                // TODO: allow absolute units
                // Open Document doesn't allow to mix rel and absolute units like html
                // And we can't render styles to understand widths
                // The approximate algorithm needed that ignores paddings
                if (col.width.unit != LengthUnit.perc) throw Exception("Only relative width for columns supported")
                val relColumnWidth = col.width.value * 100
                tableProperties { attribute("style:rel-column-width", "$relColumnWidth*") }
            }
        }
    }

    override fun write(ol: OrderedList) {
        preOdNode.apply {
            "text:list" {
                attribute("text:style-name", "Numbering 123")
                process(ol)
            }
        }
    }

    override fun write(ul: UnorderedList) {
        preOdNode.apply {
            "text:list" {
                attribute("text:style-name", "List 1")
                process(ul)
            }
        }
    }

    override fun write(li: ListItem) {
        preOdNode.apply {
            "text:list-item" {
                process(li)
            }
        }
    }

    override fun write(h: Header) {
        // todo -- to example
        preOdNode.apply {
            "text:h" {
                attribute("text:style-name", "Heading ${h.level}")
                attribute("text:outline-level", h.level)
                val id = h.id
                if (id != null) {
                    "text:bookmark-start" {
                        attribute("text:name", id)
                    }
                    process(h)
                    "text:bookmark-end" {
                        attribute("text:name", id)
                    }
                } else process(h)
            }
        }
    }

    override fun write(p: Paragraph) {
        preOdNode.apply {
            "text:p" {
                when (p.sourceTagName) {
                    "pre" -> attribute("text:style-name", "Preformatted Text")
                    else -> attribute("text:style-name", "Text body")
                }
                val id = p.id
                if (id != null) {
                    "text:bookmark-start" {
                        attribute("text:name", id)
                    }
                    process(p)
                    "text:bookmark-end" {
                        attribute("text:name", id)
                    }
                } else process(p)
            }
        }
    }

    override fun write(doc: Document) {
        preOdNode.apply {
            process(doc)
        }
    }

    override fun write(text: Text) {
        preOdNode.apply {
            // todo: bug in XML builder?, outputs nothing for space between tags
            var textChunk = ""
            var spacesNum = 0
            text.text.map { it.toString() }.forEach { character ->
                if (character == " ") {
                    if (text.ancestor { it.sourceTagName == "pre" }
                            .isNotEmpty()) {
                        if (textChunk != "") {
                            -textChunk
                            textChunk = ""
                        }
                        spacesNum += 1
                    } else {
                        textChunk += " "
                    }
                } else if (character == "\t") {
                    if (spacesNum > 0) {
                        "text:s" { attribute("text:c", "$spacesNum") }
                        spacesNum = 0
                    } else {
                        -textChunk
                        textChunk = ""
                    }
                    "text:tab" {}
                } else if (character == "\n") {
                    if (spacesNum > 0) {
                        "text:s" { attribute("text:c", "$spacesNum") }
                        spacesNum = 0
                    }
                    if (text.ancestor { it.sourceTagName == "pre" }
                            .isNotEmpty()) {
                        -textChunk
                        textChunk = ""
                        "text:line-break" { }
                    } else {
                        textChunk += character
                    }
                } else {
                    if (spacesNum > 0) {
                        "text:s" { attribute("text:c", "$spacesNum") }
                        spacesNum = 0
                    }
                    textChunk += character
                }
            }
            if (spacesNum > 0) {
                "text:s" { attribute("text:c", "$spacesNum") }
                spacesNum = 0
            }
            if (textChunk != "") -textChunk
        }
    }

    override fun write(span: Span) {
        preOdNode.apply {
            if (span.parent()?.sourceTagName == "pre" && span.roles.contains("code")) {
                process(span)
                return@apply
            }
            "text:span" {
                if (span.roles.contains("code")) attribute("text:style-name", "Source Text")
                textProperties {
                    if (span.roles.contains("em") or span.roles.contains("i")) {
                        attribute("fo:font-style", "italic")
                    }
                    if (span.roles.contains("strong") or span.roles.contains("b")) {
                        attribute("fo:font-weight", "bold")
                    }
                    arrayOf("super", "sub").forEach { role ->
                        if (span.roles.contains(role.substring(0, 3)))
                            attribute("style:text-position", "$role 58%")
                    }
                }
                process(span)
            }
        }
    }

    override fun write(a: Anchor) {
        preOdNode.apply {
            "text:a" {
                attribute("xlink:href", a.href)
                attribute("text:style-name", "Internet Link")
                attribute("text:visited-style-name", "Visited Internet Link")
                process(a)
            }
        }
    }

    override fun write(toc: Toc) {
        preOdNode.apply {
            "text:table-of-content" {
                "text:table-of-content-source" {
                    attributes("text:outline-level" to toc.levels)
                    "text:index-title-template" {
                        process(toc.titleNode)
                    }
                }
            }
        }
    }

    override fun write(textFrame: TextFrame) {
        preOdNode.apply {
            "text:p" {
                textProperties {
                    attributes("fo:font-size" to "0pt")
                }
                paragraphProperties {
                    attributes("fo:line-height" to "100%")
                }
                odtStyleList.applyStyle(textFrame, this, "p")
                "draw:frame" {
                    attributes(
                        "draw:style-name" to "Frame",
                        "style:rel-width" to "100%",
                        "text:anchor-type" to "paragraph"
                    )
                    graphicProperties {
                        attributes(
                            "fo:padding" to "0mm", "fo:border" to "none",
                            "fo:margin-top" to "0mm", "fo:margin-left" to "0mm", "fo:margin-right" to "0mm",
                            "fo:margin-bottom" to "2mm",
                            "style:horizontal-rel" to "paragraph"
                        )
                    }
                    odtStyleList.applyStyle(textFrame, this, "frame")
                    "draw:text-box" {
                        process(textFrame, "text-box")
                    }
                }
            }
        }
    }

    override fun write(footnote: Footnote) {
        preOdNode.apply {
            "text:note" {
                attributes(
                    "text:id" to footnote.footnoteId,
                    "text:note-class" to "footnote",
                    "text:note-citation" to footnote.footnoteId
                )
                "text:note-body" {
                    "text:p" {
                        process(footnote)
                    }
                }
            }
        }
    }

    override fun write(footnoteRef: FootnoteRef) {
        preOdNode.apply {
            "text:span" {
                attributes("text:style-name" to "Footnote anchor")
                "text:note-ref" {
                    attributes(
                        "text:note-class" to "footnote",
                        "text:ref-name" to footnoteRef.footnote.footnoteId,
                        "text:reference-format" to "text"
                    )
                    -footnoteRef.footnote.footnoteId
                }
                odtStyleList.applyStyle(footnoteRef, this)
            }
        }
    }

    override fun write(dummyNode: Node) {
        println("Error: no writer available")
    }
}
