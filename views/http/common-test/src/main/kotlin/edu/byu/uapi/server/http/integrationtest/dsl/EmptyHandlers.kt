package edu.byu.uapi.server.http.integrationtest.dsl

import edu.byu.uapi.server.http.HttpMethod
import edu.byu.uapi.server.http.integrationtest.TestResponse

fun RoutingInit.emptyRoute(method: HttpMethod.Routable, consumes: String? = null) {
    this.route(method, consumes = consumes) {
        TestResponse.Empty(204)
    }
}

fun RoutingInit.emptyGet(consumes: String? = null) {
    emptyRoute(HttpMethod.GET, consumes)
}

fun RoutingInit.emptyPost(consumes: String? = null) {
    emptyRoute(HttpMethod.POST, consumes)
}

fun RoutingInit.emptyPut(consumes: String? = null) {
    emptyRoute(HttpMethod.PUT, consumes)
}

fun RoutingInit.emptyPatch(consumes: String? = null) {
    emptyRoute(HttpMethod.PATCH, consumes)
}

fun RoutingInit.emptyDelete(consumes: String? = null) {
    emptyRoute(HttpMethod.DELETE, consumes)
}