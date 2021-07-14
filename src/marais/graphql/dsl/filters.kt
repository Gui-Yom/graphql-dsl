package marais.graphql.dsl

import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

sealed class BaseExclusionFilterBuilder {
    val nameExclusions: MutableList<String> = mutableListOf()

    /**
     * Do not include properties or functions with this name in the schema.
     * You should prefer using [BasePropertyExclusionFilter.exclude] or [BaseFunctionExclusionFilter.exclude] when possible.
     *
     * @throws Exception if this name has already been excluded from the schema
     */
    @SchemaDsl
    fun exclude(name: String) {
        if (name in nameExclusions)
            throw Exception("This name has already been excluded from the schema.")
        nameExclusions += name
    }

    /**
     * Do not include properties or functions with this name in the schema.
     * You should prefer using [BasePropertyExclusionFilter.exclude] or [BaseFunctionExclusionFilter.exclude] when possible.
     *
     * @throws Exception if this name has already been excluded from the schema
     */
    @SchemaDsl
    operator fun String.unaryMinus() {
        exclude(this)
    }
}

sealed interface BasePropertyExclusionFilter<R> {
    val propExclusions: MutableList<KProperty1<R, *>>

    /**
     * Do not include fields based on this property in the schema.
     *
     * @throws Exception if this property has already been excluded from the schema
     */
    @SchemaDsl
    fun exclude(prop: KProperty1<R, *>) {
        if (prop in propExclusions)
            throw Exception("This property has already been excluded from the schema.")
        propExclusions += prop
    }

    /**
     * Do not include fields based on this property in the schema.
     *
     * @throws Exception if this property has already been excluded from the schema
     */
    @SchemaDsl
    operator fun KProperty1<R, *>.unaryMinus() {
        exclude(this)
    }
}

sealed interface BaseFunctionExclusionFilter {
    val funExclusions: MutableList<KFunction<*>>

    /**
     * Do not include fields based on this function in the schema.
     *
     * @throws Exception if this function has already been excluded from the schema
     */
    @SchemaDsl
    fun exclude(func: KFunction<*>) {
        if (func in funExclusions)
            throw Exception("This function has already been excluded from the schema.")
        funExclusions += func
    }

    /**
     * Do not include fields based on this function in the schema.
     *
     * @throws Exception if this function has already been excluded from the schema
     */
    @SchemaDsl
    operator fun KFunction<*>.unaryMinus() {
        exclude(this)
    }
}

@SchemaDsl
class ExclusionFilterBuilder<R> : BaseExclusionFilterBuilder(), BasePropertyExclusionFilter<R>,
    BaseFunctionExclusionFilter {
    override val propExclusions: MutableList<KProperty1<R, *>> = mutableListOf()
    override val funExclusions: MutableList<KFunction<*>> = mutableListOf()
}

@SchemaDsl
class PropertyExclusionFilterBuilder<R> : BaseExclusionFilterBuilder(), BasePropertyExclusionFilter<R> {
    override val propExclusions: MutableList<KProperty1<R, *>> = mutableListOf()
}

@SchemaDsl
class FunctionExclusionFilterBuilder : BaseExclusionFilterBuilder(), BaseFunctionExclusionFilter {
    override val funExclusions: MutableList<KFunction<*>> = mutableListOf()
}
