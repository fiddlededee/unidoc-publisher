package converter.fo



import common.normalizeImageDimensions
import common.setImageBestFitDimensions
import converter.AsciidoctorAdapterCommon
import model.*
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.*

object AsciidoctorFoAdapter : GenericFoAdapter {

    class PageNumberCitation(var refId: String) : NoWriterNode() {
        override val isInline: Boolean get() = true
    }

    class Leader(var pattern: String) : NoWriterNode() {
        override val isInline: Boolean get() = true
    }

    enum class Color { RED, GREEN, YELLOW, BLUE, ORANGE }

    var lightColors = arrayOf(
        Color.RED to "#f8ced0", Color.GREEN to "#c2f3d3",
        Color.YELLOW to "#fef0c2", Color.BLUE to "#c2f3f1",
        Color.ORANGE to "#fedaca"
    )
    var darkColors = arrayOf(
        Color.RED to "#8f141b", Color.GREEN to "#19803d",
        Color.YELLOW to "#a17c02", Color.BLUE to "#19807c",
        Color.ORANGE to "#a03203"
    )
    var admonitionColors = arrayOf(
        "note" to Color.BLUE,
        "tip" to Color.GREEN,
        "important" to Color.YELLOW,
        "caution" to Color.ORANGE,
        "warning" to Color.RED
    )

    enum class AdmonitionCellType { TYPE_CONTENT, CONTENT }

    override val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing =
        AsciidoctorAdapterCommon.unknownTagProcessingRule

