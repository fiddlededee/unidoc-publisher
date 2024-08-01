package reader

import common.AsciidoctorAdapter
import common.asciidocAsHtml
import common.prettySerialize
import converter.FodtConverter
import fodt.parseStringAsXML
import model.Document
import model.OpenBlock
import model.Paragraph
import nodeRolesSequence
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import shouldBe
import verify


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