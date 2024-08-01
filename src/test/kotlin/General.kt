import model.Node
import org.approvaltests.Approvals
import org.junit.jupiter.api.Assertions
import kotlin.reflect.KClass

fun String.verify() {
    Approvals.verify(this)
}

infix fun <T> T.shouldBe(to: T) {
    Assertions.assertEquals(to, this)
}

fun ArrayList<Node>.nodeSequence(): String {
    return this.joinToString(" -> ") { it::class.java.simpleName }
}

fun ArrayList<Node>.nodeRolesSequence(): String {
    return this.joinToString(" -> ") { it.roles.joinToString(", ") }
}


