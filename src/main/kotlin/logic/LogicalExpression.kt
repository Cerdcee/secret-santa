package logic

import joinToLogicalExpression


// TODO do not allow users to implement LogicalExpression
interface LogicalExpression {
    fun simplifyXORs(): LogicalExpression

    fun distributeNOTs(): LogicalExpression

    /** CNF = Conjunctive Normal Form --> AND, OR, NOT, PrimitiveLogicalExpression
     * Convert to CNF :
     * 1) Move NOT to primitive types
     * 2) A OR (B AND C) -> (A OR B) AND (A OR C)
     * 3) Repeat until nothing changes anymore
     */
    fun toCNF(): LogicalExpression

    // For debug purposes
    fun toHumanReadable(): String
}

// This is the smallest, indivisible fragment of a logical expression
// Implement THIS class at least once
// TODO investigate how to prevent simplifyXORs, distributeNOTs and toCNF to be overriden
abstract class LogicalVariable : LogicalExpression {

    override fun simplifyXORs(): LogicalExpression = this

    override fun distributeNOTs(): LogicalExpression = this

    override fun toCNF(): LogicalExpression = this
}

class TRUE : LogicalVariable() {

    override fun toHumanReadable(): String = "true"
}

data class XOR(
    val a: LogicalExpression,
    val b: LogicalExpression
) : LogicalExpression {
    override fun simplifyXORs(): LogicalExpression =
        AND(
            OR(a.simplifyXORs(), b.simplifyXORs()),
            NOT(AND(a.simplifyXORs(), b.simplifyXORs()))
        ).simplifyXORs()

    override fun distributeNOTs(): LogicalExpression =
        throw IllegalStateException("Cannot use distributeNOTs() on XOR")

    // Replace simplify XOR
    override fun toCNF(): LogicalExpression =
        OR(
            AND(a.toCNF(), NOT(b.toCNF())),
            AND(NOT(a.toCNF()), b.toCNF())
        ).toCNF()

    override fun toHumanReadable() = "( ${a.toHumanReadable()} XOR ${b.toHumanReadable()} )"
}

data class OR(
    val a: LogicalExpression,
    val b: LogicalExpression
) : LogicalExpression {

    override fun simplifyXORs(): LogicalExpression = OR(a.simplifyXORs(), b.simplifyXORs())

    override fun distributeNOTs(): LogicalExpression = OR(a.distributeNOTs(), b.distributeNOTs())

    // A OR (B AND C) -> (A OR B) AND (A OR C)
    override fun toCNF(): LogicalExpression {
        val aCNF: LogicalExpression = a.toCNF()
        val bCNF: LogicalExpression = b.toCNF()

        val aORArray = toORArray(aCNF)
        val bORArray = toORArray(bCNF)

        val developedExprArray = mutableListOf<LogicalExpression>()

        aORArray.forEach { aORValue ->
            bORArray.forEach { bORValue ->
                developedExprArray += OR(aORValue, bORValue)
            }
        }

        if (developedExprArray.size == 1) return developedExprArray.first()

        return developedExprArray.joinToLogicalExpression { a, b -> AND(a, b) }
    }

    override fun toHumanReadable() = "( ${a.toHumanReadable()} OR ${b.toHumanReadable()} )"
}

fun toORArray(exprCNF: LogicalExpression): List<LogicalExpression> = // List of only ORs
    when (exprCNF) {
        is LogicalVariable, is NOT, is OR -> listOf(exprCNF)
        is AND -> toORArray(exprCNF.a) + toORArray(exprCNF.b)
        else -> throw IllegalStateException("Not a valid CNF for toORArray(), should be in [PrimitiveLogicalExpression, NOT, OR, AND]: $exprCNF")
    }

fun toPrimitiveArray(exprCNF: LogicalExpression): List<LogicalExpression> = // List of only PrimitiveLogicalExpression or NOT
    when (exprCNF) {
        is LogicalVariable, is NOT -> listOf(exprCNF)
        is OR -> toPrimitiveArray(exprCNF.a) + toPrimitiveArray(exprCNF.b)
        else -> throw IllegalStateException("Not a valid CNF for toPrimitiveArray(), should be in [PrimitiveLogicalExpression, NOT, OR]: $exprCNF")
    }

data class AND(
    val a: LogicalExpression,
    val b: LogicalExpression
) : LogicalExpression {
    override fun simplifyXORs(): LogicalExpression = AND(a.simplifyXORs(), b.simplifyXORs())

    override fun distributeNOTs(): LogicalExpression = AND(a.distributeNOTs(), b.distributeNOTs())

    override fun toCNF(): LogicalExpression = AND(a.toCNF(), b.toCNF())

    override fun toHumanReadable() = "( ${a.toHumanReadable()} AND ${b.toHumanReadable()} )"
}

data class NOT(
    val a: LogicalExpression
) : LogicalExpression {
    override fun simplifyXORs(): LogicalExpression = NOT(a.simplifyXORs())

    override fun distributeNOTs(): LogicalExpression =
        when (a) {
            is LogicalVariable -> this
            is NOT -> a.a.distributeNOTs()
            is AND -> OR(NOT(a.a.distributeNOTs()), NOT(a.b.distributeNOTs()))
            is OR -> AND(NOT(a.a.distributeNOTs()), NOT(a.b.distributeNOTs()))
            else -> throw IllegalStateException("Should not find logical expression other than AND, OR, NOT but found : $a")
        }

    // Replace distributeNOTs
    override fun toCNF(): LogicalExpression =
        when (a) {
            is LogicalVariable -> this
            is NOT -> a.a.toCNF()
            is AND -> OR(NOT(a.a.toCNF()), NOT(a.b.toCNF())).toCNF()
            is OR -> AND(NOT(a.a.toCNF()), NOT(a.b.toCNF())).toCNF()
            is XOR -> NOT(a.toCNF()).toCNF()
            else -> throw IllegalStateException("Should not find logical expression not in [AND, OR, NOT, XOR] but found : $a")
        }

    override fun toHumanReadable() = "NOT( ${a.toHumanReadable()} )"
}