package edu.byu.uapi.server.types

import edu.byu.uapi.server.rendering.Renderable
import edu.byu.uapi.server.rendering.Renderer
import edu.byu.uapi.server.scalars.ScalarType

sealed class UAPIProperty: Renderable {
    abstract val apiType: APIType
    abstract val key: Boolean
    abstract val displayLabel: String?
    abstract val domain: OrMissing<String>
    abstract val relatedResource: OrMissing<String>

    final override fun render(renderer: Renderer<*>) {
        renderValue(renderer)
        renderer.value("api_type", apiType)
        if (key) {
            renderer.value("key", key)
        }
        displayLabel?.let { renderer.value("display_label", it) }
        domain.ifPresent { renderer.value("domain", it) }
        relatedResource.ifPresent { renderer.value("related_resource", it) }
    }

    protected abstract fun renderValue(renderer: Renderer<*>)
}

class UAPIValueProperty<Value: Any>(
    val value: Value?,
    val description: OrMissing<String>,
    val longDescription: OrMissing<String>,
    override val apiType: APIType,
    override val key: Boolean,
    override val displayLabel: String?,
    override val domain: OrMissing<String>,
    override val relatedResource: OrMissing<String>
): UAPIProperty() {
    override fun renderValue(
        renderer: Renderer<*>
    ) {
        renderer.value("value", value)
        description.ifPresent { renderer.value("description", it) }
        longDescription.ifPresent { renderer.value("long_description", it) }
    }
}

sealed class OrMissing<out Type : Any> {

    abstract fun ifPresent(fn: (Type?) -> Unit)
    abstract fun <R: Any> map(fn: (Type?) -> R?): OrMissing<R>

    data class Present<out Type : Any>(
        val value: Type?
    ) : OrMissing<Type>() {
        override fun ifPresent(fn: (Type?) -> Unit) {
            fn(value)
        }

        override fun <R : Any> map(fn: (Type?) -> R?): OrMissing<R> {
            return Present(fn(this.value))
        }
    }

    object Missing : OrMissing<Nothing>() {
        override fun ifPresent(fn: (Nothing?) -> Unit) {
        }

        override fun <R : Any> map(fn: (Nothing?) -> R?) = Missing
    }
}

enum class APIType(val apiValue: String) {
    READ_ONLY("read-only"),
    MODIFIABLE("modifiable"),
    SYSTEM("system"),
    DERIVED("derived"),
    RELATED("related");
}


