package common

import model.*
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.*

object AsciidoctorAdapter : GenericAdapter {
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

    override val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing = {
        if (classNames().intersect(
                setOf(
                    "listingblock",
                    "admonitionblock",
                    "exampleblock",
                    "imageblock",
                    "paragraph",
                    "book",
                    "article",
                    "title",
                    "footnotes",
                    "footnote",
                    "dlist",
                    "olist",
                    "ulist",
                    "content",
                    *(1..6).map { "sect$it" }.toTypedArray(),
                )
            ).isNotEmpty()
        ) {
            UnknownTagProcessing.PROCESS
        } else if (setOf("dl", "dt", "dd").contains(nodeName())) {
            UnknownTagProcessing.PROCESS
        } else if (nodeName() == "div") {
            UnknownTagProcessing.PASS
        } else UnknownTagProcessing.UNDEFINDED
    }

    override fun basicStyle(): OdtStyleList {
        return OdtStyleList(
            OdtStyle { genericTitle ->
                if (genericTitle !is Paragraph) return@OdtStyle
                val genericTitleWrapper = genericTitle.parent()
                if (genericTitleWrapper == null ||
                    !genericTitleWrapper.roles.contains("title")
                ) return@OdtStyle
                attributes("text:style-name" to "Text")
            },
            OdtStyle { tableCell ->
                if (tableCell !is TableCell) return@OdtStyle
                tableCellProperties {
                    arrayOf("top", "right", "bottom", "left").forEach {
                        attribute("fo:border-$it", "0.5pt solid #000000")
                        if (it != "bottom") attribute("fo:padding-$it", "1mm")
                    }
                    if (tableCell.sourceTagName == "th") attribute("fo:background-color", "#eeeeee")
                }
                arrayOf("top", "middle", "bottom").forEach {
                    if (tableCell.roles.contains("valign-$it"))
                        tableCellProperties { attributes("style:vertical-align" to it) }
                }
            },
            OdtStyle { paragraph ->
                if (paragraph !is Paragraph) return@OdtStyle
                if (paragraph.ancestor { it is TableCell }.isEmpty()) return@OdtStyle
                attributes("text:style-name" to "Table Contents")
            },
            OdtStyle { paragraph ->
                if (paragraph !is Paragraph) return@OdtStyle
                arrayOf("center", "left", "right").forEach {
                    if (paragraph.parent()?.roles?.contains("halign-$it") == true)
                        paragraphProperties { attributes("fo:text-align" to it) }
                }
            },
            OdtStyle { tableCell ->
                if (tableCell !is TableCell) return@OdtStyle
                val nodeParent = tableCell.parent() ?: return@OdtStyle
                lightColors.forEach { color ->
                    if (nodeParent.descendant()
                            .map { it.roles }.reduce { acc, roles ->
                                ArrayList(acc + roles)
                            }.contains("row-background-color-${color.first.toString().lowercase()}")
                    ) {
                        tableCellProperties { attributes("fo:background-color" to color.second) }
                    }
                }

            },
            OdtStyle { table ->
                if (table !is Table) return@OdtStyle
                if (table.ancestor { it is Table }.isEmpty())
                    tableProperties { attributes("fo:margin-bottom" to "2mm") } else
                    tableProperties { attributes("fo:margin-bottom" to "1mm") }
            },
            OdtStyle { admonitionTypeContentCell ->
                if (admonitionTypeContentCell !is TableCell) return@OdtStyle
                val admonitionCellType = run {
                    if (admonitionTypeContentCell.roles.contains("admonition-type-content")) {
                        AdmonitionCellType.TYPE_CONTENT
                    } else if (admonitionTypeContentCell.roles.contains("admonition-content"))
                        AdmonitionCellType.CONTENT
                    else null
                }
                if (admonitionCellType == null) return@OdtStyle
                val admonitionBlockRoles = admonitionTypeContentCell
                    .ancestor { it.roles.contains("admonitionblock") }
                    .firstOrNull()
                    ?.roles ?: throw Exception("Not in an admonition block")
                admonitionColors.filter { admonitionBlockRoles.contains(it.first) }
                    .first()
                    .apply {
                        tableCellProperties {
                            val color =
                                lightColors.toMap()[second]
                                    ?: throw Exception("Color ${second} not found")
                            if (admonitionCellType == AdmonitionCellType.TYPE_CONTENT)
                                attributes("fo:background-color" to color)
                            arrayOf("left", "right", "top", "bottom").forEach {
                                attributes("fo:border-$it" to "0.7pt solid $color")
                            }
                        }
                    }
            },
            OdtStyle { admonitionTitleParagraph ->
                if (admonitionTitleParagraph !is Paragraph) return@OdtStyle
                if (admonitionTitleParagraph.ancestor { it.roles.contains("admonition-content") }.isEmpty())
                    return@OdtStyle
                if (admonitionTitleParagraph.ancestor { it.roles.contains("title") }.isEmpty())
                    return@OdtStyle
                textProperties { attributes("fo:font-style" to "italic") }
            },
            OdtStyle { titleParagraph ->
                if (titleParagraph !is Paragraph) return@OdtStyle
                if (!titleParagraph.roles.contains("doctitle")) return@OdtStyle
                attributes("text:style-name" to "Title")
            },
            OdtStyle { figureTitle ->
                if (figureTitle !is Paragraph) return@OdtStyle
                if (figureTitle.ancestor { it.roles.contains("imageblock") }.isEmpty()) return@OdtStyle
                if (figureTitle.ancestor { it.roles.contains("title") }.isEmpty()) return@OdtStyle
                attributes("text:style-name" to "Figure")
            },
            OdtStyle { imageblockParagraph ->
                if (imageblockParagraph !is Paragraph) return@OdtStyle
                if (!imageblockParagraph.roles.contains("imageblock-paragraph"))
                    return@OdtStyle
                attributes("text:style-name" to "Image Block")
                val imageBlockRoot =
                    imageblockParagraph.ancestor { it.roles.contains("imageblock") }
                        .firstOrNull()
                if (imageBlockRoot != null &&
                    imageBlockRoot.descendant { it.roles.contains("title") }.isNotEmpty()
                ) paragraphProperties { attributes("fo:keep-with-next" to "always") }
            },
            OdtStyle { tableTitle ->
                if (tableTitle !is Paragraph) return@OdtStyle
                val parent = tableTitle.parent()
                if (parent != null && parent.roles.contains("table-title")) {
                    attributes("text:style-name" to "Table")
                }
            },
            OdtStyle { listTitle ->
                if (listTitle !is Paragraph) return@OdtStyle
                val listTitleWrapper = listTitle.parent() ?: return@OdtStyle
                if (!listTitleWrapper.roles.contains("title")) return@OdtStyle
                if (!listTitleWrapper.hasNext()) return@OdtStyle
                val listTitleWrapperNextSibling = listTitleWrapper.next()
                if (listTitleWrapperNextSibling !is UnorderedList &&
                    listTitleWrapperNextSibling !is OrderedList
                ) return@OdtStyle
                if (listTitle.ancestor { it is Table }.isEmpty())
                    attributes("text:style-name" to "Text body") else
                    attributes("text:style-name" to "Table Contents")
                paragraphProperties { attributes("fo:keep-with-next" to "always") }
            },
            OdtStyle { listingTitle ->
                if (listingTitle !is Paragraph) return@OdtStyle
                val listingTitleWrapper = listingTitle.parent() ?: return@OdtStyle
                if (!listingTitleWrapper.roles.contains("title")) return@OdtStyle
                if (!listingTitleWrapper.hasNext()) return@OdtStyle
                val listingTitleWrapperNextSibling = listingTitleWrapper.next()
                if (!listingTitleWrapperNextSibling.roles.contains("pre")) return@OdtStyle
                attributes("text:style-name" to "Text")
            },
            OdtStyle { paragraph ->
                if (paragraph !is Paragraph) return@OdtStyle
                if (!paragraph.roles.contains("definition-term")) return@OdtStyle
                attributes("text:style-name" to "Definition Term")
            },
            OdtStyle { paragraph ->
                if (paragraph !is Paragraph) return@OdtStyle
                if (!paragraph.roles.contains("horizontal-line-for-example")) return@OdtStyle
                attributes("text:style-name" to "Horizontal Line for Example")
            },
            OdtStyle { node ->
                if (node.ancestor { it.roles.contains("keep-with-next") }.isNotEmpty()
                    || node.roles.contains("keep-with-next")
                ) {
                    if (node is Paragraph)
                        paragraphProperties { attributes("fo:keep-with-next" to "always") }
                    if (node is Table)
                        tableProperties { attributes("fo:keep-with-next" to "always") }
                    if (node is TextFrame)
                        graphicProperties { attributes("fo:keep-with-next" to "always") }
                }
            },
            OdtStyle { span ->
                if (span !is Span) return@OdtStyle
                darkColors.forEach {
                    if (span.roles.contains(it.first.toString().lowercase())) {
                        textProperties { attributes("fo:color" to it.second) }
                    }
                }
            },
            OdtStyle { paragraph ->
                if (paragraph !is Paragraph) return@OdtStyle
                if (!paragraph.roles.contains("text-frame-wrapper-paragraph")) return@OdtStyle
                textProperties { attributes("fo:font-size" to "0pt") }
                paragraphProperties { attributes("fo:font-size" to "0pt") }
            }
        )
    }

