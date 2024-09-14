package e2e

import common.asciidoc2PdfApprove
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File


class TestNoTuning {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "admonition-basic-1",
            "example-basic-1",
            "footnote-basic-1",
            "image-block-no-dimensions-1",
            "image-block-percent-base64-1",
            "image-inline-pixel-1",
            "image-inline-fitrect-1",
            "image-block-svg-srcdpi-fitrect-1",
            "inline-basic-span-types-1",
            "inline-symbols-1",
            "inline-links-1",
            "inline-text-multiline-1",
            "list-basic-1",
            "list-basic-2",
            "list-term-1",
            "list-complex-1",
            "listing-1",
            "listing-callout-1",
            "section-dedication-1",
            "section-preamble-1",
            "table-basic-grid-1",
            "table-simple-content-1",
            "table-complex-content-1",
            "table-with-title-1",
            "toc-basic-1",
            "toc-basic-2",
        ]
    )
    fun run(key: String) {
        File("approved/asciidoc/$key.adoc")
            .readText()
            .asciidoc2PdfApprove(key)
    }
}