package converter

import model.*
import reader.HtmlNode
import reader.UnknownTagProcessing

object AsciidoctorAdapterCommon {
    val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing = {
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


}