@file:DependsOn("ru.fiddlededee:unidoc-publisher:0.7.8")
@file:DependsOn("org.asciidoctor:asciidoctorj:2.5.11")
@file:DependsOn("com.helger:ph-css:7.0.1")
@file:DependsOn("com.google.guava:guava:21.0")
@file:DependsOn("org.languagetool:language-en:5.6")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:4.2.2")

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.helger.css.ECSSVersion
import com.helger.css.reader.CSSReader
import common.AsciidoctorAdapter
import converter.FodtConverter
import model.*
import org.asciidoctor.*
import org.languagetool.JLanguageTool
import org.languagetool.language.AmericanEnglish
import org.languagetool.rules.spelling.SpellingCheckRule
import writer.OdtStyle
import writer.OdtStyleList
import writer.paragraphProperties
import writer.textProperties
import java.io.File
import java.nio.charset.StandardCharsets

CliOptions.apply { main(args) }

// tag::body[]
FodtConverter {
    adaptWith(AsciidoctorAdapter) // <1>
    template = File(CliOptions.template).readText() // <2>
    html = AsciidocHtmlFactory
        .getHtmlFromFile(File(CliOptions.adocFile)) // <3>
    odtStyleList.add(rougeStyles()) // <4>
    odtStyleList.add(indentPreamble()) // <5>
    parse() // <6>
    if (CliOptions.logo != null) addLogoToAst() // <7>
    ast2fodt() // <8>
    if (CliOptions.checkSpelling) checkSpelling() // <9>
    CliOptions.htmlOutput
        ?.let { File(it).writeText(html()) } // <10>
    CliOptions.yamlOutput
        ?.let { File(it).writeText(ast().toYamlString()) } // <11>
    File(CliOptions.fodtOutput).writeText(fodt()) // <12>
}
// end::body[]

object CliOptions : NoOpCliktCommand() {
    val adocFile by option(help = "File to process (required)").required()
    val template by option(help = "Template path (required)").required()
    val fodtOutput by option(help = "Fodt output file (required)").required()
    val yamlOutput by option(help = "Yaml output file")
    val htmlOutput by option(help = "Html output file")
    val checkSpelling by option(help = "Check spelling").flag()
    val logo by option(help = "Path to logo")
}

object AsciidocHtmlFactory {
    private val factory: Asciidoctor = Asciidoctor.Factory.create()
    fun getHtmlFromFile(file: File): String = factory.convertFile(
        file,
        Options.builder().backend("html5").sourcemap(true).safe(SafeMode.UNSAFE)
            .toFile(false).standalone(true)
            .attributes(Attributes.builder().attribute("data-uri").build())
    )
}

fun FodtConverter.checkSpelling() {
    ast().descendant { paragraph ->
        paragraph is Paragraph &&
                paragraph.ancestor { it.roles.contains("listingblock") }.isEmpty()
    }.forEach { paragraph ->
        paragraph.extractText { text ->
            if (text.ancestor { it.roles.contains("code") }.isNotEmpty()
                || text.text.contains("/")
            ) "Dummy" else text.text
        }.langToolsCheck()
    }
}

fun FodtConverter.addLogoToAst() {
    val logo = CliOptions.logo ?: return
    ast().appendFirstChild(Paragraph().apply {
        roles("header-image")
        img(logo).apply { width = Length(25F, LengthUnit.mm) }
    })
    val imageStyleList = OdtStyleList(OdtStyle { paragraph ->
        if (paragraph !is Paragraph) return@OdtStyle
        if (!paragraph.roles.contains("header-image")) return@OdtStyle
        paragraphProperties { attributes("fo:text-align" to "center") }
    })
    odtStyleList.add(imageStyleList)
}

fun indentPreamble(): OdtStyleList {
    return OdtStyleList(
        OdtStyle { preamble ->
            if (preamble !is Paragraph ||
                preamble.ancestor { it.id == "preamble" }.isEmpty()
            ) return@OdtStyle
            paragraphProperties { attributes("fo:margin-top" to "12pt") }
        }
    )
}

