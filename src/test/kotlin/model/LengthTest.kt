package model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.math.roundToInt

class LengthTest {
    @ParameterizedTest
    @CsvSource(
        "10pt,10,pt",
        "15.1mm,15.1,mm",
        "15.1in,15.1,inch",
        "10.5%,10.5,perc",
        ",,",
        "10km,,",
        "abrakadabra,,"
    )
    fun fromString(srcLength: String?, value: String?, unit: String?) {
        val length = Length.fromString(srcLength)
        if (length == null) {
            assertNull(value)
            assertNull(unit)
        } else {
            assertEquals(value!!.toFloat().times(1000).roundToInt(), length.value.times(1000).roundToInt())
            assertEquals(unit.toString(), unit)
        }
    }
}