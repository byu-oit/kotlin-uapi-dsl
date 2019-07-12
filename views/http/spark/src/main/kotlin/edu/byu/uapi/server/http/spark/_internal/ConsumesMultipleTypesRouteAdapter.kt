package edu.byu.uapi.server.http.spark._internal

import edu.byu.uapi.server.http.HttpHandler
import edu.byu.uapi.server.http.errors.UAPIHttpMissingHeaderError
import edu.byu.uapi.server.http.errors.UAPIHttpUnrecognizedContentTypeError
import edu.byu.uapi.server.http.path.RoutePath
import spark.Request
import spark.utils.MimeParse
import kotlin.coroutines.CoroutineContext

internal class ConsumesMultipleTypesRouteAdapter(
    routePath: RoutePath,
    internal val handlers: Map<String, HttpHandler>,
    context: CoroutineContext
) : BaseSparkRouteAdapter(routePath, context) {
    override fun getHandlerFor(req: Request): HttpHandler {
        val requestType = req.contentType()
            ?: throw UAPIHttpMissingHeaderError("Content-Type")
        val type = MimeParse.bestMatch(handlers.keys, requestType)
        if (type == null || type.isBlank()) {
            throw UAPIHttpUnrecognizedContentTypeError(handlers.keys.toList())
        }
        return handlers.getValue(type)
    }
}