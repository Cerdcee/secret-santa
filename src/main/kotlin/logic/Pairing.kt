package logic

import data.Person

data class Pairing(
    val person: Person,
    val linkedPerson: Person
) : LogicalExpression {
    override fun simplifyXORs(): LogicalExpression = this

    override fun distributeNOTs(): LogicalExpression = this

    override fun toCNF(): LogicalExpression = this

    override fun toHumanReadable() = "${person.id}-->${linkedPerson.id}"
}