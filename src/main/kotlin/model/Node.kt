package model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator

fun document(init: Document.() -> Unit = {}): Document {
    return Document().apply(init)
}

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
abstract class Node() {

    var id: String? = null
    var sourceTagName: String? = null
    var includeTags: MutableSet<String> = mutableSetOf()
    var sourceAttributes: MutableMap<String, String> = mutableMapOf()
    var sourceMapping: SourceMapping? = null

    @get:JsonIgnore
    abstract val isInline: Boolean

    @JsonIgnore
    private var parent: Node? = null

    private val children: ArrayList<Node> = arrayListOf()
    val roles = arrayListOf<String>()
    fun <T : Node> appendChild(childNode: T): T {
        val oldParent = childNode.parent
        children.add(childNode)
        childNode.parent = this
        oldParent?.removeChild(childNode)
        return childNode
    }

    fun <T : Node> appendFirstChild(childNode: T): T {
        if (children().size == 0)
            appendChild(childNode) else
            children()[0].insertBefore(childNode)
        return childNode
    }

    fun table(init: Table.() -> Unit = {}): Table {
        return appendChild(Table().apply(init))
    }

    fun img(src: String, init: Image.() -> Unit = {}): Image {
        return appendChild(Image(src).apply(init))
    }

    fun tableRowGroup(type: TRG, init: TableRowGroup.() -> Unit = {}): TableRowGroup {
        return appendChild(TableRowGroup(type).apply(init))
    }

    fun col(width: Length): Col {
        return appendChild(Col(width))
    }

    fun tr(init: TableRow.() -> Unit = {}): TableRow {
        return appendChild(TableRow().apply(init))
    }

    fun td(init: TableCell.() -> Unit = {}): TableCell {
        return appendChild(TableCell().apply(init))
    }

    fun h(level: Int, init: Heading.() -> Unit = {}): Heading {
        return appendChild(Heading(level).apply(init))
    }


    fun li(init: ListItem.() -> Unit = {}): ListItem {
        return appendChild(ListItem().apply(init))
    }

    fun ol(init: OrderedList.() -> Unit = {}): OrderedList {
        return appendChild(OrderedList().apply(init))
    }

    fun ul(init: UnorderedList.() -> Unit = {}): UnorderedList {
        return appendChild(UnorderedList().apply(init))
    }

    fun p(init: Paragraph.() -> Unit = {}): Paragraph {
        return appendChild(Paragraph().apply(init))
    }

    fun span(init: Span.() -> Unit = {}): Span {
        return appendChild(Span().apply(init))
    }

    fun a(href: String, init: Anchor.() -> Unit = {}): Anchor {
        return appendChild(Anchor(href).apply(init))
    }

    fun text(text: String): Text {
        return appendChild(Text(text))
    }

    fun openBlock(init: OpenBlock.() -> Unit = {}): OpenBlock {
        return appendChild(OpenBlock().apply(init))
    }

    operator fun String.unaryPlus(): Text {
        return this@Node.appendChild(Text(this))
    }

    private fun removeChild(childNode: Node) {
        children.remove(childNode)
    }

    fun remove() {
        val parentNode = parent
        if (parentNode != null) {
            parentNode.removeChild(this)
        } else throw Exception("Node doesn't hava a parent")
    }

    fun insertAfter(nodeToInsert: Node): Node {
        val oldParent = nodeToInsert.parent
        val parent = this.parent
            ?: throw Exception("Can't add node after another if it has no parent")
        val index = parent.children.indexOf(this)
        if (index == -1) {
            throw Exception("Didn't find object to add after")
        }
        parent.children.add(index + 1, nodeToInsert)
        nodeToInsert.parent = parent
        oldParent?.removeChild(nodeToInsert)
        return nodeToInsert
    }

    fun insertBefore(nodeToInsert: Node): Node {
        val oldParent = nodeToInsert.parent
        val parent = this.parent
            ?: throw Exception("Can't add node before another if it has no parent")
        val index = parent.children.indexOf(this)
        if (index == -1) {
            throw Exception("Didn't find object to add after")
        }
        parent.children.add(index, nodeToInsert)
        nodeToInsert.parent = parent
        oldParent?.removeChild(nodeToInsert)
        return nodeToInsert
    }


    // todo abstract ?
    abstract fun write(bw: BackendWriter)

    private fun ArrayList<Node>.fillWithDescendants(node: Node, filter: (Node) -> Boolean) {
        node.children.forEach {
            if (filter(it)) {
                add(it)
            }
            fillWithDescendants(it, filter)
        }
    }

    fun descendant(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        return arrayListOf<Node>().apply { fillWithDescendants(this@Node, filter) }
    }

    fun ancestor(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        val ancestors = arrayListOf<Node>()
        var parent = this@Node.parent()
        while (parent != null) {
            if (filter.invoke(parent)) ancestors.add(parent)
            parent = parent.parent()
        }
        return ancestors
    }

    fun nextSibling(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        val nextSibling = arrayListOf<Node>()
        val parent = this.parent() ?: return nextSibling
        if (parent.children.indexOf(this) == parent.children.size - 1) return nextSibling
        IntRange(parent.children.indexOf(this) + 1, parent.children.size - 1).forEach {
            if (filter(parent.children[it])) nextSibling.add(parent.children[it])
        }
        return nextSibling
    }

    fun previousSibling(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        val previousSibling = arrayListOf<Node>()
        val parent = this.parent() ?: return previousSibling
        if (parent.children.indexOf(this) == 0) return previousSibling
        IntRange(0, parent.children.indexOf(this) - 1).forEach {
            if (filter(parent.children[it])) previousSibling.add(parent.children[it])
        }
        return previousSibling
    }
    fun hasNext(): Boolean {
        val parent = this.parent() ?: throw Exception("The ${this::class.java.simpleName} has no parent")
        return (parent.children.indexOf(this) != parent.children.size - 1)
    }

