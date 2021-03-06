package edu.byu.uapi.http.spark

import com.google.gson.JsonObject
import edu.byu.uapi.http.*
import edu.byu.uapi.http.json.*
import edu.byu.uapi.server.UAPIRuntime
import edu.byu.uapi.server.util.info
import edu.byu.uapi.server.util.loggerFor
import edu.byu.uapi.spi.dictionary.TypeDictionary
import edu.byu.uapi.spi.rendering.Renderer
import spark.*
import java.io.InputStream
import java.io.StringWriter
import java.io.Writer


data class SparkConfig(
    val port: Int = defaultPort,
    override val jsonEngine: JsonEngine<*, *> = defaultJsonEngine,
    val customizer: (Service) -> Unit = defaultCustomizer
) : HttpEngineConfig {
    companion object {
        val defaultPort = 4567
        val defaultJsonEngine: JsonEngine<*, *> = JacksonEngine
        val defaultCustomizer: (Service) -> Unit = {}
    }
}

class SparkHttpEngine(config: SparkConfig) : HttpEngineBase<Service, SparkConfig>(config) {
    companion object {
        internal val LOG = loggerFor<SparkHttpEngine>()
    }

    init {
        super.doInit()
    }

    override fun startServer(config: SparkConfig): Service {
        return Service.ignite().apply {
            port(config.port)
            config.customizer(this)
            after { request, response ->
                response.header("Content-Encoding", "gzip")
            }
            LOG.info("UAPI-HTTP Spark server is starting on port {}", config.port)
        }
    }

    override fun stop(server: Service) {
        server.stop()
        server.awaitStop()
    }

    override fun registerRoutes(
        server: Service,
        config: SparkConfig,
        routes: List<HttpRoute>,
        rootPath: String,
        runtime: UAPIRuntime<*>
    ) {
        server.optionalPath(rootPath) {
            routes.forEach {
                LOG.info { "Adding route ${it.method} $rootPath${it.pathParts.stringifySpark()}" }
                server.addRoute(it.method.toSpark(), it.toSpark(config, runtime.typeDictionary))
            }

            routes.asSequence().map { it.pathParts.stringifySpark() }.distinct().forEach {
                server.before(it) { request, response ->
                    request.attribute("uapi.start", System.currentTimeMillis())
                    LOG.info("Processing request: ${request.requestMethod()} ${request.uri()}")
                }

                server.after(it) { request, response ->
                    val start = request.attribute<Long>("uapi.start")
                    val end = System.currentTimeMillis()
                    LOG.info("Responding with status ${response.status()}. Took ${end - start} ms")
                }
            }
        }
    }

    override fun registerDocRoutes(
        server: Service,
        config: SparkConfig,
        docRoutes: List<DocRoute>,
        rootPath: String,
        runtime: UAPIRuntime<*>
    ) {
        server.optionalPath(rootPath) {
            docRoutes.forEach { dr ->
                val path = dr.path.stringifySpark()
                LOG.info("Adding GET $rootPath$path")
                server.get(path) { req, res ->
                    res.type(dr.source.contentType)
                    dr.source.getInputStream(req.queryParams("pretty")?.toBoolean() ?: false)
                }
            }
        }
    }

    private inline fun Service.optionalPath(
        rootPath: String,
        crossinline group: () -> Unit
    ) {
        if (rootPath.isBlank() || rootPath.trim() == "/") {
            group()
        } else {
            this.path(rootPath) { group() }
        }
    }
}

fun List<PathPart>.stringifySpark(): String {
    return this.stringify(PathParamDecorators.COLON) { part, decorator ->
        decorator.invoke(part.names.joinToString(separator = COMPOUND_PARAM_SEPARATOR, prefix = COMPOUND_PARAM_PREFIX))
    }
}

fun <UserContext : Any> UAPIRuntime<UserContext>.startSpark(
    config: SparkConfig
): SparkHttpEngine {
    return SparkHttpEngine(config).also { it.register(this) }
}

fun <UserContext : Any> UAPIRuntime<UserContext>.startSpark(
    port: Int = SparkConfig.defaultPort,
    jsonEngine: JsonEngine<*, *> = SparkConfig.defaultJsonEngine
): SparkHttpEngine {
    return this.startSpark(SparkConfig(port, jsonEngine))
}

