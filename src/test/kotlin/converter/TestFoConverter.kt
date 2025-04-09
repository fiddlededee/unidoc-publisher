package converter

import com.helger.css.ECSSVersion
import com.helger.css.reader.CSSReader
import common.AsciidocHtmlFactory
import converter.fo.AsciidoctorFoAdapter
import converter.fo.FoConverter
import fodt.parseStringAsXML
import model.*
import org.apache.fop.apps.FOUserAgent
import org.apache.fop.apps.Fop
import org.apache.fop.apps.FopFactory
import org.apache.fop.apps.MimeConstants
import org.apache.fop.configuration.Configuration
import org.apache.fop.configuration.DefaultConfigurationBuilder
import org.junit.jupiter.api.Test
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.xml
import org.w3c.dom.Document
import verify
import writer.*
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXResult


class TestFoConverter {


    @Test
    fun redundentText() {
        // if fails simplify Text writer in FoWriter
        println(
            arrayOf("\r" to """\r""", "\n" to """\n""").joinToString("\n") {
                xml("root")
                    .apply { "a" {}; -it.first; "a" {} }
                    .toString(PrintOptions(pretty = false))
                    .replace(it.first, it.second)
            }.verify()
        )
    }

    @Test
    fun redundentNbsp() {
        // if fails simplify Text writer in FoWriter
        FodtConverter {
            ast = Document().apply {
                span { +" " }
            }
            generatePre()
            println(preList().first().pre)

        }
    }

    private fun fopTransform(doc: Document, fop: Fop) {
        val factory = TransformerFactory.newInstance()
        val transformer: Transformer = factory.newTransformer()
        val src: Source = DOMSource(doc)
        val res: Result = SAXResult(fop.getDefaultHandler())
        transformer.transform(src, res)
    }

    fun rougeStyles(): FoStyleList {
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
            ?.map { it.key to it.value.associate { pair -> pair.second.first to pair.second.second } }?.toMap()
            ?: mapOf()

        return FoStyleList(FoStyle { span ->
            val condition =
                (span is Span) && (span.ancestor { it is Paragraph && it.sourceTagName == "pre" }.isNotEmpty())
            if (!condition) return@FoStyle
            rougeStyles.filter { span.roles.contains(it.key) }.forEach { style ->
                arrayOf("color", "background-color", "font-weight", "font-style").forEach {
                    style.value[it]?.let { value ->
                        attribute(it, value)
                    }
                }
            }
        })

    }

    @Test
    fun buildDoc() {
        FoConverter {
            adaptWith(AsciidoctorFoAdapter)
            foStyleList.add(rougeStyles())
            html = AsciidocHtmlFactory
                .getHtmlFromFile(File("doc/pages/unidoc-publisher-doc.adoc"))
            parse()
            ast().descendant { it.id == "footer" }.forEach { it.remove() }
            ast().appendFirstChild(Paragraph().apply {
                roles("header-image")
                img("${Paths.get("").toAbsolutePath()}/doc/images/unidoc-processor-symbol.svg")
                    .apply { width = Length(25F, LengthUnit.mm) }
            })
            val imageStyleList = FoStyleList(FoStyle { paragraph ->
                if (paragraph !is Paragraph) return@FoStyle
                if (!paragraph.roles.contains("header-image")) return@FoStyle
                attributes("text-align" to "center", "space-after" to "4mm")
            })
            foStyleList.add(imageStyleList)
            File("temp/ast.yaml").writeText(ast().toYamlString())
            generatePre()
            File("temp/fo.xml").writeText(preList().first().pre)
            template = File("src/test/data/simple-xsl-fo.xml").readText()
            generateFo()
            val out = FileOutputStream("build/unidoc-publisher-doc-fo.pdf")
            val fopFactory = FopFactory.newInstance(File("src/test/data/fop.xconf"))
            val fop = fopFactory.newFop(MimeConstants.MIME_PDF, out)
            fopTransform(fo().parseStringAsXML(), fop)
        }
    }

