package marais.graphql.dsl

interface DescriptionPublisher {
    var nextDesc: String?

    /**
     * Set the next element description
     */
    operator fun String.not() {
        nextDesc = this.trimIndent()
    }

    fun takeDesc(): String? {
        val desc = nextDesc
        nextDesc = null
        return desc
    }
}
