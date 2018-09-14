package edu.byu.uapidsl.dsl.subresource.single

import edu.byu.uapidsl.DslPart
import edu.byu.uapidsl.ModelingContext

class SingleSubModelDSL<AuthContext, ParentId, ParentModel, SingleSubModel>(
) : DslPart<Nothing>() {
    override fun toModel(context: ModelingContext): Nothing {
        TODO("not implemented")
    }

    fun <UAPIType> transform(handler: SingleSubTransformer<AuthContext, ParentId, ParentModel, SingleSubModel, UAPIType>) {

    }

    inline fun <RelatedId, reified RelatedModel> relation(
        name: String,
        init: SingleSubRelationDSL<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId, RelatedModel>.() -> Unit
    ) {
    }

    inline fun externalRelation(
        name: String,
        init: SingleSubExternalRelationDSL<AuthContext, ParentId, ParentModel, SingleSubModel>.() -> Unit
    ) {

    }

}

class SingleSubRelationDSL<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId, RelatedModel>(
) : DslPart<Nothing>() {
    override fun toModel(context: ModelingContext): Nothing {
        TODO("not implemented")
    }

    fun authorization(authorizer: SingleSubRelationAuthorizer<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId, RelatedModel>) {

    }

    fun handle(handler: SingleSubRelationHandler<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId>) {

    }
}

class SingleSubExternalRelationDSL<AuthContext, ParentId, ParentModel, SingleSubModel>(
) : DslPart<Nothing>() {
    override fun toModel(context: ModelingContext): Nothing {
        TODO("not implemented")
    }

    fun authorization(authorizer: SingleSubExternalRelationAuthorizer<AuthContext, ParentId, ParentModel, SingleSubModel>) {

    }

    fun handle(handler: SingleSubExternalRelationHandler<AuthContext, ParentId, ParentModel, SingleSubModel>) {

    }
}

interface SingleSubExternalRelationContext<AuthContext, ParentId, ParentModel, SingleSubModel> {
    val authContext: AuthContext
    val parentId: ParentId
    val parent: ParentModel
    val SingleSubresource: SingleSubModel
}

interface SingleSubRelationLoadingContext<AuthContext, ParentId, ParentModel, SingleSubModel> {
    val authContext: AuthContext
    val parentId: ParentId
    val parent: ParentModel
    val SingleSubresource: SingleSubModel
}

interface SingleSubRelationAuthorizationContext<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId, RelatedType> {
    val authContext: AuthContext
    val parentId: ParentId
    val parent: ParentModel
    val SingleSubresource: SingleSubModel
    val relatedId: RelatedId
    val relatedType: RelatedType
}

interface SingleSubCustomizeFieldsContext<AuthContext, ParentId, ParentModel, SingleSubModel> {
    val authContext: AuthContext
    val parentId: ParentId
    val parent: ParentModel
    val resource: SingleSubModel
}

typealias SingleSubExternalRelationAuthorizer<AuthContext, ParentId, ParentModel, SingleSubModel> =
    SingleSubExternalRelationContext<AuthContext, ParentId, ParentModel, SingleSubModel>.() -> Boolean

typealias SingleSubExternalRelationHandler<AuthContext, ParentId, ParentModel, SingleSubModel> =
    SingleSubExternalRelationContext<AuthContext, ParentId, ParentModel, SingleSubModel>.() -> String?

typealias SingleSubRelationHandler<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId> =
    SingleSubRelationLoadingContext<AuthContext, ParentId, ParentModel, SingleSubModel>.() -> RelatedId?

typealias SingleSubRelationAuthorizer<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId, RelatedModel> =
    SingleSubRelationAuthorizationContext<AuthContext, ParentId, ParentModel, SingleSubModel, RelatedId, RelatedModel>.() -> Boolean

typealias SingleSubTransformer<AuthContext, ParentId, ParentModel, SingleSubModel, UAPIType> =
    SingleSubCustomizeFieldsContext<AuthContext, ParentId, ParentModel, SingleSubModel>.() -> UAPIType
