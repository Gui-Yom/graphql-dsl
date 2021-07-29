package starwars

object Query {
    val luke = Human("luke", "Luke Skywalker", mutableListOf(), mutableListOf(), "Tatooine")
    val r2d2 = Droid("r2d2", "R2-D2", mutableListOf(), mutableListOf(), "Naboo")

    fun hero(episode: Episode?): Character? {
        return when (episode) {
            Episode.NEW_HOPE, Episode.JEDI -> r2d2
            Episode.EMPIRE -> luke
            else -> null
        }
    }

    fun heroes(): List<Character> = listOf(luke, r2d2)
}

object Mutation {

}

object Subscription {

}
