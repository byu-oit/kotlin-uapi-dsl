package edu.byu.uapi.server.types

import edu.byu.uapi.model.UAPIClaimRelationship
import edu.byu.uapi.model.UAPIValueConstraints
import edu.byu.uapi.model.UAPIValueType
import edu.byu.uapi.spi.SpecConstants
import edu.byu.uapi.spi.rendering.Renderable
import edu.byu.uapi.spi.rendering.Renderer
import edu.byu.uapi.spi.rendering.render

sealed class UAPIResponse<MetaType : ResponseMetadata> : Renderable {
    abstract val metadata: MetaType
    abstract val links: UAPILinks

    final override fun render(renderer: Renderer<*>) {
        renderExtras(renderer)
        renderer.tree(SpecConstants.Links.KEY, links)
        renderer.tree(SpecConstants.Metadata.KEY, metadata)
    }

    protected open fun renderExtras(renderer: Renderer<*>) {
    }
}

data class UAPIFieldsetsCollectionResponse(
    val values: List<UAPIFieldsetsResponse>,
    override val metadata: CollectionMetadata,
    override val links: UAPILinks
) : UAPIResponse<CollectionMetadata>() {
    override fun renderExtras(renderer: Renderer<*>) {
        renderer.treeArray(SpecConstants.Collections.Response.KEY_VALUES, values)
    }
}

data class UAPISubresourceCollectionResponse(
    val values: List<UAPIPropertiesResponse>,
    override val metadata: CollectionMetadata,
    override val links: UAPILinks
) : UAPIResponse<CollectionMetadata>() {
    override fun renderExtras(renderer: Renderer<*>) {
        renderer.treeArray(SpecConstants.Collections.Response.KEY_VALUES, values)
    }
}

data class UAPIPropertiesResponse(
    val properties: Map<String, UAPIProperty>,
    override val metadata: UAPIResourceMeta,
    override val links: UAPILinks
) : UAPIResponse<UAPIResourceMeta>() {
    override fun renderExtras(renderer: Renderer<*>) {
        properties.render(renderer)
    }
}

data class UAPIFieldsetsResponse(
    val fieldsets: Map<String, UAPIResponse<*>>,
    override val metadata: FieldsetsMetadata,
    override val links: UAPILinks = emptyMap()
) : UAPIResponse<FieldsetsMetadata>() {
    override fun renderExtras(renderer: Renderer<*>) {
        fieldsets.render(renderer)
    }
}

sealed class UAPIErrorResponse : UAPIResponse<UAPIErrorMetadata>() {
    companion object {
        fun notAuthorized() = UAPINotAuthorizedError

        fun notFound() = UAPINotFoundError
    }
}

data class GenericUAPIErrorResponse(
    val statusCode: Int,
    val message: String,
    val validationInformation: List<String> = emptyList()
) : UAPIErrorResponse() {
    override val metadata: UAPIErrorMetadata = UAPIErrorMetadata(
        ValidationResponse(statusCode, message),
        validationInformation
    )
    override val links: UAPILinks = emptyMap()
}

data class UAPIBadRequestError(
    val messages: List<String>
) : UAPIResponse<UAPIErrorMetadata>() {
    constructor(message: String) : this(listOf(message))

    override val metadata: UAPIErrorMetadata = UAPIErrorMetadata(
        validationResponse = ValidationResponse(400, "Bad Request"),
        validationInformation = messages
    )
    override val links: UAPILinks = emptyMap()
}

class UAPINotAuthenticatedError(
    messages: List<String>
) : UAPIErrorResponse() {

    override val metadata: UAPIErrorMetadata = UAPIErrorMetadata(
        ValidationResponse(
            403,
            "NotAuthorized"
        ),
        validationInformation = messages
    )
    override val links: UAPILinks = emptyMap()

}

object UAPINotAuthorizedError : UAPIErrorResponse() {
    override val metadata: UAPIErrorMetadata = UAPIErrorMetadata(
        ValidationResponse(403, "Not Authorized"),
        listOf("The caller is not authorized to perform the requested action")
    )
    override val links: UAPILinks = emptyMap()
}

object UAPINotFoundError : UAPIErrorResponse() {
    override val metadata: UAPIErrorMetadata = UAPIErrorMetadata(
        ValidationResponse(404, "Not Found"),
        listOf("Not Found")
    )
    override val links: UAPILinks = emptyMap()
}

object UAPIOperationNotImplementedError : UAPIErrorResponse() {
    override val metadata: UAPIErrorMetadata = UAPIErrorMetadata(
        ValidationResponse(404, "Not Found"),
        listOf("Not Found")
    )
    override val links: UAPILinks = emptyMap()
}

object UAPIEmptyResponse : UAPIResponse<EmptyResponseMetadata>() {
    override val metadata = EmptyResponseMetadata
    override val links: UAPILinks = emptyMap()
}

data class MultiClaimResponse(
    val results: Map<String, UAPIResponse<*>>,
    override val metadata: ClaimsResponseMetadata = ClaimsResponseMetadata(),
    override val links: UAPILinks = emptyMap()
) : UAPIResponse<ClaimsResponseMetadata>() {
    override fun renderExtras(renderer: Renderer<*>) {
        super.renderExtras(renderer)
        renderer.mergeTree(results)
    }
}

data class ClaimResponse(
    val verified: Boolean,
    override val metadata: ClaimsResponseMetadata = ClaimsResponseMetadata(),
    override val links: UAPILinks = emptyMap()
) : UAPIResponse<ClaimsResponseMetadata>() {
    override fun renderExtras(renderer: Renderer<*>) {
        renderer.value("verified", verified)
    }
}

data class UAPIClaimDescriptionResponse(
    val values: List<ConceptDescription>,
    override val metadata: ClaimsResponseMetadata = ClaimsResponseMetadata(),
    override val links: UAPILinks = emptyMap()
) : UAPIResponse<ClaimsResponseMetadata>() {
    data class ConceptDescription(
        val concept: String,
        val type: UAPIValueType,
        val relationships: Set<UAPIClaimRelationship>,
        val constraints: UAPIValueConstraints? = null
    ) : Renderable {
        override fun render(renderer: Renderer<*>) {
            renderer.value("concept", concept)
            renderer.value("type", type.apiValue)
            renderer.valueArray("relationships", relationships.map { it.apiValue })
            // TODO: Constraints
        }
    }

    override fun renderExtras(renderer: Renderer<*>) {
        renderer.treeArray("values", values)
    }
}