fun rougeStyles(): OdtStyleList {
    val css = CSSReader.readFromFile(
        File("approved/asciidoc/syntax.css"), StandardCharsets.UTF_8, ECSSVersion.CSS30
    )

    val rougeStyles = css?.allStyleRules?.flatMap { styleRule ->
        styleRule.allSelectors.flatMap { selector ->
            styleRule.allDeclarations.map { declaration ->
                selector.allMembers.map { it.asCSSString } to (declaration.property to declaration.expressionAsCSSString)
            }
        }
    }?.filter {
        it.first.size == 3 && it.first[0] == ".highlight" && it.first[2][0] == "."[0] && it.first[2].length <= 3 && arrayOf(
            "color",
            "background-color",
            "font-weight",
            "font-style"
        ).contains(it.second.first)
    }?.map { it.first[2].substring(1) to it.second }?.groupBy { it.first }
        ?.map { it.key to it.value.associate { pair -> pair.second.first to pair.second.second } }?.toMap() ?: mapOf()

    return OdtStyleList(OdtStyle { span ->
        val condition = (span is Span) && (span.ancestor { it is Paragraph && it.sourceTagName == "pre" }.isNotEmpty())
        if (!condition) return@OdtStyle
        rougeStyles.filter { span.roles.contains(it.key) }.forEach { style ->
            textProperties {
                arrayOf("color", "background-color", "font-weight", "font-style").forEach {
                    style.value[it]?.let { value ->
                        attribute("fo:$it", value)
                    }
                }
            }
        }
    })

}

object LangTools {
    private val langTool = JLanguageTool(AmericanEnglish())
    var ruleTokenExceptions: Map<String, Set<String>> = mapOf()
    var ruleExceptions: Set<String> = setOf("")

    fun setSpellTokens(
        ignore: Array<String>, accept: Array<String> = arrayOf()
    ): LangTools {
        langTool.allActiveRules.forEach { rule ->
            if (rule is SpellingCheckRule) {
                rule.addIgnoreTokens(ignore.toList())
                rule.acceptPhrases(accept.toList())
            }
        }
        return this
    }

    fun check(text: String) {
        val errs = langTool.check(text).filterNot {
            (this.ruleTokenExceptions[it.rule.id]?.contains(text.substring(it.fromPos, it.toPos))
                ?: false) or ((this.ruleExceptions).contains(it.rule.id))
        }

        if (errs.isNotEmpty()) {
            var errorMessage = "Spell failed for:\n$text\n"
            errs.forEachIndexed { index, it ->
                errorMessage += "[${index + 1}] ${it.message}, ${it.rule.id} (${it.fromPos}:${it.toPos} " + "- ${
                    text.substring(
                        it.fromPos,
                        it.toPos
                    )
                })\n"
            }
            println(errorMessage.split("\n").map { it.chunked(120) }.flatten().joinToString("\n"))
        }
    }
}

fun String.langToolsCheck() {
    var textToCheck = this
    arrayOf(
        "Kotlin script" to "Script"
    ).forEach {
        textToCheck = textToCheck.replace(it.first, it.second)
    }
    LangTools.apply {
        setSpellTokens(
            accept = arrayOf(
                "UniDoc",
                "gradle",
                "Asciidoc",
                "Asciidoctor",
                "FODT",
                "ODT",
                "PDF",
                "HTML",
                "DOCX",
                "AST",
                "JOD",
                "Nikolaj",
                "Potashnikov",
                "templating",
                "DocOps",
                "DokuWiki",
                "runnable",
                "Writerside",
                "Hedley"
            ), ignore = arrayOf(
            )
        )
        ruleExceptions = setOf(
            "UPPERCASE_SENTENCE_START",
        )
    }.check(textToCheck)
}

"Finished"