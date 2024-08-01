package model

import java.util.UUID

class DummyNode() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

abstract class NoWriterNode() : Node() {
    override fun write(bw: BackendWriter) {
        throw Exception("Writer in NoWriterNodes should be defined in CustomStyle")
    }
}

class Document() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }

}

class OpenBlock() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class RectDimensions(val width: Length, val height: Length)

enum class AnchorType { ASCHAR }
class Image(
    var src: String,
    var width: Length? = null,
    var height: Length? = null,
    var anchorType: AnchorType = AnchorType.ASCHAR,
) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Col(var width: Length) : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}


class ColGroup : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

enum class LengthUnit { cm, mm, inch, px, pt, pc, em, ex, ch, rem, vw, vh, vmin, vmax, perc, parrots, mmm }

class Length(var value: Float, var unit: LengthUnit = LengthUnit.perc) {
    companion object Factory {
        fun fromString(length: String?): Length? {
            if (length == null) return null
            return LengthUnit.entries
                .map {
                    val inValueUnit = when (it) {
                        LengthUnit.inch -> "in"; LengthUnit.perc -> "%"
                        LengthUnit.parrots -> ""; else -> it.toString()
                    }
                    """^([0-9]+[.]?[0-9]*)(${inValueUnit})$""".toRegex()
                        .matchEntire(length) to it
                }.filter { it.first != null }
                .getOrNull(0)
                ?.run { Length(first!!.groupValues[1].toFloat(), second) }
        }
    }
}

class Table() : Node() {
    // TODO: implement cols property
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

enum class TRG { head, body, foot }

class TableRowGroup(val type: TRG) : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class TableRow() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class TableCell(var colspan: Int = 1, var rowspan: Int = 1) : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }

//    fun wrapNodeInlineContents(): TableCell {
//        // Wrapping each inline nodes chains into paragraph node
//        fun wrap(itemsToWrap: ArrayList<ArrayList<Node>>) {
//            itemsToWrap.forEach { itemsChain ->
//                if (itemsChain.isEmpty()) return@forEach
//                itemsChain[0].addBefore(Paragraph())
//                    .apply { itemsChain.forEach { itemToWrap -> addChild(itemToWrap) } }
//            }
//        }
//
//        // Finding inline nodes chains
//        val itemsToWrap = arrayListOf<ArrayList<Node>>()
//        itemsToWrap.add(arrayListOf())
//        this.children.forEach { nodeChild ->
//            if (nodeChild.isInline) itemsToWrap.last().add(nodeChild)
//            else if (itemsToWrap.last().isNotEmpty()) itemsToWrap.add(arrayListOf())
//        }
//        wrap(itemsToWrap)
//        return this
//    }
}

class OrderedList() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class UnorderedList() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class ListItem() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }

    fun wrapListItemContents(): ListItem {
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
        this.children().forEach { listItemChild ->
            if (listItemChild.isInline) itemsToWrap.last().add(listItemChild)
            else if (itemsToWrap.last().isNotEmpty()) itemsToWrap.add(arrayListOf())
        }
        wrap(itemsToWrap)
        return this
    }
}

class Header(var level: Int) : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Paragraph() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

// Look like excessive
//class Pre() : Node() {
//    override val isInline: Boolean get() = false
//    override fun write(bw: BackendWriter) {
//        bw.write(this)
//    }
//}
//
//class Code() : Node() {
//    override val isInline: Boolean get() = false
//    override fun write(bw: BackendWriter) {
//        bw.write(this)
//    }
//}


class Span() : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Anchor(var href: String) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Text(var text: String) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Toc(var levels: Int = 3, title: String = "Table of contents 111") : Node() {
    var titleNode = Paragraph().apply { +title }
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class TextFrame() : Node() {
    override val isInline: Boolean get() = false
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}

class Footnote(
    val footnoteId: String = UUID.randomUUID().toString()
) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
    init {
        id = footnoteId
    }
}

class FootnoteRef(val footnote: Footnote) : Node() {
    override val isInline: Boolean get() = true
    override fun write(bw: BackendWriter) {
        bw.write(this)
    }
}