fun <UserContext : Any> UAPIRuntime<UserContext>.startSpark(
    port: Int = SparkConfig.defaultPort,
    jsonEngine: JsonEngine<*, *> = SparkConfig.defaultJsonEngine,
    customize: (Service) -> Unit
): SparkHttpEngine {
    return this.startSpark(SparkConfig(port, jsonEngine, customize))
}

private fun HttpRoute.toSpark(
    config: SparkConfig,
    typeDictionary: TypeDictionary
): RouteImpl {
    val path = this.pathParts.stringifySpark()
    return RouteImpl.create(path, this.handler.toSpark(config, typeDictionary))
}

private fun HttpHandler.toSpark(
    config: SparkConfig,
    typeDictionary: TypeDictionary
): SparkHttpRoute {
    return SparkHttpRoute(this, config, typeDictionary)
}

class SparkHttpRoute(
    val handler: HttpHandler,
    val config: SparkConfig,
    val typeDictionary: TypeDictionary
) : Route {

    companion object {
        val LOG = loggerFor<SparkHttpRoute>()
    }

    override fun handle(
        request: Request,
        response: Response
    ): Any {
        val resp = handler.handle(SparkRequest(request))
        response.type("application/json")
        response.status(resp.status)
        resp.headers.forEach { key, set -> set.forEach { response.header(key, it) } }
        return resp.body.renderResponseBody(config.jsonEngine, typeDictionary)
    }
}

fun ResponseBody.renderResponseBody(
    json: JsonEngine<*, *>,
    typeDictionary: TypeDictionary
): Any {
    return when (json) {
        is GsonTreeEngine        -> {
            val result: JsonObject = this.render(json.renderer(typeDictionary, null))
            result.toString()
        }
        is JavaxJsonTreeEngine   -> {
            val obj = this.render(json.renderer(typeDictionary, null))
            obj.toString()
        }
        is JavaxJsonStreamEngine -> {
            json.renderBuffered(typeDictionary) {
                this.render(it)
            }
        }
        is JacksonEngine         -> {
            json.renderBuffered(typeDictionary) {
                this.render(it)
            }
        }
    }
}

inline fun <Output : Any> JsonEngine<Output, Writer>.renderBuffered(
    typeDictionary: TypeDictionary,
    render: (Renderer<Output>) -> Unit
): InputStream {
    //spark has a bug with closing input streams, (https://github.com/perwendel/spark/pull/978), so we're gonna just buffer in memory for now
//    val file = File.createTempFile("uapi-runtime-render-buffer", ".tmp.json")
//    file.deleteOnExit()
//    val writer = file.bufferedWriter()

    val writer = StringWriter()

    writer.use {
        val renderer = this.renderer(typeDictionary, it)
        render(renderer)
        it.flush()
    }

    return writer.toString().byteInputStream()
//    return file.inputStream().buffered().afterClose { file.delete() }
}

fun InputStream.afterClose(afterClose: () -> Unit): InputStream {
    return CloseActionInputStream(this, afterClose)
}

class CloseActionInputStream(
    val wrapped: InputStream,
    val afterClose: () -> Unit
) : InputStream() {

    override fun skip(n: Long): Long {
        return wrapped.skip(n)
    }

    override fun available(): Int {
        return wrapped.available()
    }

    override fun reset() {
        wrapped.reset()
    }

    override fun close() {
        wrapped.close()
        afterClose()
    }

    override fun mark(readlimit: Int) {
        wrapped.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return wrapped.markSupported()
    }

    override fun read(): Int {
        return wrapped.read()
    }

    override fun read(b: ByteArray?): Int {
        return wrapped.read(b)
    }

    override fun read(
        b: ByteArray?,
        off: Int,
        len: Int
    ): Int {
        return wrapped.read(b, off, len)
    }
}

private fun HttpMethod.toSpark(): spark.route.HttpMethod {
    return when (this) {
        HttpMethod.GET    -> spark.route.HttpMethod.get
        HttpMethod.PUT    -> spark.route.HttpMethod.put
        HttpMethod.PATCH  -> spark.route.HttpMethod.patch
        HttpMethod.POST   -> spark.route.HttpMethod.post
        HttpMethod.DELETE -> spark.route.HttpMethod.delete
    }
}
