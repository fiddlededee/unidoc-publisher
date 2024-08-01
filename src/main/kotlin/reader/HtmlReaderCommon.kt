package reader

import model.Node
import kotlin.reflect.KFunction0

open class HtmlReaderCommon(private val node: Node, private val htmlNode: HtmlNode) {

    fun node(): Node {
        return node
    }

    private var cursorNode: HtmlNode? = htmlNode.firstChild()

    fun addToAST(newNode: Node): Node {
        node().appendChild(newNode)
        return newNode
    }

    protected fun tryStep(kFunction0: () -> Unit) {
        if (cursorNode != null) {
            kFunction0.invoke()
        }
    }

    // iterating detection functions
    protected fun detectBy(vararg kFunctions: KFunction0<Unit>) {
        while (cursorNode != null) {
            val stepOldCursorNode = cursorNode as HtmlNode
            run step@{
                kFunctions.forEach {
                    val oldCursorNode = cursorNode
                    it.invoke()
                    if (oldCursorNode != cursorNode) {
                        return@step
                    }
                }
            }
            if (stepOldCursorNode == cursorNode) {
                println("WARNING: ${cursorNode?.nodeName()} (${cursorNode?.classNames()}) not detected")
                cursorNode = cursorNode?.nextSibling()
            }

        }
    }

    fun detectByExpression(
        expression: (htmlNode: HtmlNode) -> Boolean,
        initNode: (confirmedHtmlNode: HtmlNode) -> Unit
    ) {
        if (cursorNode == null) {
            return
        }
        val oldCursorNode = cursorNode as HtmlNode
        val (confirmedNode, nextNode) =
            oldCursorNode.matchByExpression(expression)
        if (confirmedNode != null) {
            if (confirmedNode.nodeNameOrText() != "") {
                initNode.invoke(confirmedNode)
            }
            cursorNode = nextNode
        }
    }
}