    override fun basicStyle(): FoStyleList {
        return FoStyleList(
            FoCustomWriter {
                if (it !is PageNumberCitation) return@FoCustomWriter
                preFoNode.apply {
                    "fo:page-number-citation" { attributes("ref-id" to it.refId) }
                }
            },
            FoCustomWriter {
                if (it !is Leader) return@FoCustomWriter
                preFoNode.apply {
                    "fo:leader" {
                        attributes(
                            "leader-pattern" to it.pattern,
                            "leader-alignment" to "reference-area",
                        )
                    }
                }
            },
            FoStyle { p ->
                if (p !is Paragraph) return@FoStyle
                attributes("space-after" to "2mm")
            },
            FoStyle { p ->
                if (p !is Paragraph) return@FoStyle
                if (!p.roles.contains("toc")) return@FoStyle
            },
            FoStyle { p ->
                if (p !is Paragraph) return@FoStyle
                if (!p.roles.contains("contents-name")) return@FoStyle
                attributes("text-align-last" to "justify", "space-after" to "0mm")
            },
            FoStyle { p ->
                if (p !is Paragraph) return@FoStyle
                if (!p.roles.contains("toc-paragraph")) return@FoStyle
                attributes("font-weight" to "bold", "font-size" to "14pt")
            },
            FoStyle { p ->
                if (p !is Paragraph) return@FoStyle
                if (!p.roles.contains("contents")) return@FoStyle
                if (p.roles.contains("contents-1"))
                    attributes("font-weight" to "bold")
                if (p.roles.contains("contents-2"))
                    attributes("start-indent" to "10mm")
            },
            FoStyle { openBlock ->
                if (openBlock !is OpenBlock) return@FoStyle
                if (!openBlock.roles.contains("toc-block")) return@FoStyle
                attributes("space-after" to "4mm")
            },
            FoStyle { listTitle ->
                if (listTitle !is Paragraph) return@FoStyle
                val listTitleWrapper = listTitle.parent() ?: return@FoStyle
                if (!listTitleWrapper.roles.contains("title")) return@FoStyle
                if (!listTitleWrapper.hasNext()) return@FoStyle
                val listTitleWrapperNextSibling = listTitleWrapper.next()
                if (listTitleWrapperNextSibling !is UnorderedList &&
                    listTitleWrapperNextSibling !is OrderedList
                ) return@FoStyle
                attributes("fo:keep-with-next" to "always")
            },
            FoStyle { listingTitle ->
                if (listingTitle !is Paragraph) return@FoStyle
                val listingTitleWrapper = listingTitle.parent() ?: return@FoStyle
                if (!listingTitleWrapper.roles.contains("title")) return@FoStyle
                if (!listingTitleWrapper.hasNext()) return@FoStyle
                val listingTitleWrapperNextSibling = listingTitleWrapper.next()
                if (!listingTitleWrapperNextSibling.roles.contains("pre")) return@FoStyle
                attributes("fo:keep-with-next" to "always")
            },
            FoStyle { tableTitle ->
                if (tableTitle !is Paragraph) return@FoStyle
                val parent = tableTitle.parent()
                if (parent != null && parent.roles.contains("table-title")) {
                    attributes(
                        "font-style" to "italic", "text-align" to "right",
                        "space-after" to "0.5mm", "keep-with-next" to "always"
                    )
                }
            },
            FoStyle { tableCell ->
                if (tableCell !is TableCell) return@FoStyle
                if (tableCell.ancestor { it is Table }.first().roles.contains("toc")) return@FoStyle
                attributes("border" to "0.7pt solid #000000", "padding" to "1mm")
            },
            FoStyle { figureTitle ->
                if (figureTitle !is Paragraph) return@FoStyle
                if (figureTitle.ancestor { it.roles.contains("imageblock") }.isEmpty()) return@FoStyle
                if (figureTitle.ancestor { it.roles.contains("title") }.isEmpty()) return@FoStyle
                attributes("font-style" to "italic", "text-align" to "center")
            },
            FoStyle { imageblockParagraph ->
                if (imageblockParagraph !is Paragraph) return@FoStyle
                if (!imageblockParagraph.roles.contains("imageblock-paragraph"))
                    return@FoStyle
                attributes("text-align" to "center")
                val imageBlockRoot =
                    imageblockParagraph.ancestor { it.roles.contains("imageblock") }
                        .firstOrNull()
                if (imageBlockRoot != null &&
                    imageBlockRoot.descendant { it.roles.contains("title") }.isNotEmpty()
                ) attributes("keep-with-next" to "always")
            },
            FoStyle { table ->
                if (table !is Table) return@FoStyle
                attributes("space-after" to "3mm")
            },
            FoStyle { admonitionTypeContentCell ->
                if (admonitionTypeContentCell !is TableCell) return@FoStyle
                val admonitionCellType = run {
                    if (admonitionTypeContentCell.roles.contains("admonition-type-content")) {
                        converter.fodt.AsciidoctorOdAdapter.AdmonitionCellType.TYPE_CONTENT
                    } else if (admonitionTypeContentCell.roles.contains("admonition-content"))
                        converter.fodt.AsciidoctorOdAdapter.AdmonitionCellType.CONTENT
                    else null
                }
                if (admonitionCellType == null) return@FoStyle
                attributes("padding" to "1mm")
                val admonitionBlockRoles = admonitionTypeContentCell
                    .ancestor { it.roles.contains("admonitionblock") }
                    .firstOrNull()
                    ?.roles ?: throw Exception("Not in an admonition block")
                admonitionColors.filter { admonitionBlockRoles.contains(it.first) }
                    .first()
                    .apply {
                        val color =
                            lightColors.toMap()[second]
                                ?: throw Exception("Color ${second} not found")
                        if (admonitionCellType == converter.fodt.AsciidoctorOdAdapter.AdmonitionCellType.TYPE_CONTENT)
                            attributes("background-color" to color)
                        arrayOf("left", "right", "top", "bottom").forEach {
                            attributes("border-$it" to "0.7pt solid $color")
                        }
                    }
            },
            FoStyle { p ->
                if (p !is Paragraph) return@FoStyle
                if (p.sourceTagName != "pre") return@FoStyle
                attributes(
                    "white-space" to "pre", "font-family" to "Liberation Mono",
                    "font-size" to "9pt", "background-color" to "#EEEEEE",
                    "border-before-style" to "solid", "border-before-color" to "#bbbbbb",
                    "padding-top" to "1mm",
                    "border-after-style" to "solid", "border-after-color" to "#bbbbbb",
                    "padding-bottom" to "1mm",
                    "space-after" to "3mm"
                )
            },
            FoStyle { codeSpan ->
                if (codeSpan !is Span) return@FoStyle
                if (!codeSpan.roles.contains("code")) return@FoStyle
                if (codeSpan.ancestor { it is Paragraph && it.sourceTagName == "pre" }.isNotEmpty())
                    return@FoStyle
                attributes(
                    "font-family" to "Liberation Mono", "color" to "#0066CC",
                    "font-size" to "11px",
                )
            },
            FoStyle { anchor ->
                if (anchor !is Anchor) return@FoStyle
                if (anchor.roles.contains("page-number-in-toc")) return@FoStyle
                attributes("color" to "#0066CC")
            },
            FoStyle { h ->
                if (h !is Heading) return@FoStyle
                attributes("keep-with-next" to "always")
                if (h.level == 1) {
                    attributes(
                        "space-after" to "4mm", "text-align" to "center",
                        "font-family" to "Liberation Sans", "font-size" to "16px"
                    )
                } else if (h.level == 2) {
                    attributes(
                        "space-before" to "4mm", "space-after" to "2mm",
                        "font-weight" to "bold", "font-size" to "14px"
                    )
                } else if (h.level == 3) {
                    attributes(
                        "space-after" to "2mm",
                        "font-weight" to "bold", "font-size" to "12px"
                    )
                }
            },
            FoStyle { list ->
                if (list !is OrderedList && list !is UnorderedList) return@FoStyle
                attributes(
                    "provisional-distance-between-starts" to "6mm", "start-indent" to "4mm",
                    "space-after" to "2mm"
                )
            },
            FoStyle("list-item-body") { listItem ->
                if (listItem !is ListItem) return@FoStyle
                attributes("start-indent" to "body-start()")
            },
            FoStyle { listItem ->
                if (listItem !is ListItem) return@FoStyle
                attributes("space-after" to "2mm")
            },
            FoStyle("list-item-label") {
                attributes(
                    "end-indent" to "label-end()",
                    "text-align" to "right"
                )
            }
        )
    }

    @JvmStatic
    fun Node.generateToc() {
        val node = this
        descendant { it is Toc }.forEach { toc ->
            val container = OpenBlock().apply {
                roles("toc-block")
                p { roles("toc-paragraph"); +"Table of contents" }
                node.descendant { it is Heading && it.level != 1 }.map { it as Heading }.forEach { h ->
                    p {
                        roles("contents", "contents-${h.level - 1}", "contents-name")
                        roles("toc")
                        span {
                            +"${h.extractText()} "
                        }
                        appendChild(Leader("dots"))
                        +" "
                        a("#${h.id}") {
                            roles("page-number-in-toc")
                            appendChild(PageNumberCitation(h.id ?: throw Exception("No id for Header")))
                        }
                    }
                }
            }
            toc.apply { insertAfter(container); remove() }
        }
    }

    override fun Node.normalizeAll() {
        AsciidoctorAdapterCommon.apply { extractToc() }
        AsciidoctorAdapterCommon.apply {
            wrapTableCellInlineContent()
            moveIdToParagraph()
            wrapTitleInlineContent()
            wrapBlockImages()
        }
        AsciidoctorAdapterCommon.apply { transformAdmonitions() }
        normalizeImageDimensions()
        setImageBestFitDimensions()
        generateToc()
    }
}