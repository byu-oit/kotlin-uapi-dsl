package edu.byu.uapi.server.resources.list

import edu.byu.uapi.model.UAPISortOrder
import edu.byu.uapi.server.inputs.create
import edu.byu.uapi.server.resources.Resource
import edu.byu.uapi.server.resources.ResourceRequestContext
import edu.byu.uapi.server.response.ResponseField
import edu.byu.uapi.server.response.UAPIResponseInit
import edu.byu.uapi.server.response.uapiResponse
import edu.byu.uapi.server.scalars.EnumScalarType
import edu.byu.uapi.server.spi.*
import edu.byu.uapi.server.spi.reflective.ReflectiveFilterParamReader
import edu.byu.uapi.server.spi.reflective.ReflectiveIdParamReader
import edu.byu.uapi.server.types.CreateResult
import edu.byu.uapi.server.types.DeleteResult
import edu.byu.uapi.server.types.IdentifiedModel
import edu.byu.uapi.server.types.UpdateResult
import edu.byu.uapi.server.util.extrapolateGenericType
import edu.byu.uapi.spi.UAPITypeError
import edu.byu.uapi.spi.dictionary.TypeDictionary
import edu.byu.uapi.spi.input.*
import edu.byu.uapi.spi.validation.ValidationEngine
import edu.byu.uapi.spi.validation.Validator
import edu.byu.uapi.utility.takeIfType
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import edu.byu.uapi.server.subresources.list.ListSubresource as IListSubresource
import edu.byu.uapi.server.subresources.singleton.SingletonSubresource as ISingletonSubresource

