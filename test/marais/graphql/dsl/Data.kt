package marais.graphql.dsl

data class SimpleInput(val data: String)

data class NestedInput(val data: Int, val nested: SimpleInput)

data class SelfRefInput(val data: String, val nested: SelfRefInput?)

data class WrongSelfRefInput(val data: String, val nested: WrongSelfRefInput)

data class WithOtherFieldsInput(val data: String) {

    val ignored: String
        get() = "$data !"

    val ignored2: String = ""
}

enum class Baz {
    VALUE0,
    VALUE1,
    VALUE2
}