    fun Node.trimSimpleTdContent() {
        this.descendant { it is TableCell }.map { it as TableCell }
            .forEach { tableCell ->
                if (tableCell.children().size == 1 && tableCell.children()[0] is Text) {
                    val textNode = tableCell.children()[0] as Text
                    textNode.text = textNode.text.trim()
                }
            }
    }

    @JvmStatic
    fun Node.wrapTableCellInlineContent() {
        this.descendant { it is TableCell }.map { it as TableCell }
            .forEach { tableCell -> tableCell.wrapNodeInlineContents() }
    }

    @JvmStatic
    fun Node.wrapTitleInlineContent() {
        this.descendant { it is OpenBlock && it.roles.contains("title") }
            .forEach { openBlock ->
                openBlock.wrapNodeInlineContents()
                val table = openBlock.parent()
                if (table != null && table is Table) {
                    val tableId = table.id
                    if (tableId != null) {
                        openBlock.descendant { it is Paragraph }.firstOrNull()?.apply {
                            id = tableId
                        }
                        table.id = "table--${tableId}"
                    }
                    table.insertBefore(openBlock)
                    openBlock.roles("table-title")
                }
            }
    }

    @JvmStatic
    fun Node.extractToc() {
        val parsedToc = this.descendant { it.id == "toc" }
            .firstOrNull() ?: return
        val toc = Toc().apply {
            levels = parsedToc.descendant { it is UnorderedList }
                .maxOfOrNull { ul -> ul.ancestor { it is UnorderedList }.size + 1 }
                ?: throw Exception("Can't determine number of levels in the TOC")
            titleNode = Paragraph().apply {
                parsedToc.descendant { it.parent()?.id == "toctitle" }.forEach {
                    appendChild(it)
                }
            }
        }
        parsedToc.insertBefore(toc)
        parsedToc.remove()
    }