interface ListResource<UserContext : Any, Id : Any, Model : Any, Params : ListParams>
    : Resource<UserContext, Model, IdentifiedModel<Id, Model>> {

    val pluralName: String

    val singleName: String
        get() = defaultSingleName()

    val idType: KClass<Id>
        get() = this.extrapolateGenericType("Id", ListResource<*, *, *, *>::idType)

    val listParamsType: KClass<Params>
        get() = this.extrapolateGenericType("Params", ListResource<*, *, *, *>::listParamsType)

    val scalarIdParamName: String
        get() = this.singleName + "_id"

    fun loadModel(
        requestContext: ResourceRequestContext,
        userContext: UserContext,
        id: Id
    ): Model?

    fun canUserViewModel(
        requestContext: ResourceRequestContext,
        userContext: UserContext,
        id: Id,
        model: Model
    ): Boolean

    fun idFromModel(model: Model): Id

    @Throws(UAPITypeError::class)
    fun getIdReader(
        dictionary: TypeDictionary
    ): IdParamReader<Id> {
        return defaultIdReader(dictionary, scalarIdParamName)
    }

    fun list(
        requestContext: ResourceRequestContext,
        userContext: UserContext,
        params: Params
    ): List<Model>

    @Throws(UAPITypeError::class)
    fun getListParamReader(dictionary: TypeDictionary): ListParamReader<Params> {
        return defaultGetListParamReader(dictionary)
    }

    // TODO: Maybe it would be better to have a map of types to properties definitions/renderers? That'll especially help with trees of objects.
    //   Then again, it's not THAT hard to share around a list representing the properties for a given type...
    val responseFields: List<ResponseField<UserContext, Model, *>>

    val createOperation: Creatable<UserContext, Id, Model, *>?
        get() = this.takeIfType()

    val updateOperation: Updatable<UserContext, Id, Model, *>?
        get() = this.takeIfType()

    val deleteOperation: Deletable<UserContext, Id, Model>?
        get() = this.takeIfType()

    override val claims: Resource.HasClaims<UserContext, Id, Model, IdentifiedModel<Id, Model>>?
        get() = this.takeIfType()

    interface Creatable<UserContext : Any, Id : Any, Model : Any, Input : Any> {
        fun canUserCreate(
            requestContext: ResourceRequestContext,
            userContext: UserContext
        ): Boolean

        fun getCreateValidator(validationEngine: ValidationEngine): Validator<Input> {
            return validationEngine.validatorFor(createInput)
        }

        fun handleCreate(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            input: Input
        ): CreateResult<Model>

        val createInput: KClass<Input>
            get() = extrapolateGenericType("Input", Creatable<*, *, *, *>::createInput)
    }

    interface Deletable<UserContext : Any, Id : Any, Model : Any> {
        fun canUserDelete(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            id: Id,
            model: Model
        ): Boolean

        fun canBeDeleted(
            id: Id,
            model: Model
        ): Boolean

        fun handleDelete(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            id: Id,
            model: Model
        ): DeleteResult
    }

    interface Updatable<UserContext : Any, Id : Any, Model : Any, Input : Any> {
        fun canUserUpdate(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            id: Id,
            model: Model
        ): Boolean

        fun canBeUpdated(
            id: Id,
            model: Model
        ): Boolean

        fun getUpdateValidator(validationEngine: ValidationEngine): Validator<Input> {
            return validationEngine.validatorFor(updateInput)
        }

        fun handleUpdate(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            id: Id,
            model: Model,
            input: Input
        ): UpdateResult<Model>

        val updateInput: KClass<Input>
            get() = extrapolateGenericType("Input", Updatable<*, *, *, *>::updateInput)
    }

    interface CreatableWithId<UserContext : Any, Id : Any, Model : Any, Input : Any> : Updatable<UserContext, Id, Model, Input> {
        fun canUserCreateWithId(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            id: Id
        ): Boolean

        fun handleCreateWithId(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            id: Id,
            input: Input
        ): CreateResult<Model>
    }

    interface Simple<UserContext : Any, Id : Any, Model : Any> : ListResource<UserContext, Id, Model, ListParams.Empty> {
        override val listParamsType: KClass<ListParams.Empty>
            get() = ListParams.Empty::class

        override fun getListParamReader(dictionary: TypeDictionary): ListParamReader<ListParams.Empty> {
            return EmptyListParamReader
        }
    }

    interface ListWithSubset<UserContext : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithSubset> :
        ListResource<UserContext, Id, Model, CollectionParams> {

        override fun list(
            requestContext: ResourceRequestContext,
            userContext: UserContext,
            params: CollectionParams
        ): ListWithTotal<Model>

        val listDefaultSubsetSize: Int
        val listMaxSubsetSize: Int
    }

    interface ListWithSearch<UserContext : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithSearch<SearchContext>, SearchContext : Enum<SearchContext>>
        : ListResource<UserContext, Id, Model, CollectionParams> {

        fun listSearchContexts(value: SearchContext): Collection<String>

        @Throws(UAPITypeError::class)
        fun getListSearchParamReader(dictionary: TypeDictionary): SearchParamsReader<SearchContext> {
            return defaultListSearchParamReader(dictionary)
        }

        @Throws(UAPITypeError::class)
        fun getListSearchContextType(dictionary: TypeDictionary): EnumScalarType<SearchContext> {
            return DefaultParameterStyleEnumScalar(
                this.extrapolateGenericType("SearchContext", ListWithSearch<*, *, *, *, *>::getListSearchContextType)
            )
        }
    }

    interface ListWithSort<UserContext : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithSort<SortProperty>, SortProperty : Enum<SortProperty>>
        : ListResource<UserContext, Id, Model, CollectionParams> {

        val listDefaultSortProperties: List<SortProperty>
        val listDefaultSortOrder: UAPISortOrder

        @Throws(UAPITypeError::class)
        fun getListSortPropertyType(typeDictionary: TypeDictionary): EnumScalarType<SortProperty> {
            return DefaultParameterStyleEnumScalar(
                this.extrapolateGenericType("SortProperty", ListWithSort<*, *, *, *, *>::getListSortPropertyType)
            )
        }

        @Throws(UAPITypeError::class)
        fun getListSortParamsReader(typeDictionary: TypeDictionary): SortParamsReader<SortProperty> {
            return defaultListSortParamsReader(typeDictionary)
        }
    }

    interface ListWithFilters<UserContext : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithFilters<Filters>, Filters : Any>
        : ListResource<UserContext, Id, Model, CollectionParams> {

        val listFilterParamType: KClass<Filters>
            get() = this.extrapolateGenericType("Filters", ListWithFilters<*, *, *, *, *>::listFilterParamType)

        @Throws(UAPITypeError::class)
        fun getListFilterParamReader(typeDictionary: TypeDictionary): FilterParamsReader<Filters> {
            return ReflectiveFilterParamReader.create(listFilterParamType, typeDictionary)
        }
    }


    // Subresource interface aliases - makes subresources a little nicer!
    interface SingletonSubresource<UserContext : Any, ParentId : Any, ParentModel : Any, Model : Any> :
        ISingletonSubresource<UserContext, IdentifiedModel<ParentId, ParentModel>, Model> {
        interface Updatable<UserContext : Any, ParentId : Any, ParentModel : Any, Model : Any, Input : Any> :
            ISingletonSubresource.Updatable<UserContext, IdentifiedModel<ParentId, ParentModel>, Model, Input>

        interface Creatable<UserContext : Any, ParentId : Any, ParentModel : Any, Model : Any, Input : Any> :
            ISingletonSubresource.Creatable<UserContext, IdentifiedModel<ParentId, ParentModel>, Model, Input>

        interface Deletable<UserContext : Any, ParentId : Any, ParentModel : Any, Model : Any> : ISingletonSubresource.Deletable<UserContext, IdentifiedModel<ParentId, ParentModel>, Model>
    }

    interface ListSubresource<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, Params : ListParams> :
        IListSubresource<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, Params> {
        interface Creatable<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, Input : Any> :
            IListSubresource.Creatable<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, Input>

        interface Updatable<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, Input : Any> :
            IListSubresource.Updatable<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, Input>

        interface CreatableWithId<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, Input : Any> :
            IListSubresource.CreatableWithId<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, Input>

        interface Deletable<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any> :
            IListSubresource.Deletable<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model>

        interface Simple<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any> : IListSubresource.Simple<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model>

        interface ListWithSubset<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithSubset> :
            IListSubresource.ListWithSubset<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, CollectionParams>

        interface ListWithSearch<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithSearch<SearchContext>, SearchContext : Enum<SearchContext>>
            : IListSubresource.ListWithSearch<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, CollectionParams, SearchContext>

        interface ListWithSort<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithSort<SortProperty>, SortProperty : Enum<SortProperty>>
            : IListSubresource.ListWithSort<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, CollectionParams, SortProperty>

        interface ListWithFilters<UserContext : Any, ParentId : Any, ParentModel : Any, Id : Any, Model : Any, CollectionParams : ListParams.WithFilters<Filters>, Filters : Any>
            : IListSubresource.ListWithFilters<UserContext, IdentifiedModel<ParentId, ParentModel>, Id, Model, CollectionParams, Filters>
    }

    interface HasClaims<UserContext : Any, Id : Any, Model : Any>
        : Resource.HasClaims<UserContext, Id, Model, IdentifiedModel<Id, Model>>
}

