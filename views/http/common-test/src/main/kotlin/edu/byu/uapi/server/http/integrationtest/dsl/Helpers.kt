package edu.byu.uapi.server.http.integrationtest.dsl

import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.RequestFactory
import edu.byu.uapi.server.http.HttpMethod

fun TestGroupInit.describeAllMethods(
    name: String,
    block: TestInit.(method: HttpMethod.Routable) -> Unit
) {
    this.describe(name) {
        HttpMethod.Routable.values().forEach { testMethod ->
            it(testMethod.name) {
                block(testMethod)
            }
        }
    }
}

fun RequestFactory.request(method: HttpMethod, path: String, parameters: Parameters? = null): Request {
    return request(method.fuel, path, parameters)
}

val HttpMethod.fuel
    get() = when (this) {
        HttpMethod.Routable.GET    -> Method.GET
        HttpMethod.Routable.PUT    -> Method.PUT
        HttpMethod.Routable.PATCH  -> Method.PATCH
        HttpMethod.Routable.POST   -> Method.POST
        HttpMethod.Routable.DELETE -> Method.DELETE
        HttpMethod.HEAD            -> Method.HEAD
        HttpMethod.OPTIONS         -> Method.OPTIONS
        HttpMethod.TRACE           -> Method.TRACE
        else                       -> throw IllegalStateException("unconvertible method: $this")
    }