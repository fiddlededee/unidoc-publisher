package experiments

import common.AsciidoctorAdapter
import converter.FodtConverter
import model.SourceMapping
import org.approvaltests.Approvals
import org.asciidoctor.Asciidoctor
import org.asciidoctor.Attributes
import org.asciidoctor.Options
import org.asciidoctor.SafeMode
import org.asciidoctor.extension.RubyExtensionRegistry
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

class SourceMappingTest {
    @Test
    fun sourceMapping() {
        val rubyExtension = """
            require 'asciidoctor'
            require 'asciidoctor/extensions'
            
            class ShellSessionTreeProcessor < Asciidoctor::Extensions::TreeProcessor
              LF = ?\n

              def process document
                document.find_by().each do |block|
                    attrs = block.attributes
                    destRole = attrs['role'] || ''
                    file_class = 'sm-file--' + (Base64.strict_encode64 block.file).gsub("+", "--plus").gsub("/", "--slash").gsub("=", "--equal") + '--sm-file'
                    line_no = "sm-lineno--#{block.lineno}--sm-lineno"
                    destRole = attrs['role'] || ''
                    destRole  = "#{destRole} sm-source-mapping #{file_class} #{line_no}"
                    destRole = destRole.strip
                    attrs['role'] = destRole
                end
                nil
              end
            end
        """.trimIndent()

        val adocPath = "src/test/kotlin/experiments/source-mapping.adoc"
        val adocFile = File(adocPath)
        val factory: Asciidoctor = Asciidoctor.Factory.create()
        factory.javaConverterRegistry().register(TextConverter::class.java)
        val rubyExtensionRegistry: RubyExtensionRegistry = factory.rubyExtensionRegistry()
        rubyExtensionRegistry.loadClass(rubyExtension.byteInputStream(StandardCharsets.UTF_8))
            .treeprocessor("ShellSessionTreeProcessor")
        val attributes = Attributes.builder().build()
        val resultHtml = factory.convertFile(
            adocFile, Options.builder()
                .backend("html5")
                .attributes(attributes)
                .sourcemap(true)
                .toFile(false)
                .safe(SafeMode.UNSAFE)
                .build()
        )
        FodtConverter {
            html = resultHtml
            adaptWith(AsciidoctorAdapter)
            parse()
            ast().descendant { it.roles.any { role -> role.contains("source-mapping") } }
                .forEach { node ->
                    val reLineno = """^sm-lineno--(.*)--sm-lineno$""".toRegex()
                    val reFile = """^sm-file--(.*)--sm-file$""".toRegex()
                    val sourceLineno = node.roles
                        .firstNotNullOfOrNull { reLineno.matchEntire(it)?.groupValues?.get(1) }?.toIntOrNull()
                    if (sourceLineno != null) {
                        node.sourceMapping = SourceMapping(
                            sourceLineno,
                            node.roles
                                .firstNotNullOfOrNull {
                                    reFile.matchEntire(it)?.groupValues?.get(1)
                                        ?.run {
                                            arrayOf("equal" to "=", "slash" to "/", "plus" to "+")
                                                .fold(this) { a, it -> a.replace("--${it.first}", it.second) }
                                        }?.run { String(Base64.getDecoder().decode(this)) }
                                        ?.run {
                                            if (this.startsWith(adocFile.parentFile.absolutePath))
                                                this.substring(adocFile.parentFile.absolutePath.length + 1) else this
                                        }
                                }
                        )

                    }
                    val newRoles = node.roles.filter { !it.startsWith("sm-") }
                    node.roles.apply { clear(); addAll(newRoles) }
                }

        }.apply { Approvals.verify(ast().toYamlString()) }
    }
}