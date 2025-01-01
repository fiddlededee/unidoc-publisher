package model

class Length(var value: Float, var unit: LengthUnit = LengthUnit.perc) {
    companion object Factory {
        fun fromString(length: String?): Length? {
            if (length == null) return null
            return LengthUnit.entries
                .map {
                    val inValueUnit = when (it) {
                        LengthUnit.inch -> "in"; LengthUnit.perc -> "%"
                        LengthUnit.parrots -> ""; else -> it.toString()
                    }
                    """^([0-9]+[.]?[0-9]*)(${inValueUnit})$""".toRegex()
                        .matchEntire(length) to it
                }.filter { it.first != null }
                .getOrNull(0)
                ?.run { Length(first!!.groupValues[1].toFloat(), second) }
        }
    }
}

enum class LengthUnit { cm, mm, inch, px, pt, pc, em, ex, ch, rem, vw, vh, vmin, vmax, perc, parrots, mmm }
