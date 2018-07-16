package implementation

import com.fasterxml.jackson.databind.ObjectWriter
import edu.byu.uapidsl.UApiModel
import edu.byu.uapidsl.dsl.ListContext
import edu.byu.uapidsl.dsl.PagingParams
import edu.byu.uapidsl.http.GetHandler
import edu.byu.uapidsl.http.GetRequest
import edu.byu.uapidsl.http.implementation.BaseHttpHandler
import edu.byu.uapidsl.http.implementation.ReadContextImpl
import edu.byu.uapidsl.http.implementation.ReadLoadContextImpl
import edu.byu.uapidsl.model.resource.ListResourceRequest
import edu.byu.uapidsl.model.resource.ops.ListOperation
import edu.byu.uapidsl.model.resource.ResourceModel
import edu.byu.uapidsl.types.*
import either.fold

abstract class BaseListGet<AuthContext : Any, IdType : Any, ModelType : Any, Filters : Any, RequestContext: ListContext<AuthContext, Filters>, IdCollection: Collection<IdType>, ModelCollection: Collection<ModelType>>(
    apiModel: UApiModel<AuthContext>,
    protected val resource: ResourceModel<AuthContext, IdType, ModelType>,
    jsonMapper: ObjectWriter
) : BaseHttpHandler<GetRequest, AuthContext>(
    apiModel, jsonMapper
), GetHandler {

    protected abstract val list: ListOperation<AuthContext, IdType, ModelType, Filters, RequestContext, IdCollection, ModelCollection>

    override fun handleAuthenticated(request: GetRequest, authContext: AuthContext): UAPIResponse<*> {
        val filters = list.filterType.reader.read(request.query)

        val pagingParams: PagingParams? = this.extractPagingParams(request)

        return resource.handleListRequest(ListResourceRequest(
            authContext,
            filters,
            pagingParams
        ))
    }

    protected abstract fun extractPagingParams(request: GetRequest): PagingParams?


}
