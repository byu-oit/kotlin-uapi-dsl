package edu.byu.uapi.schemagen

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator
import edu.byu.uapi.model.jsonschema07.Schema
import edu.byu.uapi.model.serialization.jackson2.UAPIModelModule
import edu.byu.uapi.spi.introspection.SchemaGenerator
import kotlin.reflect.KClass

private val defaultObjectMapper by lazy { ObjectMapper().apply {
    findAndRegisterModules()
    registerModule(UAPIModelModule())
    propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
} }

class JacksonSchemaGenerator(mapper: ObjectMapper = defaultObjectMapper): SchemaGenerator {
    private val generator by lazy { JsonSchemaGenerator(mapper) }
    override fun generateSchemaFor(type: KClass<*>): Schema {
        return generator.generateJsonSchema(type.java).toSchema()
    }

    private fun JsonNode.toSchema(): Schema {
        return defaultObjectMapper.treeToValue(this)
    }
}

