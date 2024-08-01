package common

import model.LengthUnit

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AstPrintNormalizationKtTest {

    @ParameterizedTest
    @CsvSource(
        "300,60,  1000000,300,  300,  1500,300",
        "300,60,  1000000,600,  300,  2540,508",
        "300,60,  500,600,      300,  500,100",
    )
    fun getBestDimensions(
        widthPx: Float,
        heightPx: Float,
        fitRectWidthMmm: Float? = null,
        fitRectHeightMmm: Float? = null,
        srcDpiMmm: Float? = null,
        expectedWidth: Float?,
        expectedHeight: Float?
    ) {
        if (expectedWidth != null && expectedHeight != null) {
            assertEquals(
                ImageMeta(expectedWidth, expectedHeight, LengthUnit.mmm),
                getBestDimensions(widthPx, heightPx, fitRectWidthMmm, fitRectHeightMmm, srcDpiMmm)
            )
        } else {
            assertEquals(
                null,
                getBestDimensions(widthPx, heightPx, fitRectWidthMmm, fitRectHeightMmm, srcDpiMmm)
            )
        }
    }
}