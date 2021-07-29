package starwars

import marais.graphql.dsl.GraphQLSchema

fun main(args: Array<String>) {
    val schema = GraphQLSchema {

        enum<Episode>()

        inter<Character>()

        type<Human> {
            inter<Character>()

            derive()
        }

        type<Droid> {
            inter<Character>()

            derive()
        }

        query(Query)
    }
}
