package edu.byu.uapidsl.types

interface UAPIResponse<MetaType : ResponseMetadata> {
    val metadata: MetaType
    val links: UAPILinks
}

data class UAPIListResponse<MetaType : CollectionMetadata>(
    val values: List<UAPIResponse<*>>,
    override val metadata: MetaType,
    override val links: UAPILinks = emptyMap()
) : UAPIResponse<MetaType>

interface UAPIResource : UAPIResponse<UAPIResourceMeta>

interface UAPIResourceResponse : UAPIResource {
    val basic: UAPIResponse<*>?
    val fieldsets: Map<String, UAPIResponse<*>>
}

data class ErrorResponse(
    override val metadata: UAPIErrorMetadata
) : UAPIResponse<UAPIErrorMetadata> {
    override val links: UAPILinks = emptyMap()

    companion object {
        fun notAuthorized() = ErrorResponse(
            metadata = UAPIErrorMetadata(
                validationResponse = ValidationResponse(
                    403,
                    "Not Authorized"
                ),
                validationInformation = listOf(
                    "The caller is not authorized to perform the requested action"
                )
            )
        )

        fun notFound() = ErrorResponse(
            UAPIErrorMetadata(
                ValidationResponse(404, "Not Found"),
                emptyList()
            )
        )
    }
}

data class SimpleResourceResponse(
    override val fieldsets: Map<String, UAPIResponse<*>>,
    override val metadata: UAPIResourceMeta,
    override val links: UAPILinks = emptyMap()
) : UAPIResourceResponse {
    override val basic: UAPIResponse<*>? = fieldsets["basic"]
}

abstract class UAPIResourceResponseBase(
    override val metadata: UAPIResourceMeta,
    override val links: UAPILinks
) : UAPIResponse<UAPIResourceMeta>

data class UAPISimpleResource(
    override val metadata: UAPIResourceMeta,
    override val links: UAPILinks = emptyMap(),
    val properties: Any
) : UAPIResource

data class UAPIMapResource(
    override val metadata: UAPIResourceMeta,
    override val links: UAPILinks = emptyMap(),
    val properties: Map<String, UAPIField<*>>
) : UAPIResource

object UAPIEmptyResponse : UAPIResponse<UAPIResourceMeta> {
    override val metadata: UAPIResourceMeta = UAPIResourceMeta()
    override val links: UAPILinks = emptyMap()
}

class UAPINotFoundException(): RuntimeException("Object not found")


