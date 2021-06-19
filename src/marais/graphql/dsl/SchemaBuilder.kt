package marais.graphql.dsl

import graphql.Scalars
import graphql.schema.*
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

class SchemaBuilder(configure: SchemaSpec.() -> Unit) {

    internal val log = LoggerFactory.getLogger(SchemaBuilder::class.java)

    private val schemaBuilder = SchemaSpec().apply(configure)

    // A kotlin class to its mapped graphql type
    private val names = mutableMapOf<KClass<*>, String>()

    private val inputNames = mutableMapOf<KClass<*>, String>()

    // Maps kotlin types to graphql types
    private val scalars = mutableMapOf<KClass<*>, GraphQLScalarType>()
    private val enums = mutableMapOf<KClass<*>, GraphQLEnumType>()
    private val inputs = mutableMapOf<KClass<*>, GraphQLInputObjectType>()
    private val interfaces = mutableMapOf<KClass<*>, GraphQLInterfaceType>()
    private val types = mutableMapOf<KClass<*>, GraphQLObjectType>()
    private val codeRegistry = GraphQLCodeRegistry.newCodeRegistry()

    /**
     * Build the schema following the spec
     */
    fun build(): GraphQLSchema? {

        // At this step we should know everything in order to build the schema.

        scalars += schemaBuilder.scalars.map {
            // Probably unnecessary to put scalars since they don't reference anything else and they're mapped first
            names[it.kclass] = it.name
            it.kclass to GraphQLScalarType.newScalar()
                .name(it.name)
                .description(it.description)
                .coercing(it.coercing)
                .apply(it.builder)
                .build()
        }

        log.debug("Registered scalars : $scalars")

        enums += schemaBuilder.enums.map {
            names[it.kclass] = it.name
            it.kclass to GraphQLEnumType.newEnum()
                .name(it.name)
                .description(it.description)
                .apply {
                    for (enumConstant: Enum<*> in it.kclass.java.enumConstants as Array<Enum<*>>) {
                        value(enumConstant.name)
                    }
                }
                .build()
        }

        log.debug("Registered enums : $enums")

        // Early name registration since input objects can refer to each other
        schemaBuilder.inputs.forEach {
            inputNames[it.kclass] = it.name
        }

        inputs += schemaBuilder.inputs.map {
            it.kclass to GraphQLInputObjectType.newInputObject()
                .name(it.name)
                .description(it.description)
                .fields(it.fields.map { (name, type) ->
                    GraphQLInputObjectField.newInputObjectField()
                        .name(name)
                        .type(resolveInputType(type))
                        .build()
                })
                .build()
        }

        // Early name registration
        schemaBuilder.interfaces.forEach {
            names[it.kclass] = it.name
        }
        // Early name registration
        schemaBuilder.types.forEach {
            names[it.kclass] = it.name
        }

        // Interfaces
        interfaces += schemaBuilder.interfaces.map {
            it.kclass to makeInterface(it)
        }

        log.debug("Registered interfaces : $interfaces")

        // Any other types
        types += schemaBuilder.types.map {

            // Add default fields from parent interface if not present
            for (inter in it.interfaces) {
                for (field in schemaBuilder.interfaces.find { it.kclass == inter }!!.fields) {
                    // We find every field not implemented by the type.
                    if (it.fields.find { it.name == field.name } == null) {
                        it.fields.add(field)
                    }
                }
            }

            it.kclass to makeObject(it)
        }

        log.debug("Registered types : $types")

        val query = makeOperation(schemaBuilder.query)
        val mutation = schemaBuilder.mutation?.let { makeOperation(it) }
        val subscription = schemaBuilder.subscription?.let { makeOperation(it) }

        return GraphQLSchema.newSchema()
            .additionalTypes(scalars.values.toSet())
            .additionalTypes(enums.values.toSet())
            .additionalTypes(inputs.values.toSet())
            .additionalTypes(interfaces.values.toSet())
            .additionalTypes(types.values.toSet())
            .query(query)
            .mutation(mutation)
            .subscription(subscription)
            .codeRegistry(codeRegistry.build())
            .build()
    }

