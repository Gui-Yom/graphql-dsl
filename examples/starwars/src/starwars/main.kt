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

            //Any string custom field
            "friends" { ->
                mutableListOf("C-3PO", "R2D2")
            }

            "name" { ->
                "BB-8"
            }

            //Custom field resolver based on the existing properties
            Droid::friends { ->
                mutableListOf("C-3PO", "R2D2")
            }

            Droid::name { ->
                "BB-8"
            }

        }

        query(Query)
    }
}
