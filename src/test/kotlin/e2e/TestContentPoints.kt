package e2e

import common.asciidoc2PdfApprove
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File


class TestContentPoints {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "content-points-1",
            "content-points-2",
            "content-points-3",
            "content-points-4",
        ]
    )
    fun withTemplate2(key: String) {
        File("approved/asciidoc/$key.adoc")
            .readText()
            .asciidoc2PdfApprove(key) {
                template = File("approved/asciidoc/template-2.fodt").readText()
            }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "content-points-5",
        ]
    )
    fun withTemplate1(key: String) {
        File("approved/asciidoc/$key.adoc")
            .readText()
            .asciidoc2PdfApprove(key) {
                template = File("approved/asciidoc/template-1.fodt").readText()
            }
    }
}