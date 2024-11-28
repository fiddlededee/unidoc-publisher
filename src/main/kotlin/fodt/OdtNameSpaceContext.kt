package fodt

import javax.xml.namespace.NamespaceContext

class OdtNameSpaceContext : NamespaceContext {
    override fun getNamespaceURI(p0: String?): String? {
        val namespaceURI = when (p0) {
            "text" -> "urn:oasis:names:tc:opendocument:xmlns:text:1.0"
            "style" -> "urn:oasis:names:tc:opendocument:xmlns:style:1.0"
            "fo" -> "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
            "office" -> "urn:oasis:names:tc:opendocument:xmlns:office:1.0"
            "table" -> "urn:oasis:names:tc:opendocument:xmlns:table:1.0"
            "draw" -> "urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
            "svg" -> "http://www.w3.org/2000/svg"
            "loext" -> "urn:org:documentfoundation:names:experimental:office:xmlns:loext:1.0"
            else -> null
        }
        return namespaceURI
    }

    override fun getPrefix(p0: String?): String {
        throw UnsupportedOperationException()
    }

    override fun getPrefixes(p0: String?): MutableIterator<String> {
        throw UnsupportedOperationException()
    }
}

