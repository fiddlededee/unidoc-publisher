package common

import fodt.iterable
import fodt.parseStringAsXML
import fodt.xpath
import model.LengthUnit
import org.w3c.dom.Attr
import java.util.*
import javax.imageio.ImageIO
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.IIOException

data class ImageMeta(val width: Float, val height: Float, val unit: LengthUnit)

fun imageMeta(src: String): ImageMeta {
    val base64Regex = """^data:image/(.*);base64,(.*)$""".toRegex()
    val matchResult = base64Regex.matchEntire(src)
    val type = if (matchResult == null) src.substringAfterLast('.', "") else
        matchResult.groupValues[1]
    return if (type == "svg" || type == "svg+xml") svgImageMeta(matchResult, src) else
        binaryImageMeta(matchResult, src)
}

private fun svgImageMeta(matchResult: MatchResult?, src: String): ImageMeta {
    val svgXml = run {
        if (matchResult == null) {
            File(src).readText()
        } else {
            val base64Image = matchResult.groupValues[2]
            String(Base64.getDecoder().decode(base64Image))
        }
    }
    val viewBox = svgXml.parseStringAsXML().xpath("/svg:svg/@viewBox").iterable().map { it as Attr }
        .firstOrNull()?.value
        ?.split(" ") ?: throw Exception("ViewBox not found in svg file")
    return ImageMeta(viewBox[2].toFloat(), viewBox[3].toFloat(), LengthUnit.parrots)
}

private fun binaryImageMeta(matchResult: MatchResult?, src: String): ImageMeta {
    val img = run {
        if (matchResult == null) {
            try {
                ImageIO.read(File(src))
            }catch (e : IIOException) {
                throw IIOException("Can't read input file ${src}")
            }
        } else {
            val base64Image = matchResult.groupValues[2]
            val imageBytes: ByteArray = Base64.getDecoder().decode(base64Image)
            ImageIO.read(ByteArrayInputStream(imageBytes))
        }
    }
    return ImageMeta(img.width.toFloat(), img.height.toFloat(), LengthUnit.px)
}