    @Test
    fun basicE2e() {
        val myImage =
            "data:image/svg;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjwhLS0gQ3JlYXRlZCB3aXRoIElua3NjYXBlIChodHRwOi8vd3d3Lmlua3NjYXBlLm9yZy8pIC0tPgoKPHN2ZwogICB3aWR0aD0iMjguNW1tIgogICBoZWlnaHQ9IjI4bW0iCiAgIHZpZXdCb3g9IjAgMCAyOC41IDI4IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmc1IgogICBpbmtzY2FwZTp2ZXJzaW9uPSIxLjEuMiAoMGEwMGNmNTMzOSwgMjAyMi0wMi0wNCkiCiAgIHNvZGlwb2RpOmRvY25hbWU9InVuaS1kb2MtcHJvY2Vzc29yLXN5bWJvbC5zdmciCiAgIGlua3NjYXBlOmV4cG9ydC1maWxlbmFtZT0iL2hvbWUvbm1wL3dvcmsvYW55LW1hcmt1cC0yLWFueS1iYWNrZW5kL3VuaS1wdWJsaXNoZXItbXZwL2RvYy9pbWFnZXMvdW5pLWRvYy1wcm9jZXNzb3Itc3ltYm9sLnBuZyIKICAgaW5rc2NhcGU6ZXhwb3J0LXhkcGk9IjMwMCIKICAgaW5rc2NhcGU6ZXhwb3J0LXlkcGk9IjMwMCIKICAgeG1sbnM6aW5rc2NhcGU9Imh0dHA6Ly93d3cuaW5rc2NhcGUub3JnL25hbWVzcGFjZXMvaW5rc2NhcGUiCiAgIHhtbG5zOnNvZGlwb2RpPSJodHRwOi8vc29kaXBvZGkuc291cmNlZm9yZ2UubmV0L0RURC9zb2RpcG9kaS0wLmR0ZCIKICAgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIgogICB4bWxuczpzdmc9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8c29kaXBvZGk6bmFtZWR2aWV3CiAgICAgaWQ9Im5hbWVkdmlldzciCiAgICAgcGFnZWNvbG9yPSIjZmZmZmZmIgogICAgIGJvcmRlcmNvbG9yPSIjNjY2NjY2IgogICAgIGJvcmRlcm9wYWNpdHk9IjEuMCIKICAgICBpbmtzY2FwZTpwYWdlc2hhZG93PSIyIgogICAgIGlua3NjYXBlOnBhZ2VvcGFjaXR5PSIwLjAiCiAgICAgaW5rc2NhcGU6cGFnZWNoZWNrZXJib2FyZD0iMCIKICAgICBpbmtzY2FwZTpkb2N1bWVudC11bml0cz0ibW0iCiAgICAgc2hvd2dyaWQ9ImZhbHNlIgogICAgIHNob3dndWlkZXM9InRydWUiCiAgICAgaW5rc2NhcGU6Z3VpZGUtYmJveD0idHJ1ZSIKICAgICBpbmtzY2FwZTp6b29tPSIzLjYyMDM4NjciCiAgICAgaW5rc2NhcGU6Y3g9Ii0xMS4xODY2NSIKICAgICBpbmtzY2FwZTpjeT0iNTQuMjc1OTciCiAgICAgaW5rc2NhcGU6d2luZG93LXdpZHRoPSIxODUwIgogICAgIGlua3NjYXBlOndpbmRvdy1oZWlnaHQ9IjkwMiIKICAgICBpbmtzY2FwZTp3aW5kb3cteD0iMCIKICAgICBpbmtzY2FwZTp3aW5kb3cteT0iMCIKICAgICBpbmtzY2FwZTp3aW5kb3ctbWF4aW1pemVkPSIxIgogICAgIGlua3NjYXBlOmN1cnJlbnQtbGF5ZXI9ImxheWVyMSIgLz4KICA8ZGVmcwogICAgIGlkPSJkZWZzMiIgLz4KICA8ZwogICAgIGlua3NjYXBlOmxhYmVsPSJMYXllciAxIgogICAgIGlua3NjYXBlOmdyb3VwbW9kZT0ibGF5ZXIiCiAgICAgaWQ9ImxheWVyMSIKICAgICBzdHlsZT0iZGlzcGxheTppbmxpbmUiPgogICAgPHJlY3QKICAgICAgIHN0eWxlPSJmaWxsOiNmZmZmZmY7ZmlsbC1vcGFjaXR5OjE7c3Ryb2tlOm5vbmU7c3Ryb2tlLXdpZHRoOjAuMzEyMDQ7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5OjEuMjQ4MTcsIDAuMzEyMDQ7c3Ryb2tlLWRhc2hvZmZzZXQ6MCIKICAgICAgIGlkPSJyZWN0NzQ5NjUiCiAgICAgICB3aWR0aD0iMjguNSIKICAgICAgIGhlaWdodD0iMjgiCiAgICAgICB4PSItMy41NTI3MTM3ZS0xNSIKICAgICAgIHk9IjAiIC8+CiAgICA8ZwogICAgICAgaWQ9ImcxMzE4IgogICAgICAgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoLTIzLjE2Mzc0LC05NS41MDg0NTIpIj4KICAgICAgPGcKICAgICAgICAgYXJpYS1sYWJlbD0iVSIKICAgICAgICAgaWQ9InRleHQzNjI5NCIKICAgICAgICAgc3R5bGU9ImZvbnQtc2l6ZTozMi44Mzk5cHg7bGluZS1oZWlnaHQ6MS4yNTtsZXR0ZXItc3BhY2luZzowcHg7d29yZC1zcGFjaW5nOjBweDtmaWxsOiNmZmZmZmY7c3Ryb2tlOiNhYTAwMDA7c3Ryb2tlLXdpZHRoOjAuNzA1NTU2O3N0cm9rZS1taXRlcmxpbWl0OjQ7c3Ryb2tlLWRhc2hhcnJheTpub25lIj4KICAgICAgICA8cGF0aAogICAgICAgICAgIGQ9Im0gMzQuNzQ1MzQ1LDEyMS44NDA0MSBjIC0zLjExMDgxMSwwIC01LjU3MzY4MSwtMC43NTkgLTcuMjMwNjQyLC0yLjI3Njk5IC0xLjY0NjI3MSwtMS41MTc5OSAtMi40Njk0MDYsLTMuNjg4MDcgLTIuNDY5NDA2LC02LjUxMDI1IFYgOTguOTI2MjQgaCA0Ljk0MTMxNiB2IDEzLjc1ODEyIGMgMCwxLjc4NTI1IDAuNDIyMjU4LDMuMTQyODkgMS4yNjY3NzQsNC4wNzI5MiAwLjg1NTIwNiwwLjkxOTM1IDEuOTczOTY4LDEuMzc5MDIgMy42MjAyMzksMS4zNzkwMiAxLjY4OTAzMSwwIDIuODg2MjI1LC0wLjQ4MTA1IDMuNzk0ODgyLC0xLjQ0MzE2IDAuOTA4NjU2LC0wLjk3MjggMS4zNTg5OTksLTIuMzYyNTEgMS4zNjI5ODUsLTQuMTY5MTMgViA5OC45MjYyNCBoIDQuOTk5NDIxIHYgMTMuODg2NCBjIDAsMi44NjQ5NCAtMC44ODcyNzYsNS4wODg0OCAtMi42NjE4MjgsNi42NzA2MSAtMS43NjM4NjEsMS41NzE0NCAtNC40MTY3MTksMi4zNTcxNiAtNy42MjM3NDEsMi4zNTcxNiB6IgogICAgICAgICAgIHN0eWxlPSJmb250LXdlaWdodDpib2xkO2ZvbnQtZmFtaWx5OidMaWJlcmF0aW9uIFNhbnMnOy1pbmtzY2FwZS1mb250LXNwZWNpZmljYXRpb246J0xpYmVyYXRpb24gU2FucyBCb2xkJztmaWxsOiNmZmZmZmY7c3Ryb2tlOiNhYTAwMDA7c3Ryb2tlLXdpZHRoOjAuNzA1NTU2O3N0cm9rZS1taXRlcmxpbWl0OjQ7c3Ryb2tlLWRhc2hhcnJheTpub25lIgogICAgICAgICAgIGlkPSJwYXRoNzE3MTUiCiAgICAgICAgICAgc29kaXBvZGk6bm9kZXR5cGVzPSJzY3NjY3Njc2NzY2NzY3MiIC8+CiAgICAgIDwvZz4KICAgICAgPGcKICAgICAgICAgYXJpYS1sYWJlbD0iVSIKICAgICAgICAgaWQ9InRleHQzNjI5NC0yIgogICAgICAgICBzdHlsZT0iZm9udC1zaXplOjMyLjgzOTlweDtsaW5lLWhlaWdodDoxLjI1O2xldHRlci1zcGFjaW5nOjBweDt3b3JkLXNwYWNpbmc6MHB4O2ZpbGw6I2ZmZmZmZjtzdHJva2U6I2FhMDAwMDtzdHJva2Utd2lkdGg6MC43MDU1NTY7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5Om5vbmUiCiAgICAgICAgIHRyYW5zZm9ybT0idHJhbnNsYXRlKDQuOTQxMzE2KSI+CiAgICAgICAgPHBhdGgKICAgICAgICAgICBkPSJtIDM0Ljc0NTM0NSwxMjEuODQwNDEgYyAtMy4xMTA4MTEsMCAtNS41NzM2ODEsLTAuNzU5IC03LjIzMDY0MiwtMi4yNzY5OSAtMS42NDYyNzEsLTEuNTE3OTkgLTIuNDY5NDA2LC0zLjY4ODA3IC0yLjQ2OTQwNiwtNi41MTAyNSBWIDk4LjkyNjI0IGggNC45NDEzMTYgdiAxMy43NTgxMiBjIDAsMS43ODUyNSAwLjQyMjI1OCwzLjE0Mjg5IDEuMjY2Nzc0LDQuMDcyOTIgMC44NTUyMDYsMC45MTkzNSAxLjk3Mzk2OCwxLjM3OTAyIDMuNjIwMjM5LDEuMzc5MDIgMS42ODkwMzEsMCAyLjg4NjIyNSwtMC40ODEwNSAzLjc5NDg4MiwtMS40NDMxNiAwLjkwODY1NiwtMC45NzI4IDEuMzU4OTk5LC0yLjM2MjUxIDEuMzYyOTg1LC00LjE2OTEzIFYgOTguOTI2MjQgaCA0Ljk5OTQyMSB2IDEzLjg4NjQgYyAwLDIuODY0OTQgLTAuODg3Mjc2LDUuMDg4NDggLTIuNjYxODI4LDYuNjcwNjEgLTEuNzYzODYxLDEuNTcxNDQgLTQuNDE2NzE5LDIuMzU3MTYgLTcuNjIzNzQxLDIuMzU3MTYgeiIKICAgICAgICAgICBzdHlsZT0iZm9udC13ZWlnaHQ6Ym9sZDtmb250LWZhbWlseTonTGliZXJhdGlvbiBTYW5zJzstaW5rc2NhcGUtZm9udC1zcGVjaWZpY2F0aW9uOidMaWJlcmF0aW9uIFNhbnMgQm9sZCc7ZmlsbDojZmZmZmZmO3N0cm9rZTojYWEwMDAwO3N0cm9rZS13aWR0aDowLjcwNTU1NjtzdHJva2UtbWl0ZXJsaW1pdDo0O3N0cm9rZS1kYXNoYXJyYXk6bm9uZSIKICAgICAgICAgICBpZD0icGF0aDcxNzE1LTciCiAgICAgICAgICAgc29kaXBvZGk6bm9kZXR5cGVzPSJzY3NjY3Njc2NzY2NzY3MiIC8+CiAgICAgIDwvZz4KICAgICAgPHJlY3QKICAgICAgICAgc3R5bGU9ImZpbGw6I2ZmZmZmZjtmaWxsLW9wYWNpdHk6MTtzdHJva2U6bm9uZTtzdHJva2Utd2lkdGg6MC43MDU1NTY7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5Om5vbmUiCiAgICAgICAgIGlkPSJyZWN0NzI2NzMiCiAgICAgICAgIHdpZHRoPSIyNi4xOTQ1NTMiCiAgICAgICAgIGhlaWdodD0iMi4wOTM5NzM2IgogICAgICAgICB4PSIyNC4zMTY0NjMiCiAgICAgICAgIHk9Ijk2LjgyMzcxNSIgLz4KICAgICAgPHJlY3QKICAgICAgICAgc3R5bGU9ImZpbGw6I2ZmZmZmZjtmaWxsLW9wYWNpdHk6MTtzdHJva2U6bm9uZTtzdHJva2Utd2lkdGg6MC44NDgyMDI7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5Om5vbmUiCiAgICAgICAgIGlkPSJyZWN0NzI2NzMtMyIKICAgICAgICAgd2lkdGg9IjQuMjM3MjQ1MSIKICAgICAgICAgaGVpZ2h0PSIwLjkxNDAzNzM1IgogICAgICAgICB4PSI0MC4zODMxODMiCiAgICAgICAgIHk9Ijk4LjY5NDYzMyIgLz4KICAgICAgPHJlY3QKICAgICAgICAgc3R5bGU9ImZpbGw6I2ZmZmZmZjtmaWxsLW9wYWNpdHk6MTtzdHJva2U6bm9uZTtzdHJva2Utd2lkdGg6MC44NDc5NTI7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5Om5vbmUiCiAgICAgICAgIGlkPSJyZWN0NzI2NzMtMy02IgogICAgICAgICB3aWR0aD0iNC4yMzQ3NDQ1IgogICAgICAgICBoZWlnaHQ9IjAuOTE0MDM3MzUiCiAgICAgICAgIHg9IjMwLjMzOTcxMiIKICAgICAgICAgeT0iOTguNjk0NjQ5IiAvPgogICAgICA8ZWxsaXBzZQogICAgICAgICBzdHlsZT0iZmlsbDojZmZmZmZmO2ZpbGwtb3BhY2l0eToxO3N0cm9rZTojYWEwMDAwO3N0cm9rZS13aWR0aDowLjcwNTU1NjtzdHJva2UtbWl0ZXJsaW1pdDo0O3N0cm9rZS1kYXNoYXJyYXk6bm9uZSIKICAgICAgICAgaWQ9InBhdGg3MjI5NiIKICAgICAgICAgY3g9IjI3LjUyMTUxNyIKICAgICAgICAgY3k9Ijk4LjkzNzg2NiIKICAgICAgICAgcng9IjIuNDc3MzUxNCIKICAgICAgICAgcnk9IjEuNTU4Njk4MyIgLz4KICAgICAgPGVsbGlwc2UKICAgICAgICAgc3R5bGU9ImZpbGw6I2ZmZmZmZjtmaWxsLW9wYWNpdHk6MTtzdHJva2U6I2FhMDAwMDtzdHJva2Utd2lkdGg6MC43MDU1NTY7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5Om5vbmUiCiAgICAgICAgIGlkPSJwYXRoNzIyOTYtMCIKICAgICAgICAgY3g9IjM3LjQ5NjUwMiIKICAgICAgICAgY3k9Ijk4LjkzNzg2NiIKICAgICAgICAgcng9IjIuNTIzMTc2MiIKICAgICAgICAgcnk9IjEuNTU0NTM5NiIgLz4KICAgICAgPGVsbGlwc2UKICAgICAgICAgc3R5bGU9ImZpbGw6I2ZmZmZmZjtmaWxsLW9wYWNpdHk6MTtzdHJva2U6I2FhMDAwMDtzdHJva2Utd2lkdGg6MC43MDU1NTY7c3Ryb2tlLW1pdGVybGltaXQ6NDtzdHJva2UtZGFzaGFycmF5Om5vbmUiCiAgICAgICAgIGlkPSJwYXRoNzIyOTYtOSIKICAgICAgICAgY3g9IjQ3LjQ5NjUxNyIKICAgICAgICAgY3k9Ijk4LjkzNzg2NiIKICAgICAgICAgcng9IjIuNDc3MzUxNCIKICAgICAgICAgcnk9IjEuNTU4Njk4MyIgLz4KICAgIDwvZz4KICA8L2c+Cjwvc3ZnPgo="
        FoConverter {
            ast = Document().apply {
                p {
                    roles("logo")
                    img(myImage) {
                        width = Length(3500F, LengthUnit.cmm)
                        height = Length(3500F, LengthUnit.cmm)
                    }
                }
                p { roles("title"); +"Some title paragraph 1" }
                p {
                    +"Some "; span { roles("strong"); +"paragraph" }; +" 2 with C"
                    span { roles("sup"); +"sup" }; span { roles("sub"); +"sub" }
                }
                table {
                    repeat(3) { col(Length(1F)) }
                    tableRowGroup(TRG.head) { tr { (1..3).forEach { td { p { +"Column $it" } } } } }
                    tableRowGroup(TRG.body) {
                        (1..10).forEach { row ->
                            tr { (1..3).forEach { td { p { +"Cell $row.$it" } } } }
                        }
                    }
                }
            }
            foStyleList = FoStyleList(
                FoStyle {
                    if (it !is Paragraph) return@FoStyle
                    if (!it.roles.contains("title")) return@FoStyle
                    attributes("font-size" to "16pt")
                }, FoStyle {
                    if (it !is Paragraph) return@FoStyle
                    attributes("font-family" to "Liberation Serif", "space-after" to "2mm")
                }, FoStyle {
                    if (it !is TableCell) return@FoStyle
                    attributes("border" to "solid 0.7pt black", "padding" to "1mm")
                }, FoStyle { thParagraph ->
                    if (thParagraph !is Paragraph) return@FoStyle
                    if (thParagraph.ancestor { it is TableRowGroup && it.type == TRG.head }.isEmpty()) return@FoStyle
                    attributes("text-align" to "center", "font-weight" to "bold", "font-size" to "85%")
                }, FoStyle { logoParagraph ->
                    if (logoParagraph !is Paragraph) return@FoStyle
                    if (!logoParagraph.roles.contains("logo")) return@FoStyle
                    attributes("text-align" to "center")
                }
            )
            generatePre()
            template = File("src/test/data/simple-xsl-fo.xml").readText()
            generateFo()
            val out = FileOutputStream("build/test-basic.pdf")
            val fop = FopFactory.newInstance(File("src/test/data/fop.xconf"))
                .newFop(MimeConstants.MIME_PDF, out);
            fopTransform(fo().parseStringAsXML(), fop)
        }
    }
}