    fun Node.transformAdmonitions() {
        this.descendant { it.roles.contains("admonitionblock") }.forEach { block ->
            val oldTable = block.children().first { it is Table }
            val (typeContent, content) = run {
                oldTable
                    .children().first { it is TableRowGroup }
                    .children().first { it is TableRow }
                    .apply {
                        if (children().size != 2 ||
                            children().any { it !is TableCell }
                        ) throw Exception("Can't define admonition block")
                    }.children()
            }
            typeContent.roles("admonition-type-content")
            content.roles("admonition-content")
            block.apply {
                table {
                    col(Length(100F))
                    tableRowGroup(TRG.body) {
                        tr { appendChild(typeContent) }
                        tr { appendChild(content) }
                    }
                }
            }
            oldTable.remove()
        }
    }

    @JvmStatic
    fun Node.normalizeHeaders() {
        this.descendant { it is Header }.map { it as Header }.forEach { header ->
            if (header.level == 1) {
                val titleParagraph = Paragraph().apply {
                    roles("doctitle")
                    header.children().forEach { appendChild(it) }
                }
                header.insertBefore(titleParagraph)
                header.remove()
            } else header.level -= 1
        }
    }

    @JvmStatic
    fun Node.wrapTablesInLists() {
        this.descendant { it is ListItem }.forEach { listItem ->
            listItem.descendant().forEach liChildren@{ table ->
                if (table !is Table) return@liChildren
                if (table.parent() is TextFrame) return@liChildren
                val textFrame = TextFrame()
                val textFrameWrapperParagraph = Paragraph().apply {
                    roles("text-frame-wrapper-paragraph")
                    appendChild(textFrame)
                }
                table.insertBefore(textFrameWrapperParagraph)
                textFrame.appendChild(table)
            }
        }
    }


