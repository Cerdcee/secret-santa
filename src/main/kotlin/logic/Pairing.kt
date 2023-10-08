package logic

import data.Person

data class Pairing(
    val person: Person,
    val linkedPerson: Person
) : LogicalVariable() {

    override fun toHumanReadable() = "${person.id}-->${linkedPerson.id}"
}