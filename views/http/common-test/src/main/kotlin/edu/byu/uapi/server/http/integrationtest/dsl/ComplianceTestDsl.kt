package edu.byu.uapi.server.http.integrationtest.dsl

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.RequestFactory
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.interceptors.LogRequestInterceptor
import com.github.kittinunf.fuel.core.interceptors.LogResponseInterceptor
import com.github.kittinunf.fuel.util.encodeBase64UrlToString
import edu.byu.uapi.server.http.BodyConsumer
import edu.byu.uapi.server.http.HttpHandler
import edu.byu.uapi.server.http.HttpMethod
import edu.byu.uapi.server.http.HttpRequest
import edu.byu.uapi.server.http.HttpResponse
import edu.byu.uapi.server.http.HttpRoute
import edu.byu.uapi.server.http.integrationtest.ActualRequest
import edu.byu.uapi.server.http.integrationtest.ServerInfo
import edu.byu.uapi.server.http.integrationtest.TestResponse
import edu.byu.uapi.server.http.integrationtest.jackson
import edu.byu.uapi.server.http.path.CompoundVariablePathPart
import edu.byu.uapi.server.http.path.RoutePath
import edu.byu.uapi.server.http.path.SingleVariablePathPart
import edu.byu.uapi.server.http.path.StaticPathPart
import edu.byu.uapi.server.http.path.staticPart
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.net.URI
import java.util.stream.Stream

@DslMarker
annotation class ComplianceDsl

fun suite(suiteName: String, init: ComplianceSuiteInit.() -> Unit): ComplianceSuite {
    return ComplianceSuiteInit(suiteName).apply(init).buildSuite()
}

class ComplianceSuite(
    val name: String,
    private val init: ComplianceSuiteInit
) {
    fun buildRoutes(): List<HttpRoute> {
        return init.buildRoutes(emptyList())
    }

    fun buildTests(serverInfo: ServerInfo): Stream<DynamicNode> {
        return init.buildTests(serverInfo)
    }
}

class ComplianceSuiteInit(private val suiteName: String) : TestGroupInit(suiteName, null) {
    override val pathUri: URI = URI.create("compliance-dsl://$pathName")
    override val pathContext: List<String> = emptyList()

    override fun buildTests(serverInfo: ServerInfo): Stream<DynamicNode> {
        return super.getChildTests(serverInfo)
    }

    internal fun buildSuite(): ComplianceSuite {
        return ComplianceSuite(suiteName, this)
    }
}

@ComplianceDsl
sealed class DynamicNodeBuilder(
    internal val name: String,
    internal val parent: DynamicNodeBuilder?
) {
    internal val pathName: String = name.pathSafe
    internal open val pathContext: List<String>
        get() = parent?.pathContext?.plus(pathName) ?: listOf(pathName)

    internal open val pathUri: URI
        get() = URI.create(parent!!.pathUri.toASCIIString() + "/" + this.pathName)

    protected val routeInit: RoutingInit = RoutingInit(emptyList())

    fun givenRoutes(init: RoutingInit.() -> Unit) {
        routeInit.apply(init)
    }

    internal abstract fun buildRoutes(
        extraRoutes: List<RoutingInit>
    ): List<HttpRoute>

    internal abstract fun buildTests(
        serverInfo: ServerInfo
    ): Stream<DynamicNode>
}

open class TestGroupInit(
    name: String,
    parent: DynamicNodeBuilder?
) : DynamicNodeBuilder(name, parent) {

    fun describe(name: String, init: TestGroupInit.() -> Unit) {
        children += TestGroupInit(name, this).apply(init)
    }

    fun it(name: String, init: TestInit.() -> Unit) {
        children += TestInit(name, this).apply(init)
    }

    override fun buildRoutes(
        extraRoutes: List<RoutingInit>
    ): List<HttpRoute> {
        return children.flatMap { it.buildRoutes(extraRoutes + this.routeInit) }
    }

    override fun buildTests(
        serverInfo: ServerInfo
    ): Stream<DynamicNode> {
        val uri = URI.create(pathUri.toASCIIString() + "/root")
        println(uri)
        return Stream.of(
            DynamicContainer.dynamicContainer(
                name,
                uri,
                getChildTests(serverInfo)
            )
        )
    }

    protected fun getChildTests(serverInfo: ServerInfo): Stream<DynamicNode> {
        return children.stream().flatMap { it.buildTests(serverInfo) }
    }

    private val children = mutableListOf<DynamicNodeBuilder>()

}

typealias WhenCalledWith = RequestFactory.Convenience.() -> Request

class TestInit(name: String, parent: TestGroupInit) : DynamicNodeBuilder(name, parent) {

    fun whenCalledWith(init: WhenCalledWith) {
        requestInit = init
    }

    fun then(init: Response.() -> Unit) {
        asserts = init
    }

    private lateinit var requestInit: WhenCalledWith

    private lateinit var asserts: Response.() -> Unit

