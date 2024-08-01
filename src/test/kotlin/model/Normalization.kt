package model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.approvaltests.Approvals
import org.junit.jupiter.api.Test
import verify

class Normalization {

    @Test
    fun liAlreadyWrappedContents() {
        ListItem().apply { p { +"Некоторый текст"; span { +"и еще"; +" текст" } } }
            .wrapListItemContents().toYamlString().verify()
    }

    @Test
    fun liSimpleContents() {
        ListItem().apply { +"Некоторый текст"; span { +"и еще"; +" текст" } }
            .wrapListItemContents().toYamlString().verify()
    }
}

