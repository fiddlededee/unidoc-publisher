package common

import com.sun.star.beans.PropertyValue
import com.sun.star.beans.XPropertySet
import com.sun.star.bridge.XBridgeFactory
import com.sun.star.comp.helper.Bootstrap
import com.sun.star.connection.XConnection
import com.sun.star.connection.XConnector
import com.sun.star.frame.*
import com.sun.star.lang.XComponent
import com.sun.star.lang.XMultiComponentFactory
import com.sun.star.text.XDocumentIndex
import com.sun.star.text.XDocumentIndexesSupplier
import com.sun.star.text.XTextDocument
import com.sun.star.uno.UnoRuntime
import com.sun.star.uno.XComponentContext
import java.io.File
import java.io.IOException
import kotlin.io.path.Path
import kotlin.system.exitProcess

object Lo {
    private val outputFormats = "pdf"
    private val outputFileName : String? = null
    fun fodtToPdf(inputDoc: String) {
        val matchedDocBasePath = """.*(?=[\.][a-zA-Z_]+$)""".toRegex().find(inputDoc)
        val outputDocBasePath = matchedDocBasePath?.value ?: inputDoc
        val outputDocBaseFolder = Path(inputDoc).parent
            ?: throw Exception("Can't extract folder from \"${outputDocBasePath}\"")

        val xContext = socketContext()
        val xMCF: XMultiComponentFactory = xContext.serviceManager ?: throw Exception("Can't get xContext service manager")
        val dispatcherHelper = xMCF.createInstanceWithContext("com.sun.star.frame.DispatchHelper", xContext)!!
        val xDispatcherHelper = qi(XDispatchHelper::class.java, dispatcherHelper)
        val desktop: Any = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext)
        val xDeskop = qi(XDesktop::class.java, desktop)
        val xComponentLoader = qi(XComponentLoader::class.java, desktop)
        val loadProps = arrayOf<PropertyValue>()
        lateinit var component: XComponent
        try {
            component = xComponentLoader.loadComponentFromURL(fnmToURL(inputDoc), "_blank", 0, loadProps)
        } catch (e: Exception) {
            println(e)
            if (trace) {
                e.stackTrace.forEach {
                    println(it.toString())
                }
            }
            println("ERROR: Unable to open $inputDoc. If file exists and not corrupted try to delete LibreOffice lock files")
            exitProcess(-1)
        }
        val xTextDocument = qi(XTextDocument::class.java, component)
        val xDispatchProvider = qi(XDispatchProvider::class.java, xDeskop.currentFrame)

// Update indexes
        val indexes = qi(XDocumentIndexesSupplier::class.java, xTextDocument)
        for (i in 0..indexes.documentIndexes.count - 1) {
            val index = qi(XDocumentIndex::class.java, indexes.documentIndexes.getByIndex(i))
            index.update()
        }
        println("INFO: Indexes updated")

        xDispatcherHelper.executeDispatch(xDispatchProvider, ".uno:UpdateFields", "", 0, arrayOf<PropertyValue>())
        println("INFO: Fields updated")

        val xStorable = qi(XStorable::class.java, component)
        val saveProps = Array(2) { PropertyValue() }
        saveProps[0].Name = "Overwrite"
        saveProps[0].Value = true
        outputFormats.split(",").forEach { it ->
            saveProps[1].Name = "FilterName"
            saveProps[1].Value = ext2format(it)
            val outputPath = if (outputFileName == null)
                "$outputDocBasePath.$it" else "$outputDocBaseFolder/$outputFileName.$it"
            try {
                xStorable.storeToURL(fnmToURL(outputPath), saveProps)
                println("INFO: $outputPath stored")
            } catch (e: Exception) {
                println(e)
                if (trace) {
                    e.stackTrace.forEach { stackTraceElement ->
                        println(stackTraceElement.toString())
                    }
                }
                println("ERROR: Unable to save $outputPath. Probably file is locked")
            }
        }

