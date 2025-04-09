package model

import converter.fodt.FodtConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import reader.UnknownTagProcessing
import verify

class Normalization {

    @Test
    fun liAlreadyWrappedContents() {
        ListItem().apply { p { +"Некоторый текст"; span { +"и еще"; +" текст" } } }
            .wrapListItemContents().toYamlString().verify()
    }

    @Test
    fun liSimpleContents() {
        ListItem().apply { +"Некоторый текст"; span { +"и еще"; +" текст" } }
            .wrapListItemContents().toYamlString().verify()
    }

    @ParameterizedTest
    @CsvSource(
        """<div>\n<a>1</a>2\n</div>, |12|""",
        """<p>1<i> <b>2</b></i> 3</p>, |1 2 3|""",
        """<div>a <div>b</div> c</div>,|abc|""",
        """<p>a <em> b</em></p>,|a b|""",
        """<span>a <span/> b</span>,|a b|""",
        """<p>\n   start</p>,|start|""",
        """<p><span> a</span>\n    start</p>,|a start|""",
        """<p><img/>\n    start</p>,| start|""",
        """<p><span><img/></span>\n    start</p>,| start|""",
        """<p><span> </span>\n    start</p>,|start|""",
        """<p>start\n</p>,|start|""",
        """<p>start   <span>   </span></p>,|start|""",
        """<p>start   <img/></p>,|start |""",
        """<p>start   <span>a  </span></p>,|start a|""",
        """<p>start   <span>a  </span></p>,|start a|""",
        """<p>\na\n<span>\n   b\n</span>\nc\n</p>,|a b c|""",
        """<pre>  start</pre>,|  start|""",
        """<pre><span>  start</span></pre>,|  start|""",
    )
    fun inlineSpaces(htmlString: String, expected: String) {
        FodtConverter {
            html = htmlString.replace("""\n""", "\n")
            unknownTagProcessingRule = {
                if (nodeName() == "div") {
                    UnknownTagProcessing.PROCESS
                } else UnknownTagProcessing.UNDEFINDED
            }
            parse()
            println(1111)
            Assertions.assertEquals(expected, "|${ast().extractText()}|")
        }
    }
}