    override fun buildRoutes(extraRoutes: List<RoutingInit>): List<HttpRoute> {
        val basePath = pathContext.map(::staticPart)
        return (extraRoutes + routeInit).flatMap { it.buildRoutes(basePath) }
    }

    override fun buildTests(
        serverInfo: ServerInfo
    ): Stream<DynamicNode> {
        //make sure everything's initialized
        requestInit
        asserts

        val path = pathContext.joinToString("/", prefix = "/")
        val url = serverInfo.url + path

        println(pathUri.toString() + "\n")
        return Stream.of(DynamicTest.dynamicTest(name, pathUri) {
            val request = FuelManager().apply {
                basePath = url
                addRequestInterceptor { LogRequestInterceptor(it) }
                addResponseInterceptor { LogResponseInterceptor(it) }
            }.run(requestInit)

            Assumptions.assumeFalse(
                request.method == Method.PATCH,
                "Fuel doesn't support PATCH, so we'll skip it until we can work around it."
            )

            val (_, response) = request.response()
            response.apply(asserts)
        })
    }
}


@ComplianceDsl
class RoutingInit(
    private val pathParts: RoutePath
) {
    private val routes = mutableListOf<RouteBuilding>()

    fun path(vararg parts: String, init: RoutingInit.() -> Unit) {
        path(parts.map { staticPart(it) }, init)
    }

    fun pathParam(name: String, init: RoutingInit.() -> Unit) {
        path(listOf(SingleVariablePathPart(name)), init)
    }

    fun pathParam(vararg names: String, init: RoutingInit.() -> Unit) {
        path(listOf(CompoundVariablePathPart(names.toList())), init)
    }

    fun path(parts: RoutePath, init: RoutingInit.() -> Unit) {
        routes += RoutingInit(this.pathParts + parts).apply(init).routes
    }

    fun get(consumes: String? = null, produces: String? = null, handler: TestHttpHandler) {
        route(HttpMethod.Routable.GET, consumes, produces, handler)
    }

    fun post(consumes: String? = null, produces: String? = null, handler: TestHttpHandler) {
        route(HttpMethod.Routable.POST, consumes, produces, handler)
    }

    fun put(consumes: String? = null, produces: String? = null, handler: TestHttpHandler) {
        route(HttpMethod.Routable.PUT, consumes, produces, handler)
    }

    fun patch(consumes: String? = null, produces: String? = null, handler: TestHttpHandler) {
        route(HttpMethod.Routable.PATCH, consumes, produces, handler)
    }

    fun delete(consumes: String? = null, produces: String? = null, handler: TestHttpHandler) {
        route(HttpMethod.Routable.DELETE, consumes, produces, handler)
    }

    internal fun route(
        method: HttpMethod.Routable,
        consumes: String? = null,
        produces: String? = null,
        handler: TestHttpHandler
    ) {
        routes += RouteBuilding(
            path = pathParts,
            method = method,
            produces = produces,
            consumes = consumes,
            handler = handler
        )
    }

    private class RouteBuilding(
        val path: RoutePath,
        val method: HttpMethod.Routable,
        val consumes: String? = null,
        val produces: String? = null,
        val handler: TestHttpHandler
    )

    internal fun buildRoutes(basePath: List<StaticPathPart>): List<HttpRoute> {
        val basePathString = basePath.joinToString("/", prefix = "/") { it.part }
        return routes.map {
            HttpRoute(
                pathParts = basePath + it.path,
                method = it.method,
                consumes = it.consumes,
                produces = it.produces,
                handler = TestHandlerWrapper(basePathString, it.handler)
            )
        }
    }
}

typealias TestHttpHandler = suspend HttpRequest.() -> TestResponse

private class TestHandlerWrapper(
    val basePath: String,
    val handler: TestHttpHandler
) : HttpHandler {
    override suspend fun handle(request: HttpRequest): HttpResponse {
        val tr = TestRequestWrapper(request)
        val req = ActualRequest(tr, basePath)
        val resp = handler(tr)
        return TestResponseWrapper(resp, req)
    }
}

private class TestRequestWrapper(val request: HttpRequest) : HttpRequest by request {
    //    override val path: String = request.path.removePrefix(basePath)
    private lateinit var body: Pair<String?, ByteArray?>

    override suspend fun <T> consumeBody(consumer: BodyConsumer<T>): T? {
        val (contentType, bytes) = if (this::body.isInitialized) {
            this.body
        } else {
            request.consumeBody { contentType, stream -> contentType to stream.readBytes() }
                ?: null to null
        }
        return if (contentType != null && bytes != null) {
            consumer(contentType, bytes.inputStream())
        } else {
            null
        }
    }
}

private class TestResponseWrapper(val response: HttpResponse, receivedRequest: ActualRequest) :
    HttpResponse by response {
    override val headers: Map<String, String> =
        response.headers + Pair(
            ActualRequest.headerName,
            jackson.writeValueAsString(receivedRequest).encodeBase64UrlToString()
        )
}

internal val String.pathSafe: String
    get() = this.toLowerCase().replace("""[^-_0-9a-z]+""".toRegex(), "_")


