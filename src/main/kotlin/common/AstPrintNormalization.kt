package common

import model.Image
import model.Length
import model.LengthUnit
import model.Node

fun getBestDimensions(
    widthPx: Float,
    heightPx: Float,
    fitRectWidthMmm: Float? = null,
    fitRectHeightMmm: Float? = null,
    srcDpi: Float? = null,
): ImageMeta? {
    if (srcDpi != null) {
        val (dpiExpectedWidth, dpiExpectedHeight) = arrayOf(widthPx, heightPx).map { it / srcDpi * 2540 }
        if ((fitRectWidthMmm == null || fitRectHeightMmm == null) || (dpiExpectedWidth <= fitRectWidthMmm && dpiExpectedHeight <= fitRectHeightMmm)) return ImageMeta(
            dpiExpectedWidth,
            dpiExpectedHeight,
            LengthUnit.cmm
        )
    }
    if (fitRectWidthMmm != null && fitRectHeightMmm != null) {
        if (widthPx / heightPx > fitRectWidthMmm / fitRectHeightMmm) {
            return (ImageMeta(
                fitRectWidthMmm, fitRectWidthMmm / widthPx * heightPx, LengthUnit.cmm
            ))
        } else {
            return (ImageMeta(
                fitRectHeightMmm / heightPx * widthPx, fitRectHeightMmm, LengthUnit.cmm
            ))
        }
    }
    return null
}

fun Node.setImageBestFitDimensions() {
    this.descendant { it is Image }.map { it as Image }.forEach { image ->
        val reFitRect = """^fitrect-([0-9]+)-([0-9]+)$""".toRegex()
        val reSrcDpi = """^srcdpi-([0-9]+)$""".toRegex()
        val thisOrAncestorRoles = arrayListOf<String>()
        image.ancestor().forEach { thisOrAncestorRoles.addAll(it.roles) }
        thisOrAncestorRoles.addAll(image.roles)

        val srcDpi =
            thisOrAncestorRoles.firstNotNullOfOrNull { reSrcDpi.matchEntire(it) }
                ?.run { groupValues[1] }?.toFloat()
        val (fitRectWidthMmm, fitRectHeightMmm) = run {
            val matchResult = thisOrAncestorRoles.firstNotNullOfOrNull { reFitRect.matchEntire(it) }
            if (matchResult != null) {
                arrayOf(
                    matchResult.groupValues[1].toFloat(),
                    matchResult.groupValues[2].toFloat()
                )
            } else arrayOf<Float?>(null, null)
        }
        val realImageMeta = imageMeta(image.src)
        val bestFitImageMeta = getBestDimensions(
            realImageMeta.width, realImageMeta.height,
            fitRectWidthMmm, fitRectHeightMmm, srcDpi
        )
        if (bestFitImageMeta != null) {
            image.width = Length(bestFitImageMeta.width, bestFitImageMeta.unit)
            image.height = Length(bestFitImageMeta.height, bestFitImageMeta.unit)
        }
    }
}

fun Node.normalizeImageDimensions(portraitWidth: Float = 17000F, basicDpi: Float = 96F) {
    this.descendant { it is Image }.map { it as Image }.forEach { image ->
        val (imageWidth, imageHeight) = arrayOf(image.width, image.height)
        arrayOf(imageWidth, imageHeight).filterNotNull().forEach {
            when (it.unit) {
                LengthUnit.perc -> {
                    it.unit = LengthUnit.cmm
                    it.value *= portraitWidth / 100
                }

                LengthUnit.px -> {
                    it.unit = LengthUnit.cmm
                    it.value = it.value * 2540 / basicDpi
                }

                else -> {}
            }
        }
        if (imageHeight == null) {
            val imageMeta = imageMeta(image.src)
            val newImageWidth = run {
                imageWidth ?: Length(imageMeta.width / basicDpi * 2540F, LengthUnit.cmm)
            }
            image.width = newImageWidth
            val proportionalHeight = newImageWidth.value / imageMeta.width * imageMeta.height
            image.height = Length(proportionalHeight, newImageWidth.unit)
        }
    }
}