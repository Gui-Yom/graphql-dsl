package marais.graphql.dsl

import graphql.Scalars
import graphql.schema.*
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

@SchemaDsl
fun GraphQLSchema(spec: SchemaSpec.() -> Unit): GraphQLSchema {
    return SchemaBuilder(spec).build()
}

/**
 * The base object for building a schema.
 *
 * @param configure the DSL to create the schema.
 *
 * @see SchemaSpec
 */
class SchemaBuilder(configure: SchemaSpec.() -> Unit) {

    private val log = LoggerFactory.getLogger(SchemaBuilder::class.java)

    private val schemaSpec = SchemaSpec(log).apply(configure)

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
    fun build(): GraphQLSchema {

        // At this step we should know everything in order to build the schema.

        scalars += schemaSpec.scalars.map {
            // Probably unnecessary to put scalars since they don't reference anything else and they're mapped first
            names[it.kclass] = it.name
            it.kclass to it.createScalar()
        }

        log.debug("Registered scalars : $scalars")

        enums += schemaSpec.enums.map {
            names[it.kclass] = it.name
            it.kclass to it.createEnum()
        }

        log.debug("Registered enums : $enums")

        // Early name registration since input objects can refer to each other
        schemaSpec.inputs.forEach {
            inputNames[it.kclass] = it.name
        }

        inputs += schemaSpec.inputs.map {
            it.kclass to it.createInputObject()
        }

        // Early name registration
        schemaSpec.interfaces.forEach {
            names[it.kclass] = it.name
        }
        // Early name registration
        schemaSpec.types.forEach {
            names[it.kclass] = it.name
        }

        // Interfaces
        interfaces += schemaSpec.interfaces.map {
            it.kclass to it.createInterface()
        }

        log.debug("Registered interfaces : $interfaces")

        // Any other types
        types += schemaSpec.types.map {

            // Add default fields from parent interface if not present
            for (inter in it.interfaces) {
                for (field in schemaSpec.interfaces.find { it.kclass == inter }!!.fields) {
                    // We find every field not implemented by the type.
                    if (it.fields.find { it.name == field.name } == null) {
                        it.fields.add(field)
                    }
                }
            }

            it.kclass to it.createOutputObject()
        }

        log.debug("Registered types : $types")

        val query = schemaSpec.query.createOperation()
        val mutation = schemaSpec.mutation?.createOperation()
        val subscription = schemaSpec.subscription?.createOperation()

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

        val resolved = resolveInOutType(kclass) as? GraphQLOutputType
        // Search through what has already been resolved
            ?: interfaces[kclass] ?: types[kclass]
            // Fallback to late binding if possible
            ?: names[kclass]?.let { GraphQLTypeReference(it) }
            ?: if (kclass.coerceWithList()) {
                GraphQLList(resolveOutputType(type.unwrap()))
            } else null
                ?: if (kclass.isSubclassOf(Map::class)) {
                    GraphQLList(GraphQLNonNull(makeMapEntry(type)))
                } else null
                // We won't ever see it
                    ?: throw Exception("Can't resolve $type to a valid graphql type")

        return if (type.isMarkedNullable)
            resolved
        else
            GraphQLNonNull(resolved)
    }

    private fun resolveInputType(type: KType): GraphQLInputType {

        val kclass = type.classifier as KClass<*>

        val resolved = resolveInOutType(kclass) as? GraphQLInputType
        // Search through what has already been resolved
            ?: inputs[kclass]
            // Fallback to late binding if possible
            ?: inputNames[kclass]?.let { GraphQLTypeReference(it) }
            ?: if (kclass.coerceWithList()) {
                GraphQLList(resolveInputType(type.unwrap()))
            } else null
            // We won't ever see it
                ?: throw Exception("Can't resolve $type to a valid graphql type")

        return if (type.isMarkedNullable)
            resolved
        else
            GraphQLNonNull(resolved)
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
            in schemaSpec.idCoercers -> Scalars.GraphQLID
            else -> null
        } ?: enums[kclass]
    }

    private fun ScalarSpec.createScalar(): GraphQLScalarType {
        return GraphQLScalarType.newScalar()
            .name(name)
            .description(description)
            .coercing(coercing)
            .apply(builder)
            .build()
    }

    private fun EnumSpec.createEnum(): GraphQLEnumType {
        return GraphQLEnumType.newEnum()
            .name(name)
            .description(description)
            .apply {
                for (enumConstant: Enum<*> in kclass.java.enumConstants as Array<Enum<*>>) {
                    value(enumConstant.name)
                }
            }
            .apply(builder)
            .build()
    }

    private fun InputSpec.createInputObject(): GraphQLInputObjectType {
        return GraphQLInputObjectType.newInputObject()
            .name(name)
            .description(description)
            .fields(fields.map { (name, type) ->
                GraphQLInputObjectField.newInputObjectField()
                    .name(name)
                    .type(resolveInputType(type))
                    .build()
            })
            .apply(builder)
            .build()
    }

    private fun FieldSpec.createField(parentType: String): GraphQLFieldDefinition {
        // Register the field data fetcher
        codeRegistry.dataFetcher(FieldCoordinates.coordinates(parentType, name), dataFetcher)

        return GraphQLFieldDefinition.newFieldDefinition()
            .name(name)
            .description(description)
            .arguments(arguments.filter { it.isShownInSchema }.map { it.createArgument() })
            .type(resolveOutputType(outputType))
            .build()
    }

    private fun Argument.createArgument(): GraphQLArgument {
        return GraphQLArgument.newArgument()
            .name(name)
            .description(description)
            .type(resolveInputType(type))
            .build()
    }

    private fun InterfaceSpec<*>.createInterface(): GraphQLInterfaceType {
        val fields = fields.map {
            it.createField(name)
        }

        codeRegistry.typeResolver(name) { env ->
            env.schema.getObjectType(names[names.keys.find { it == env.getObject<Any?>()::class }])
        }

        return GraphQLInterfaceType.newInterface()
            .name(name)
            .description(description)
            .fields(fields)
            .build()
    }

    private fun TypeSpec<*>.createOutputObject(): GraphQLObjectType {
        val fields = fields.map {
            it.createField(name)
        }

        return GraphQLObjectType.newObject()
            .name(name)
            .description(description)
            .fields(fields)
            .withInterfaces(*interfaces.map { this@SchemaBuilder.interfaces[it] }.toTypedArray())
            .build()
    }

    private fun OperationSpec<*>.createOperation(): GraphQLObjectType {
        val fields = fields.map {
            it.createField(name)
        }

        return GraphQLObjectType.newObject()
            .name(name)
            .fields(fields)
            .build()
    }

    private fun makeMapEntry(type: KType): GraphQLObjectType {
        val name = type.deepName()

        codeRegistry.dataFetcher(
            FieldCoordinates.coordinates(name, "key"),
            DataFetcher { (it.getSource() as Map.Entry<*, *>).key })

        codeRegistry.dataFetcher(
            FieldCoordinates.coordinates(name, "value"),
            DataFetcher { (it.getSource() as Map.Entry<*, *>).value })

        val obj = GraphQLObjectType.newObject()
            .name(name)
            .field {
                it.name("key")
                    .type(resolveOutputType(type.arguments[0].type!!))
            }
            .field {
                it.name("value")
                    .type(resolveOutputType(type.arguments[1].type!!))
            }
            .build()

        types[Map::class] = obj

        return obj
    }
}
