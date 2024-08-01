package converter

data class PreRegion(
    val includeTags: Set<String>,
    val pre: String,
    val inLine: Boolean
)