        xDeskop.terminate()
    }

    val loCliCommand = "soffice"
    val trace = false

    fun socketContext(): XComponentContext // use socket connection to Office
// https://forum.openoffice.org/en/forum/viewtopic.php?f=44&t=1014
    {
        val xcc: XComponentContext?  // the remote office component context
        try {
            val cmdArray = arrayOfNulls<String>(3)
            cmdArray[0] = loCliCommand
            // requires soffice to be in Windows/Linux PATH env var.
            cmdArray[1] = "--headless"
            cmdArray[2] = "--accept=socket,host=localhost,port=" +
                    "8100" + ";urp;"
            val p = Runtime.getRuntime().exec(cmdArray)
            if (p != null) println("INFO: Office process created")
            val localContext = Bootstrap.createInitialComponentContext(null)
            // Get the local service manager
            val localFactory = localContext.serviceManager
            // connect to Office via its socket
            val connector: XConnector = qi(
                XConnector::class.java,
                localFactory.createInstanceWithContext(
                    "com.sun.star.connection.Connector", localContext
                )
            )
            lateinit var connection: XConnection
            var connected = false
            var attempts = 30
            while (attempts > 0 && !connected) {
                if (trace) {
                    println("TRACE: $attempts attempts left")
                }
                try {
                    connection = connector.connect(
                        "socket,host=localhost,port=" + "8100"
                    )
                    connected = true
                } catch (_: Exception) {
                }
                delay(500)
                attempts -= 1
            }

            // create a bridge to Office via the socket
            val bridgeFactory: XBridgeFactory = qi(
                XBridgeFactory::class.java,
                localFactory.createInstanceWithContext(
                    "com.sun.star.bridge.BridgeFactory", localContext
                )
            )

            // create a nameless bridge with no instance provider
            val bridge = bridgeFactory.createBridge("socketBridgeAD", "urp", connection, null)

            // get the remote service manager
            val serviceManager: XMultiComponentFactory = qi(
                XMultiComponentFactory::class.java,
                bridge.getInstance("StarOffice.ServiceManager")
            )

            // retrieve Office's remote component context as a property
            val props: XPropertySet = qi(XPropertySet::class.java, serviceManager)
            val defaultContext = props.getPropertyValue("DefaultContext")

            // get the remote interface XComponentContext
            xcc = qi(XComponentContext::class.java, defaultContext)
        } catch (e: Exception) {
            println(e.message)
            if (trace) {
                e.stackTrace.forEach {
                    println(it.toString())
                }
            }
            println("ERROR: Unable to socket connect to Office")
            exitProcess(-1)
        }
        return xcc
    } // end of socketContext()

    fun delay(ms: Int) {
        try {
            Thread.sleep(ms.toLong())
        } catch (_: InterruptedException) {
        }
    }

    // ====== interface object creation wrapper (uses generics) ===========
    fun <T> qi(aType: Class<T>?, o: Any?): T // the "Loki" function -- reduces typing
    {
        return UnoRuntime.queryInterface(aType, o)
    }

    fun fnmToURL(fnm: String): String? // convert a file path to URL format
    {
        return try {
            val sb: StringBuffer?
            val path = File(fnm).canonicalPath
            sb = StringBuffer("file:///")
            sb.append(path.replace('\\', '/'))
            sb.toString()
        } catch (e: IOException) {
            println("Could not access $fnm")
            null
        }
    } // end of fnmToURL()

    fun ext2format(ext: String): String {
        val format = when (ext) {
            "docx" -> "Office Open XML Text"
            "pdf" -> "writer_pdf_Export"
            "odt" -> "writer8"
            "fodt" -> "OpenDocument Text Flat XML"
            "html" -> "HTML (StarWriter)"
            else -> null
        }
        if (format == null) {
            throw Exception("Output format [$ext] not supported")
        } else {
            return format
        }
    }
}

