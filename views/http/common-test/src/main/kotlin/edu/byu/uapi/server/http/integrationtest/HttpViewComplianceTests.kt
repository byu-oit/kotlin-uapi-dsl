package edu.byu.uapi.server.http.integrationtest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.FuelManager
import edu.byu.uapi.server.http.HttpResponse
import edu.byu.uapi.server.http.HttpResponseBody
import edu.byu.uapi.server.http.HttpRoute
import edu.byu.uapi.server.http.HttpRouteSource
import edu.byu.uapi.server.http.integrationtest.dsl.ComplianceSuite
import edu.byu.uapi.server.http.test.fixtures.FakeHttpRouteSource
import me.alexpanov.net.FreePortFinder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.OutputStream
import java.net.InetAddress
import java.util.stream.Stream
import kotlin.system.exitProcess
import kotlin.test.assertFalse

@Suppress("FunctionName")
abstract class HttpViewComplianceTests<Handle : Any> {

    @TestFactory
    fun simpleRouting() = runSuite(simpleRoutingTests)

    @AfterEach
    fun stopServers() {
        var panic = false
        handles.forEach { (name, handle) ->
            try {
                stopServer(handle)
            } catch (ex: Throwable) {
                panic = true
                System.err.println(" !!!!! Error stopping it server ${this::class.simpleName} $name! !!!!! ")
                ex.printStackTrace()
                Thread.sleep(1)
            }
        }
        if (panic) {
            System.err.println("Due to server stopping failure, we're just gonna panic and exit the JVM process, just to be safe. Bye Bye!")
            exitProcess(11111)
        }
    }

    private val handles = mutableMapOf<String, Handle>()

    private fun runSuite(suite: ComplianceSuite): Stream<DynamicNode> {
        val routes = suite.buildRoutes()
        val server = startServer(suite.name, routes)

        return suite.buildTests(server)
    }

    private fun startServer(name: String, routes: List<HttpRoute>): ServerInfo {
        val addr = InetAddress.getLocalHost()
        val port = FreePortFinder.findFreeLocalPort(11111, addr)

        val baseUrl = "http://${addr.hostAddress}:$port"
        println("\t-------- Starting it server for '$name' at $baseUrl --------")

        handles += (name to startServer(FakeHttpRouteSource(routes), addr, port))

        return ServerInfo(addr, port, baseUrl)
    }

    @Test
    fun `server starts on requested port`() {
        val serverInfo = startServer("init it", emptyList())
        assertFalse(FreePortFinder.available(serverInfo.port, serverInfo.address))
    }


    abstract fun startServer(routes: HttpRouteSource, address: InetAddress, port: Int): Handle
    abstract fun stopServer(handle: Handle)

}

data class ServerInfo(
    val address: InetAddress,
    val port: Int,
    val url: String
)

internal fun client(serverInfo: ServerInfo, path: String): FuelManager {
    return FuelManager().apply { basePath = "${serverInfo.url}/$path" }
}

val jackson = ObjectMapper().registerKotlinModule()

sealed class TestResponse(
    override val status: Int,
    override val headers: Map<String, String>
) : HttpResponse {

    companion object {
        fun Json(status: Int, body: String, headers: Map<String, String> = emptyMap()) =
            Body(status, body.toByteArray(), "application/json", headers)
    }

    class Empty(
        status: Int,
        headers: Map<String, String> = emptyMap()
    ) : TestResponse(status, headers) {
        override val body: HttpResponseBody? = null
    }

    class Body(
        status: Int,
        val bodyBytes: ByteArray,
        override val contentType: String,
        headers: Map<String, String> = emptyMap()
    ) : TestResponse(status, headers + ("Content-Type" to contentType)), HttpResponseBody {
        constructor(
            status: Int,
            bodyString: String,
            contentType: String,
            headers: Map<String, String> = emptyMap()
        ) : this(status, bodyString.toByteArray(), contentType, headers)

        override val body: HttpResponseBody? = this
        override fun writeTo(stream: OutputStream) {
            stream.write(bodyBytes)
        }
    }
}