package edu.byu.uapidsl.http

import com.fasterxml.jackson.databind.ObjectWriter
import edu.byu.uapidsl.UApiModel
import edu.byu.uapidsl.http.implementation.*
import edu.byu.uapidsl.http.path.CompoundPathVariablePart
import edu.byu.uapidsl.http.path.PathPart
import edu.byu.uapidsl.http.path.SimplePathVariablePart
import edu.byu.uapidsl.http.path.StaticPathPart
import edu.byu.uapidsl.model.resource.identified.IdentifiedResource
import edu.byu.uapidsl.model.resource.identified.ops.*
import edu.byu.uapidsl.model.resource.ops.*
import edu.byu.uapidsl.typemodeling.ComplexPathParamSchema
import edu.byu.uapidsl.typemodeling.PathParamSchema
import edu.byu.uapidsl.typemodeling.SimplePathParamSchema

data class HttpPath(
    val pathParts: List<PathPart>,
    val handlers: MethodHandlers
)


data class MethodHandlers(
    val options: OptionsHandler,
    val get: GetHandler? = null,
    val post: PostHandler? = null,
    val put: PutHandler? = null,
    val patch: PatchHandler? = null,
    val delete: DeleteHandler? = null
)

val <AuthContext : Any> UApiModel<AuthContext>.httpPaths: List<HttpPath>
    get() {
        return this.resources.flatMap { pathsFor(this, it) }
    }

private fun <AuthContext : Any> pathsFor(apiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, *, *>): Iterable<HttpPath> {

    val basePath: List<PathPart> = listOf(StaticPathPart(resource.name))

    // TODO("Actually, you know, handleCreate other types of IDs")
    val idParamSchema = resource.idModel.schema

    val resourcePath = basePath + idParamSchema.asPathPart()

    val collection = collectionHandlers(apiModel, resource)

    val single = singleHandlers(apiModel, resource)

    val result = mutableListOf(
        HttpPath(resourcePath, single)
    )

    if (collection != null) {
        result += HttpPath(basePath, collection)
    }

    return result
}

private fun PathParamSchema<*>.asPathPart(): PathPart {
    return when (this) {
        is SimplePathParamSchema -> SimplePathVariablePart(this.name)
        is ComplexPathParamSchema -> CompoundPathVariablePart(this.properties.map { it.name })
    }
}

private fun <AuthContext : Any, IdType : Any, ModelType : Any> collectionHandlers(uapiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, IdType, ModelType>): MethodHandlers? {
    val ops = resource.operations
    if (ops.list == null && ops.create == null) {
        return null
    }

    val create = ops.create

    val get = ops.list?.toHandler(uapiModel, resource, resource.responseModel.writer)
    val post = if (create != null) {
        SimplePost(uapiModel, resource, create, resource.responseModel.writer)
    } else null

    return MethodHandlers(
        options = AuthorizationAwareOptions(),
        get = get,
        post = post
    )
}

private fun <AuthContext : Any, IdType : Any, ModelType : Any> singleHandlers(uapiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, IdType, ModelType>): MethodHandlers {
    val writer = resource.responseModel.writer
    val ops = resource.operations
    val get = ResourceGet(uapiModel, resource, resource.responseModel.writer)

    val put: PutHandler? = ops.update?.toHandler(uapiModel, resource, writer)

    val delete = ops.delete?.toHandler(uapiModel, resource, writer)

    val options = AuthorizationAwareOptions()

    return MethodHandlers(
        options = options,
        get = get,
        put = put,
        delete = delete
    )
}

private fun <AuthContext : Any, IdType : Any, ModelType : Any> DeleteOperation<AuthContext, IdType, ModelType>.toHandler(
    uapiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, IdType, ModelType>, writer: ObjectWriter
): DeleteHandler {
    return SimpleDelete(uapiModel, resource, writer)
}

private fun <AuthContext : Any, IdType : Any, ModelType : Any, InputType : Any> UpdateOperation<AuthContext, IdType, ModelType, InputType, *>.toHandler(
    uapiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, IdType, ModelType>, writer: ObjectWriter
): PutHandler {
    return ResourcePut(uapiModel, resource, this, writer)
}

//
//private fun <AuthContext : Any, IdType : Any, ModelType : Any, Filters : Any, RequestContext: ListContext<AuthContext, Filters>, IdCollection: Collection<IdType>, ModelCollection: Collection<ModelType>>
//    ListOperation<AuthContext, IdType, ModelType, Filters, RequestContext, IdCollection, ModelCollection>.toHandler(
//    uapiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, IdType, ModelType>, writer: ObjectWriter
//): GetHandler {
//    return when (this) {
//        is SimpleListOperation<AuthContext, IdType, ModelType, Filters> -> SimpleListGet(uapiModel, resource, this, writer)
//        is PagedListOperation<AuthContext, IdType, ModelType, Filters> -> PagedListGet(uapiModel, resource, this, writer)
//    }
//}

private fun <AuthContext : Any, IdType : Any, ModelType : Any, Filters : Any>
    ListOperation<AuthContext, IdType, ModelType, Filters, *, *, *>.toHandler(
    uapiModel: UApiModel<AuthContext>, resource: IdentifiedResource<AuthContext, IdType, ModelType>, writer: ObjectWriter
): GetHandler {
    return when(this) {
        is SimpleListOperation -> SimpleListGet(uapiModel, resource, this, writer)
        is PagedListOperation -> PagedListGet(uapiModel, resource, this, writer)
    }
}

typealias PathParamDecorator = (part: String) -> String

object PathParamDecorators {
    val COLON: PathParamDecorator = { ":$it" }
    val CURLY_BRACE: PathParamDecorator = { "{$it}" }
    val NONE: PathParamDecorator = { it }
}

fun stringifyPaths(pathParts: List<PathPart>, paramDecorator: PathParamDecorator): String {
    return pathParts.joinToString(separator = "/", prefix = "/") { part ->
        when (part) {
            is StaticPathPart -> part.part
            is SimplePathVariablePart -> paramDecorator(part.name)
            is CompoundPathVariablePart -> part.names.joinToString(separator = ",", transform = paramDecorator)
        }
    }
}


