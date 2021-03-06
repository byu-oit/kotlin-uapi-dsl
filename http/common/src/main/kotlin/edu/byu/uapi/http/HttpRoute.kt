package edu.byu.uapi.http


data class HttpRoute(
    val pathParts: List<PathPart>,
    val method: HttpMethod,
    val handler: HttpHandler
)

//class ErrorHttpResponse(val error: HttpError, jsonWriter: ObjectWriter)
//    : UAPIHttpResponse(error.toResponse(), jsonWriter) {
//
//    override val status = error.code
//
//    override val headers: Headers
//        get() = SimpleHeaders(super.headers + mapOf("X-BYUAPI-Error" to setOf(error.message)))
//
//}

//open class UAPIHttpResponse(uapiResponse: UAPIResponse<*>, jsonWriter: ObjectWriter) : HttpResponse {
//
//    private val validationResponse = uapiResponse.metadata.validationResponse
//
//    override val status = validationResponse.code
//
//    override val headers: Headers = SimpleHeaders(
//        "X-BYUAPI-Response-Message" to validationResponse.message,
//        "Content-Type" to "application/json"
//    )
//
//    override val body: ResponseBody = JacksonResponseBody(uapiResponse, jsonWriter)
//
//}
//
//object EmptyUAPIHttpResponse: HttpResponse {
//    override val status: Int = 204
//    override val headers: Headers = EmptyHeaders
//    override val body: ResponseBody = EmptyResponseBody
//}

