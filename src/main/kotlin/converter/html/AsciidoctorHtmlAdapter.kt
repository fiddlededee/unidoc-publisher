package converter.html

import common.normalizeImageDimensions
import common.setImageBestFitDimensions
import converter.AsciidoctorAdapterCommon
import converter.fodt.AsciidoctorOdAdapter
import model.*
import reader.HtmlNode
import reader.UnknownTagProcessing
import writer.*

object AsciidoctorHtmlAdapter : GenericHtmlAdapter {

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

    override fun basicStyle(): HtmlStyleList {
        return HtmlStyleList(
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
                        a("#${h.id}") {
                            roles("toc-item")
                            +"${h.extractText()} "
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