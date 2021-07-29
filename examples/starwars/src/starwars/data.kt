package starwars

enum class Episode {
    NEW_HOPE,
    EMPIRE,
    JEDI
}

sealed class Character(
    val id: String,
    val name: String,
    val friends: MutableList<String>,
    val appearsIn: MutableList<Episode>
)

class Human(
    id: String,
    name: String,
    friends: MutableList<String>,
    appearsIn: MutableList<Episode>,
    val homePlanet: String
) : Character(id, name, friends, appearsIn)

class Droid(
    id: String,
    name: String,
    friends: MutableList<String>,
    appearsIn: MutableList<Episode>,
    val primaryFunction: String
) : Character(id, name, friends, appearsIn)