    fun next(): Node {
        val parent = this.parent() ?: throw Exception("The ${this::class.java.simpleName} has no parent")
        if (parent.children.indexOf(this) == parent.children.size - 1)
            throw Exception("The ${this::class.java.simpleName} is last")
        return parent.children[parent.children.indexOf(this) + 1]
    }

    fun hasPrevious(): Boolean {
        val parent = this.parent() ?: throw Exception("The ${this::class.java.simpleName} has no parent")
        return (parent.children.indexOf(this) != 0)
    }

    fun previous(): Node {
        val parent = this.parent() ?: throw Exception("The ${this::class.java.simpleName} has no parent")
        if (parent.children.indexOf(this) == 0)
            throw Exception("The ${this::class.java.simpleName} is first")
        return parent.children[parent.children.indexOf(this) - 1]
    }

    fun index(): Int {
        val parent = this.parent()
            ?: throw Exception("The ${this::class.java.simpleName} node has no parent")
        return parent.children.indexOf(this)
    }

    @JsonProperty("children")
    fun getChildren(): ArrayList<Node> {
        return children
    }

    fun children(filter: (Node) -> Boolean = { true }): ArrayList<Node> {
        return arrayListOf<Node>()
            .apply { children.forEach { if (filter(it)) this.add(it) } }
    }

    fun parent(): Node? {
        return parent
    }

    fun roles(vararg role: String) {
        role.forEach { roles.add(it) }
    }

    fun toYamlString(): String {
        return ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
            .writeValueAsString(this)
    }

    fun <T : Node> T.wrapNodeInlineContents(): T {
        // Wrapping each inline nodes chains into paragraph node
        fun wrap(itemsToWrap: ArrayList<ArrayList<Node>>) {
            itemsToWrap.forEach { itemsChain ->
                if (itemsChain.isEmpty()) return@forEach
                itemsChain[0].insertBefore(Paragraph())
                    .apply { itemsChain.forEach { itemToWrap -> appendChild(itemToWrap) } }
            }
        }

        // Finding inline nodes chains
        val itemsToWrap = arrayListOf<ArrayList<Node>>()
        itemsToWrap.add(arrayListOf())
        this.children.forEach { nodeChild ->
            if (nodeChild.isInline) itemsToWrap.last().add(nodeChild)
            else if (itemsToWrap.last().isNotEmpty()) itemsToWrap.add(arrayListOf())
        }
        wrap(itemsToWrap)
        return this
    }


    // Don't understand html rendering rules, hope I'm right about this
    fun normalizeWhitespaces() {
        val localAst = this
        // Cleaning whitespaces after and before block nodes
        localAst.descendant { it is Text && it.text.isBlank() }.forEach { blankTextNode ->
            val localParent = blankTextNode.parent() ?: return@forEach
            val previousConsideredBlock =
                (!blankTextNode.hasPrevious() && !localParent.isInline) ||
                        (blankTextNode.hasPrevious() && (
                                !blankTextNode.previous().isInline ||
                                        blankTextNode.previous().sourceTagName == "br"
                                ))
            val nextConsideredBlock =
                (!blankTextNode.hasNext() && !localParent.isInline) ||
                        (blankTextNode.hasNext() && (
                                !blankTextNode.next().isInline ||
                                        blankTextNode.next().sourceTagName == "br"
                                ))

            if (previousConsideredBlock || nextConsideredBlock)
                blankTextNode.remove()
        }
        // Removing extra whitespaces
        Document().apply { appendChild(localAst) }.descendant { !it.isInline }.forEach { block ->
            if (block.sourceTagName == "pre" || block.ancestor { it.sourceTagName == "pre" }.isNotEmpty())
                return@forEach
            val nodesToNormalise = block.descendant { node ->
                (node is Image || node is Text || !node.isInline) &&
                        node.ancestor { !it.isInline }.firstOrNull() == block
            }
            var chain: ArrayList<Node> = arrayListOf()
            nodesToNormalise.forEach searchForChainsToNormalize@{ node ->
                if (!node.isInline) {
                    if (chain.isEmpty()) return@searchForChainsToNormalize else {
                        normalizeChain(chain)
                        chain = arrayListOf()
                    }
                } else chain.add(node)
            }
            normalizeChain(chain)
        }
    }

    private fun normalizeChain(chain: ArrayList<Node>) {
        if (chain.isEmpty()) return
        var trimStart = true
        chain.forEach byNodesToNormalize@{ node ->
            if (node is Image || !node.isInline) trimStart = false
            if (node !is Text) return@byNodesToNormalize
            if (trimStart) {
                node.text = node.text.trimStart()
            } else {
                if (node.text.trimStart() != node.text) node.text = " " + node.text.trimStart()
            }
            if (node.text.trimEnd() != node.text && node.text.isNotEmpty()) {
                node.text = node.text.trimEnd() + " "
                trimStart = true
            } else if (node.text.isNotEmpty()) trimStart = false
        }
        val iterator = chain.reversed().iterator()
        while (iterator.hasNext()) {
            val el = iterator.next()
            if (el !is Text) break
            if (el.text.isBlank()) el.remove() else {
                el.text = el.text.trimEnd()
                break
            }
        }

    }

    fun extractText(replaceRule: (Text) -> String = { it.text }): String {
        if (this is Text) return replaceRule.invoke(this)
        return descendant { it is Text }
            .map { it as Text }
            .joinToString("") { text -> replaceRule.invoke(text) }
    }

}