    @JvmStatic
    fun Node.extractFootnotes() {
        val footnotes: MutableMap<String, Footnote> = mutableMapOf()
        this.descendant { it is Span && it.roles.contains("footnote") }
            .forEach { footNoteSpan ->
                val footnoteId = footNoteSpan.id ?: throw Exception("Footnote id not found")
                val footnoteHref = footNoteSpan.descendant { it is Anchor }.map { it as Anchor }
                    .firstOrNull()
                    .apply { if (this == null) throw Exception("Anchor to footnote not found") }!!
                    .href
                    .substring(1)
                val footnoteBody = footNoteSpan.ancestor().last()
                    .descendant { it.id == footnoteHref }
                    .firstOrNull()
                    .apply { if (this == null) throw Exception("Footnote $footnoteHref not found") }!!
                    .children()
                    .mapIndexed { i, el ->
                        if (i == 0 && el is Anchor) {
                            null
                        } else if (i == 1 && el is Text) {
                            Text(el.text.substring(2))
                        } else {
                            el
                        }
                    }.filterNotNull()
                val footnote = Footnote(footnoteId).apply {
                    footnoteBody.forEach { this.appendChild(it) }
                }
                footNoteSpan.insertAfter(footnote)
                footNoteSpan.remove()
                footnotes[footnoteHref] = footnote
            }
        this.descendant { it is Span && it.roles.contains("footnoteref") }
            .forEach { footnoteRefSpan ->
                val footnoteToRefHref = footnoteRefSpan.descendant { it is Anchor }.map { it as Anchor }
                    .firstOrNull()
                    .apply { if (this == null) throw Exception("Anchor to footnote reference not found") }!!
                    .href
                    .substring(1)
                val footnoteToRef = footnotes[footnoteToRefHref]
                    ?: throw Exception(
                        "Can't find footnote ${footnoteRefSpan.id} to reference"
                    )
                val footnoteRef = FootnoteRef(footnoteToRef)
                footnoteRefSpan.insertAfter(footnoteRef)
                footnoteRefSpan.remove()
            }
    }

    fun Node.wrapBlockImages() {
        this.descendant { image ->
            image is Image &&
                    image.ancestor { it.roles.contains("imageblock") }
                        .isNotEmpty()
        }.forEach { image ->
            val imageBlock = image.ancestor { it.roles.contains("imageblock") }.firstOrNull()
            val p =
                Paragraph().apply { roles("imageblock-paragraph") }
            if (imageBlock?.id != null) {
                p.id = imageBlock.id
                imageBlock.id = "image-block--${imageBlock.id}"
            }
            image.insertAfter(p)
            p.appendChild(image)
        }
    }

    fun Node.transformDefinitionList() {
        this.descendant { it.sourceTagName == "dt" }.forEach { dt ->
            dt.apply {
                val paragraph = Paragraph().apply {
                    dt.children().forEach { appendChild(it) }
                    roles("definition-term")
                }
                insertAfter(paragraph)
                remove()
            }
        }
    }

    fun Node.transformExamples() {
        descendant { it.roles.contains("exampleblock") }.forEach { exampleBlock ->
            exampleBlock.descendant { it.roles.contains("content") }.forEach { exampleContent ->
                exampleContent.appendFirstChild(
                    Paragraph().apply {
                        roles("keep-with-next", "horizontal-line-for-example")
                    }
                )
                exampleContent.p { roles("horizontal-line-for-example") }
                    .previous().roles("keep-with-next")
            }
        }
    }

    override fun Node.normalizeAll() {
        trimSimpleTdContent()
        extractToc()
        wrapTableCellInlineContent()
        wrapTitleInlineContent()
        wrapBlockImages()
        normalizeHeaders()
        transformAdmonitions()
        transformExamples()
        transformDefinitionList()
        extractFootnotes()
        wrapTablesInLists()
        normalizeImageDimensions()
        setImageBestFitDimensions()
    }
}