private fun ListResource<*, *, *, *>.defaultSingleName(): String {
    if (pluralName.endsWith("s")) {
        return pluralName.dropLast(1)
    }
    return pluralName
}

private fun <Id : Any, Model : Any, UserContext : Any> ListResource<UserContext, Id, Model, *>.defaultIdReader(
    dictionary: TypeDictionary,
    scalarIdParamName: String
): IdParamReader<Id> {
    val prefix = this.singleName + "_"
    val idType = this.idType
    if (dictionary.isScalarType(idType)) {
        return ScalarTypeIdParamReader(scalarIdParamName, dictionary.requireScalarType(idType))
    }
    return ReflectiveIdParamReader.create(prefix, idType, dictionary)
}

@Throws(UAPITypeError::class)
internal fun <Params : ListParams>
    ListResource<*, *, *, Params>.defaultGetListParamReader(
    dictionary: TypeDictionary
): ListParamReader<Params> {
    if (listParamsType == ListParams.Empty::class) {
        @Suppress("UNCHECKED_CAST")
        return EmptyListParamReader as ListParamReader<Params>
    }
    if (!listParamsType.isData) {
        throw UAPITypeError.create(listParamsType, "List parameter type must be a data class.")
    }
    val ctor = listParamsType.primaryConstructor
        ?: throw UAPITypeError.create(listParamsType, "List parameter type must have a primary constructor.")

    val search = this.takeIfType<ListResource.ListWithSearch<*, *, *, *, *>>()?.getListSearchParamReader(dictionary)
    val filter = this.takeIfType<ListResource.ListWithFilters<*, *, *, *, *>>()?.getListFilterParamReader(dictionary)
    val sort = this.takeIfType<ListResource.ListWithSort<*, *, *, *, *>>()?.getListSortParamsReader(dictionary)
    val subset = this.takeIfType<ListResource.ListWithSubset<*, *, *, *>>()?.let { it ->
        SubsetParamsReader(
            defaultSize = it.listDefaultSubsetSize,
            maxSize = it.listMaxSubsetSize
        )
    }

    return DefaultListParamReader.create(
        paramsType = listParamsType,
        search = search,
        filter = filter,
        sort = sort,
        subset = subset,
        constructor = ctor
    )
}

@Throws(UAPITypeError::class)
internal fun <SearchContext : Enum<SearchContext>>
    ListResource.ListWithSearch<*, *, *, *, SearchContext>.defaultListSearchParamReader(
    dictionary: TypeDictionary
): SearchParamsReader<SearchContext> {
    val contextType = getListSearchContextType(dictionary)
    val contextClass = contextType.type
    val searchContexts = contextType.enumConstants.map { it to listSearchContexts(it) }.toMap()

    if (searchContexts.any { it.value.isEmpty() }) {
        throw UAPITypeError.create(
            contextType.type,
            "${this::class.simpleName}.listSearchContexts must return a non-empty set for every value of ${contextClass.simpleName}"
        )
    }

    return DefaultSearchParamsReader.create(
        contextType, searchContexts
    )
}

@Throws(UAPITypeError::class)
internal fun <SortProperty : Enum<SortProperty>>
    ListResource.ListWithSort<*, *, *, *, SortProperty>.defaultListSortParamsReader(
    typeDictionary: TypeDictionary
): SortParamsReader<SortProperty> {
    val sortPropType = getListSortPropertyType(typeDictionary)

    return DefaultSortParamsReader.create(
        sortPropType,
        EnumScalarType(UAPISortOrder::class),
        listDefaultSortProperties,
        listDefaultSortOrder
    )
}

inline fun <UserContext : Any, Model : Any> ListResource<UserContext, *, Model, *>.fields(fn: UAPIResponseInit<UserContext, Model>.() -> Unit)
    : List<ResponseField<UserContext, Model, *>> = uapiResponse(fn)
