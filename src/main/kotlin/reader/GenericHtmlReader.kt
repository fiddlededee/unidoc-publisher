package reader

import model.*

enum class UnknownTagProcessing { PROCESS, PASS, UNDEFINDED }
open class GenericHtmlReader(
    node: Node,
    htmlNode: HtmlNode,
    private val unknownTagProcessingRule: HtmlNode.() -> UnknownTagProcessing = { UnknownTagProcessing.UNDEFINDED },
    private val customNodeProcessingRule: Node.(htmlNode: HtmlNode) -> Unit = {}
) : HtmlReaderCommon(node, htmlNode) {

    fun parseNode(astNode: Node, confirmedHtmlNode: HtmlNode) {
        GenericHtmlReader(astNode, confirmedHtmlNode, unknownTagProcessingRule).iterateAll()
    }

    fun Node.setBasics(htmlNode: HtmlNode): Node {
        id = htmlNode.id()
        roles += htmlNode.classNames()
        if (sourceTagName == null) {
            sourceTagName = htmlNode.nodeName()
        }
        apply { customNodeProcessingRule(htmlNode) }
        htmlNode.classNames().forEach {
            val re = "^tag(?:--|—​)([a-zA-Z][a-zA-Z0-9_-]*)$".toRegex()
            val matchResult = re.matchEntire(it)
            if (matchResult != null) {
                includeTags.add(matchResult.groupValues[1])
            }
        }
        return this
    }

    protected open val detectors = arrayOf(
        ::detectImage,
        ::detectTable,
        ::detectRowGroup,
        ::detectRow,
        ::detectColGroup,
        ::detectCol,
        ::detectCell,
        ::detectOl,
        ::detectUl,
        ::detectLi,
        ::detectH,
        ::detectP,
        ::detectPre,
        ::detectCode,
        ::passComment,
        ::passHr,
        ::detectSpan,
        ::detectA,
        ::detectText,
        ::detectOpenBlock,
        ::passOtherTag,
    )

    open fun iterateAll() {
        detectBy(*detectors)
    }

    open fun passComment() {
        detectByExpression({ it.isComment() }) {}
    }

    open fun passHr() {
        detectByExpression({ it.nodeName() == "hr" }) {}
    }

    open fun passOtherTag() {
        detectByExpression({ unknownTagProcessingRule.invoke(it) == UnknownTagProcessing.PASS }) { confirmedHtmlNode ->
            parseNode(this.node(), confirmedHtmlNode)
        }
    }

    open fun detectImage() {
        detectByExpression({ it.nodeName() == "img" }) { confirmedNode ->
            val srcAttrValue = confirmedNode.attr("src")
            val widthAttrValue = confirmedNode.attr("width")
            val heightAttrValue = if (confirmedNode.hasAttr("height")) confirmedNode.attr("height") else null
            parseNode(addToAST(Image(
                srcAttrValue, Length.fromString(widthAttrValue)
            ).apply { this.height = Length.fromString(heightAttrValue) }), confirmedNode)
        }
    }

    open fun detectTable() {
        detectByExpression({ it.nodeName() == "table" }) { confirmedNode ->
            parseNode(addToAST(Table().setBasics(confirmedNode)), confirmedNode)
        }
    }

    open fun detectRowGroup() {
        detectByExpression({ arrayOf("tbody", "thead", "tfoot").contains(it.nodeName()) }) { confirmedNode ->
            val type = when (confirmedNode.nodeName()) {
                "thead" -> TRG.head
                "tfoot" -> TRG.foot
                else -> TRG.body
            }
            parseNode(addToAST(TableRowGroup(type).apply { roles(confirmedNode.nodeName()) }), confirmedNode)
        }
    }

    open fun detectRow() {
        detectByExpression({ it.nodeName() == "tr" }) { confirmedNode ->
            parseNode(addToAST(TableRow()), confirmedNode)
        }
    }

    open fun detectCell() {
        detectByExpression({ arrayOf("td", "th").contains(it.nodeName()) }) { confirmedNode ->
            val tableCell = TableCell().apply {
                setBasics(confirmedNode)
                if (confirmedNode.hasAttr("colspan")) colspan = confirmedNode.attr("colspan").toInt()
                if (confirmedNode.hasAttr("rowspan")) rowspan = confirmedNode.attr("rowspan").toInt()
            }
            parseNode(addToAST(tableCell), confirmedNode)
        }
    }

    open fun detectColGroup() {
        detectByExpression({ it.nodeName() == "colgroup" }) { confirmedNode ->
            parseNode(addToAST(ColGroup()), confirmedNode)
        }
    }

    open fun detectCol() {
        detectByExpression({ it.nodeName() == "col" }) { confirmedNode ->
            val width = run {
                val fromWidth = if (confirmedNode.hasAttr("width")) {
                    val widthAttrValue = confirmedNode.attr("width")
                    """[0-9]""".toRegex().matchEntire(widthAttrValue)?.value?.toFloat()
                } else null
                val fromStyle = if (confirmedNode.hasAttr("style")) {
                    val widthAttrValue = confirmedNode.attr("style")
                    """^.*width: *([0-9]+[.]?[0-9]*)%.*$""".toRegex().matchEntire(widthAttrValue)?.groupValues?.get(1)
                        ?.toFloat()
                } else null
                fromWidth ?: fromStyle ?: 1F
            }
            parseNode(addToAST(Col(Length(width))), confirmedNode)
        }
    }

    open fun detectOl() {
        detectByExpression({ it.nodeName() == "ol" }) { confirmedNode ->
            parseNode(addToAST(OrderedList().setBasics(confirmedNode)), confirmedNode)
        }
    }

    open fun detectUl() {
        detectByExpression({ it.nodeName() == "ul" }) { confirmedNode ->
            parseNode(addToAST(UnorderedList().setBasics(confirmedNode)), confirmedNode)
        }
    }

    open fun detectLi() {
        detectByExpression({ it.nodeName() == "li" }) { confirmedNode ->
            parseNode(addToAST(ListItem().setBasics(confirmedNode)), confirmedNode)
        }
    }

    open fun detectH() {
        val regEx = "h([0-9])".toRegex()
        detectByExpression({ regEx.matches(it.nodeName()) }) { confirmedHtmlNode ->
            val level = regEx.matchEntire(confirmedHtmlNode.nodeName())?.groupValues?.get(1) ?: "0"
            val newHeading = Heading(level = level.toInt()).setBasics(confirmedHtmlNode)
            parseNode(addToAST(newHeading), confirmedHtmlNode)
        }
    }

    open fun detectP() {
        detectByExpression({ it.nodeName() == "p" }) { confirmedNode ->
            parseNode(addToAST(Paragraph().setBasics(confirmedNode)), confirmedNode)
        }
    }

    open fun detectPre() {
        detectByExpression({ it.nodeName() == "pre" }) { confirmedNode ->
            parseNode(addToAST(Paragraph().apply { roles("pre"); sourceTagName = "pre" }), confirmedNode)
        }
    }

    open fun detectCode() {
        detectByExpression({ it.nodeName() == "code" }) { confirmedNode ->
            parseNode(addToAST(Span().apply { roles("code") }), confirmedNode)
        }
    }

    open fun detectSpan() {
        val spanTypes = "strong|b|em|i|span|br|sup|sub|mark".split("|")
        detectByExpression({ spanTypes.contains(it.nodeName()) }) { confirmedHtmlNode ->
            val newSpan = Span().apply {
                sourceTagName = confirmedHtmlNode.nodeName()
                roles.add(confirmedHtmlNode.nodeName())
                setBasics(confirmedHtmlNode)
                if (confirmedHtmlNode.nodeName() == "br") +"\n"
            }
            parseNode(addToAST(newSpan), confirmedHtmlNode)
        }
    }

    open fun detectA() {
        detectByExpression({ it.nodeName() == "a" }) { confirmedHtmlNode ->
            val href = confirmedHtmlNode.attr("href")
            val newAnchor = Anchor(href)
            parseNode(addToAST(newAnchor), confirmedHtmlNode)
        }
    }

    open fun detectOpenBlock() {
        detectByExpression({
            (it.nodeName() == "div" && (it.hasAttr("id")) || unknownTagProcessingRule.invoke(it) == UnknownTagProcessing.PROCESS)
        }) { confirmedHtmlNode ->
            val newOpenBlock = OpenBlock().setBasics(confirmedHtmlNode)
            parseNode(addToAST(newOpenBlock), confirmedHtmlNode)
        }
    }


    open fun detectText() {
        detectByExpression({ htmlNode -> htmlNode.isText() }) { confirmedNode ->
            val text = Text(confirmedNode.nodeText() ?: "")
            if (confirmedNode.inPre() || text.text.isNotEmpty()) {
                val newTextNode = Text(text.text)
                addToAST(newTextNode)
            }
        }
    }
}
