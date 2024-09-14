#!/usr/bin/env kotlin
@file:DependsOn("ru.fiddlededee:unidoc-publisher:0.7.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import common.normalizeImageDimensions
import converter.FodtConverter
import model.*
import writer.*
import java.io.File

//language=JSON
val json = """[
  {
    "state": "IL",
    "city": "Chicago",
    "fuel_type_code": "CNG",
    "zip": "60607",
    "station_name": "Clean Energy - Yellow Cab",
    "cards_accepted": "A D M V Voyager Wright_Exp CleanEnergy",
    "street_address": "540 W Grenshaw"
  },
  {
    "state": "IL",
    "city": "Chicago",
    "fuel_type_code": "CNG",
    "zip": "60621",
    "station_name": "City of Chicago FS#9",
    "cards_accepted": "A D M V Voyager Wright_Exp CleanEnergy",
    "street_address": "25 W 65th St"
  }
]
""".trimIndent()

data class Station(val streetAddress: String, val stationName: String, val cardsAccepted: String)

val stations = jacksonObjectMapper()
    .setPropertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .readValue<Array<Station>>(json)
    .filter { it.cardsAccepted.matches(""".*[vV]oyager.*""".toRegex()) }

// tag::letter-builder[]
val letterAst = Document().apply {
    p { +"Dear Boss:" }
    p { +"Here are the CNG stations that accept Voyager cards:" }
    table {
        repeat(3) { col(Length(1F)) }
        tableRowGroup(TRG.head) {
            tr {
                arrayOf("Station", "Address", "Cards Accepted")
                    .forEach { td { p { +it } } }
            }
        }
        stations.forEach { station ->
            tr {
                arrayOf(
                    station.stationName, station.streetAddress,
                    station.cardsAccepted
                ).forEach { td { p { +it } } }
            }
        }
    }
    p { roles("signature"); +"Your loyal servant" }
    p { +"John Hancock" }
    p {
        id = "john-hancock-signature"
        img(src = "${__FILE__.parent}/JohnHancock.png") {
            width = Length(40F, LengthUnit.mm)
        }
    }
}
// end::letter-builder[]

val letterOdtStyleList = OdtStyleList(
    // tag::odt-style[]
    OdtStyle { node ->
        if (node !is TableCell) return@OdtStyle
        tableCellProperties {
            arrayOf("top", "right", "bottom", "left").forEach {
                attribute("fo:border-$it", "0.5pt solid #000000")
                if (it != "bottom") attribute("fo:padding-$it", "1mm")
            }
        }
    },
    // end::odt-style[]
    OdtStyle { node ->
        if (node !is Paragraph) return@OdtStyle
        if (node.parent() is TableCell) paragraphProperties {
            attribute("fo:line-height", "100%")
            attribute("fo:margin-bottom", "1mm")
        } else paragraphProperties {
            attribute("fo:text-indent", "10mm");
        }
        val grandGrandParent = node.parent()?.parent()?.parent()
        if (grandGrandParent is TableRowGroup && grandGrandParent.type == TRG.head) paragraphProperties {
            attribute("fo:text-align", "center")
        }
        if (node.roles.contains("signature")) paragraphProperties {
            attribute("fo:margin-top", "2mm"); attribute("fo:margin-bottom", "0mm")
        }
    }
)

FodtConverter {
    ast = letterAst
    template = File("approved/asciidoc/template-1.fodt").readText()
    odtStyleList = letterOdtStyleList
    ast().normalizeImageDimensions()
    ast2fodt()
    File("${__FILE__.parent}/output/letter.fodt").writeText(fodt())
}

"Finished"