    private fun resolveOutputType(type: KType): GraphQLOutputType {

        val kclass = type.classifier as KClass<*>

        val resolved = if (kclass.isSubclassOf(List::class) || kclass.isSubclassOf(Array::class)) {
            GraphQLList.list(resolveOutputType(type.arguments[0].type!!))
        } else null
            ?: resolveInOutType(kclass) as? GraphQLOutputType
            // Search through what has already been resolved
            ?: interfaces[kclass] ?: types[kclass]
            // Fallback to late binding if possible
            ?: names[kclass]?.let { GraphQLTypeReference(it) }
            // We won't ever see it
            ?: throw Exception("Can't resolve $type to a valid graphql type")

        return if (type.isMarkedNullable) {
            resolved
        } else
            GraphQLNonNull.nonNull(resolved)
    }

    private fun resolveInputType(type: KType): GraphQLInputType {

        val kclass = type.classifier as KClass<*>

        val resolved = if (kclass.isSubclassOf(List::class) || kclass.isSubclassOf(Array::class)) {
            GraphQLList.list(resolveInputType(type.arguments[0].type!!))
        } else null
            ?: resolveInOutType(kclass) as? GraphQLInputType
            // Search through what has already been resolved
            ?: inputs[kclass]
            // Fallback to late binding if possible
            ?: inputNames[kclass]?.let { GraphQLTypeReference(it) }
            // We won't ever see it
            ?: throw Exception("Can't resolve $type to a valid graphql type")

        return if (type.isMarkedNullable) {
            resolved
        } else
            GraphQLNonNull.nonNull(resolved)
    }

    private fun resolveInOutType(kclass: KClass<*>): GraphQLType? {
        return scalars[kclass] ?: when (kclass) {
            Int::class -> Scalars.GraphQLInt
            Short::class -> Scalars.GraphQLInt // default
            Byte::class -> Scalars.GraphQLInt // default
            Float::class -> Scalars.GraphQLFloat
            Double::class -> Scalars.GraphQLFloat // default
            String::class -> Scalars.GraphQLString
            Char::class -> Scalars.GraphQLString // default
            Boolean::class -> Scalars.GraphQLBoolean
            in schemaBuilder.idTypes -> Scalars.GraphQLID
            else -> null
        } ?: enums[kclass]
    }

    private fun makeField(field: Field, parentType: String): GraphQLFieldDefinition {
        // Register the field data fetcher
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(parentType, field.name), field.dataFetcher)

        return GraphQLFieldDefinition.newFieldDefinition()
            .name(field.name)
            .description(field.description)
            .description(field.description)
            .arguments(field.arguments.map(this::makeArgument))
            .type(resolveOutputType(field.outputType))
            .build()
    }

    private fun makeArgument(argument: Argument): GraphQLArgument {
        return GraphQLArgument.newArgument()
            .name(argument.name)
            .type(resolveInputType(argument.type))
            .build()
    }

    private fun makeInterface(inter: InterfaceBuilder<*>): GraphQLInterfaceType {
        val fields = inter.fields.map { field ->
            makeField(field, inter.name)
        }

        codeRegistry.typeResolver(inter.name) { env ->
            env.schema.getObjectType(names[names.keys.find { it == env.getObject<Any?>()::class }])
        }

        return GraphQLInterfaceType.newInterface()
            .name(inter.name)
            .description(inter.description)
            .fields(fields)
            .build()
    }

    private fun makeObject(type: TypeBuilder<*>): GraphQLObjectType {
        val fields = type.fields.map { field ->
            makeField(field, type.name)
        }

        return GraphQLObjectType.newObject()
            .name(type.name)
            .description(type.description)
            .fields(fields)
            .withInterfaces(*type.interfaces.map { interfaces[it] }.toTypedArray())
            .build()
    }

    private fun makeOperation(operation: OperationBuilder<*>): GraphQLObjectType {
        val fields = operation.fields.map { field ->
            makeField(field, operation.name)
        }

        return GraphQLObjectType.newObject()
            .name(operation.name)
            .fields(fields)
            .build()
    }
}
