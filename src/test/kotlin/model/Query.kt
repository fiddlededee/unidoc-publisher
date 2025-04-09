package model

import shouldBe
import nodeRolesSequence
import nodeSequence
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class Query {

    @Test
    fun ancestor() {
        lateinit var node: Node
        Document().apply { table { tr { td { node = p { +"Some text" } } } } }
        node.ancestor().nodeSequence() shouldBe "TableCell -> TableRow -> Table -> Document"
    }

    @Test
    fun nextSibling() {
        Document().apply { arrayOf("a", "b", "c", "d").forEach { p { roles(it) } } }
            .descendant { it.roles.contains("b") }[0]
            .nextSibling().nodeRolesSequence() shouldBe "c -> d"
    }

    @Test
    fun previuosSibling() {
        Document().apply { arrayOf("a", "b", "c", "d").forEach { p { roles(it) } } }
            .descendant { it.roles.contains("c") }[0]
            .previousSibling().nodeRolesSequence() shouldBe "a -> b"
    }

    @Test
    fun index() {
        lateinit var node: Node
        Document().apply { p { roles("a") }; node = p { roles("b") }; p { roles("c") }; p { roles("d") } }
        node.index() shouldBe 1
    }


    val abcd = Document().apply { arrayOf("a", "b", "c", "d").forEach { p { roles(it) } } }

    @Test
    fun next() {
        abcd.descendant { it.roles.contains("b") }[0].hasNext() shouldBe true
        abcd.descendant { it.roles.contains("d") }[0].hasNext() shouldBe false
    }

    @Test
    fun tryNextForLast() {
        abcd.descendant { it.roles.contains("c") }[0].next().roles[0] shouldBe "d"
        assertThrows(Exception::class.java) { abcd.descendant { it.roles.contains("d") }[0].next() }
    }
}

