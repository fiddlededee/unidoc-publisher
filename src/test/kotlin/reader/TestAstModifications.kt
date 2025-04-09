package reader

import model.Document
import model.Paragraph
import nodeRolesSequence
import org.junit.jupiter.api.Test
import shouldBe


class TestAstModifications {
    @Test
    fun appendFirstChild() {
        Document().apply {
            p { roles("a") }
            appendFirstChild(Paragraph().apply { roles("b") })
            children().nodeRolesSequence() shouldBe "b -> a"
        }
    }

    @Test
    fun appendFirstChildIntoEmptyNode() {
        val document = Document()
        document.children().size shouldBe 0
        document.appendFirstChild(Paragraph())
        document.children().size shouldBe 1
    }


}