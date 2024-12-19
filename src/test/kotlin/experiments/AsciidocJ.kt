package experiments

import model.Text
import org.approvaltests.Approvals
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Attributes
import org.asciidoctor.Options
import org.asciidoctor.ast.ContentNode
import org.asciidoctor.ast.Document
import org.asciidoctor.ast.Section
import org.asciidoctor.ast.StructuralNode
import org.asciidoctor.converter.ConverterFor
import org.asciidoctor.converter.StringConverter
import org.asciidoctor.jruby.ast.impl.PhraseNodeImpl
import org.asciidoctor.log.LogRecord
import org.asciidoctor.log.Severity
import org.junit.jupiter.api.Test
import java.util.UUID


class AsciidocJ {

    @Test
    fun useConverterDirectly() {
        val asciidocText = """
            == Section *One*
            
            Some *text*!
            
        """.trimIndent()

        val factory: Asciidoctor = Asciidoctor.Factory.create()
        factory.javaConverterRegistry().register(TextConverter::class.java)
        val modelDocument = model.Document()
        val attributes = Attributes.builder()
            .attribute("unidoc-publisher-model", modelDocument)
            .build()
        factory.convert(
            asciidocText, Options.builder()
                .backend("text")
                .attributes(attributes)
                .build()
        )
        // Placing inline nodes to appropriate places
        modelDocument.descendant { it.id?.startsWith("inline-nodes-") ?: false }
            .forEach { inlineNode ->
                val inlineId = inlineNode.id!!.substringAfter("inline-nodes-")
                modelDocument.descendant { it is Text && it.text.contains(inlineId) }
                    .map { it as Text }
                    .firstOrNull()
                    ?.apply {
                        val beforeAfter = text.split("@@${inlineId}@@")
                        arrayOf(Text(beforeAfter[0]), inlineNode, Text(beforeAfter[1]))
                            .filter { it !is Text || it.text.isNotBlank() }
                            .forEach { insertBefore(it) }
                        inlineNode.id = null
                        remove()
                    }
            }
        Approvals.verify(modelDocument.toYamlString())
    }
}


@ConverterFor("text")
class TextConverter(backend: String?, opts: Map<String?, Any?>?) :
    StringConverter(backend, opts) {
    private var modelSet = false
    private lateinit var unidocModel: model.Document

    override fun convert(
        node: ContentNode, transform: String?, o: Map<Any, Any>
    ): String? {
        if (!modelSet) {
            unidocModel = node.document.attributes["unidoc-publisher-model"] as model.Document
            modelSet = true
        }
        var localTransform: String? = transform
        if (localTransform == null) {
            localTransform = node.nodeName
        }

        if (node is Document) {
            node.content
            return "dummy"
        } else if (node is Section) {
            unidocModel.apply {
                h(node.level) {
                    +node.title
                }
                node.content
            }
            return "dummy"
        } else if (localTransform == "paragraph") {
            val block = node as StructuralNode
            val content = block.content as String
            unidocModel.apply { p { +content } }
            return "dummy"
        } else if (node is PhraseNodeImpl) {
            val uid = UUID.randomUUID()
            unidocModel.apply {
                if (node.type == "strong")
                    span { roles("strong"); id = "inline-nodes-$uid"; +node.text }
            }
            return ("@@${uid}@@")
        } else {
            log(
                LogRecord(
                    Severity.WARN,
                    "Unexpected node ${node::class.java.simpleName}, transform (${transform})"
                )
            )
        }
        return null
    }
}
