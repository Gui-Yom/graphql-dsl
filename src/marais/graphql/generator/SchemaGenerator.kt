package marais.graphql.generator

import graphql.Scalars
import graphql.schema.*
import marais.graphql.dsl.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

class SchemaGenerator(configure: SchemaBuilder.() -> Unit) {

    private val schemaBuilder = SchemaBuilder().apply(configure)

    private val names = mutableMapOf<KType, String>()

    // Maps kotlin types to graphql types
    private val scalars = mutableMapOf<KType, GraphQLScalarType>()
    private val enums = mutableMapOf<KType, GraphQLEnumType>()
    private val interfaces = mutableMapOf<KType, GraphQLInterfaceType>()
    private val types = mutableMapOf<KType, GraphQLObjectType>()
    private val codeRegistry = GraphQLCodeRegistry.newCodeRegistry()

    fun build(): GraphQLSchema? {
        scalars += schemaBuilder.scalars.map {
            // Probably unnecessary to put scalars since they don't reference anything else and they're mapped first
            names[it.type] = it.name
            it.type to GraphQLScalarType.newScalar()
                    .name(it.name)
                    .coercing(it.coercing)
                    .apply(it.builder)
                    .build()
        }

        println("Registered scalars : $scalars")

        enums += schemaBuilder.enums.map {
            names[it.type] = it.name
            it.type to GraphQLEnumType.newEnum()
                    .name(it.name)
                    .apply {
                        for (enumConstant: Enum<*> in (it.type.classifier!! as KClass<*>).java.enumConstants as Array<Enum<*>>) {
                            value(enumConstant.name)
                        }
                    }
                    .build()
        }

        println("Registered enums : $enums")

        // Early name registration
        schemaBuilder.interfaces.forEach { (ktype, inter) ->
            names[ktype] = inter.name
        }
        // Early name registration
        schemaBuilder.types.forEach { (ktype, type) ->
            names[ktype] = type.name
        }

        // Interfaces
        interfaces += schemaBuilder.interfaces.map { (ktype, inter) ->
            val fields = inter.fields.map { field ->
                makeField(field, inter.name)
            }

            codeRegistry.typeResolver(inter.name) { env ->
                env.schema.getObjectType(names[names.keys.find { it.classifier == env.getObject<Any?>()::class }])
            }

            ktype to GraphQLInterfaceType.newInterface()
                    .name(inter.name)
                    .fields(fields)
                    .build()
        }

        println("Registered interfaces : $interfaces")

        // Any other types
        types += schemaBuilder.types.map { (ktype, type) ->
            names[ktype] = type.name
            ktype to makeObject(type)
        }

        println("Registered types : $types")

        val query = makeOperation(schemaBuilder.query)
        val mutation = schemaBuilder.mutation?.let { makeOperation(it) }
        val subscription = schemaBuilder.subscription?.let { makeOperation(it) }

        return GraphQLSchema.newSchema()
                .additionalTypes(scalars.values.toSet())
                .additionalTypes(enums.values.toSet())
                .additionalTypes(interfaces.values.toSet())
                .additionalTypes(types.values.toSet())
                .query(query)
                .mutation(mutation)
                .subscription(subscription)
                .codeRegistry(codeRegistry.build())
                .build()
    }

    private fun resolveOutputType(type: KType): GraphQLOutputType {

        val nonNullType = type.withNullability(false)
        // Try standard types
        val resolved = when (type.classifier!!) {
            Int::class -> Scalars.GraphQLInt
            Float::class -> Scalars.GraphQLFloat
            Double::class -> Scalars.GraphQLFloat
            String::class -> Scalars.GraphQLString
            Boolean::class -> Scalars.GraphQLBoolean
            else -> null
        }
        // Try to search through what has already been mapped
                ?: scalars[nonNullType] ?: enums[nonNullType] ?: interfaces[nonNullType] ?: types[nonNullType]
                // Fallback to late binding if possible
                ?: names[nonNullType]?.let { GraphQLTypeReference(it) }
                // We won't ever see it
                ?: throw Exception("Can't resolve $nonNullType to a valid graphql type")

        return if (type.isMarkedNullable) {
            resolved
        } else
            GraphQLNonNull.nonNull(resolved)
    }

    private fun resolveInputType(type: KType): GraphQLInputType {
        val nonNullType = type.withNullability(false)
        // Try standard types
        val resolved = when (type.classifier!!) {
            Int::class -> Scalars.GraphQLInt
            Float::class -> Scalars.GraphQLFloat
            Double::class -> Scalars.GraphQLFloat
            String::class -> Scalars.GraphQLString
            Boolean::class -> Scalars.GraphQLBoolean
            else -> null
        }
        // Try to search through what has already been mapped
                ?: scalars[nonNullType] ?: enums[nonNullType]
                // TODO search through input objects too
                // We won't ever see it
                ?: throw Exception("Can't resolve $nonNullType to a valid graphql type")

        return if (type.isMarkedNullable) {
            resolved
        } else
            GraphQLNonNull.nonNull(resolved)
    }

    private fun makeField(field: Field, parentType: String): GraphQLFieldDefinition? {
        // Register the field data fetcher
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(parentType, field.name), field.dataFetcher)

        return GraphQLFieldDefinition.newFieldDefinition()
                .name(field.name)
                .description(field.description)
                .arguments(field.arguments.map(this::makeArgument))
                .type(resolveOutputType(field.outputType))
                .build()
    }

    fun makeArgument(argument: Argument): GraphQLArgument {
        return GraphQLArgument.newArgument()
                .name(argument.name)
                .type(resolveInputType(argument.type))
                .build()
    }

    private fun makeObject(type: TypeBuilder<*>): GraphQLObjectType {
        val fields = type.fields.map { field ->
            makeField(field, type.name)
        }

        return GraphQLObjectType.newObject()
                .name(type.name)
                .fields(fields)
                .withInterfaces(*type.interfaces.map { interfaces[it] }.toTypedArray())
                .build()
    }

    private fun makeOperation(operation: Type<*>): GraphQLObjectType {
        val fields = operation.fields.map { field ->
            makeField(field, operation.name)
        }

        return GraphQLObjectType.newObject()
                .name(operation.name)
                .fields(fields)
                .build()
    }
}
