package common

import model.LengthUnit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Base64

class CommonImageKtTest {
    @Test
    fun base64PngMeta() {
        val base64String = """data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="""
        assertEquals(imageMeta(base64String), ImageMeta(5F, 5F, LengthUnit.px))
    }

    @Test
    fun base64SvgMeta() {
        val svgString = """
            <svg viewBox="0 0 200 100" xmlns="http://www.w3.org/2000/svg">
              <rect width="200" height="100" x="0" y="0" fill="orange" />
              <rect width="180" height="80" x="10" y="10" fill="wheat" />
            </svg>
        """.trimIndent()
        val base64String =
            """data:image/svg;base64,${Base64.getEncoder().encodeToString(svgString.toByteArray())}"""
        assertEquals(imageMeta(base64String), ImageMeta(200F, 100F, LengthUnit.parrots))
    }
}