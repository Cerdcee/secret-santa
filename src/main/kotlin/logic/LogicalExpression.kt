package logic

import joinToLogicalExpression

// https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html

// TODO do not allow users to implement LogicalExpression
interface LogicalExpression {
    fun simplifyXORs(): LogicalExpression

    fun distributeNOTs(): LogicalExpression

    // TODO replace with toFastCNF()
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

class RandomVariable(private val name: Int): LogicalVariable() {
    override fun toHumanReadable(): String = name.toString()
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

        return developedExprArray.joinToLogicalExpression { a, b -> AND(a, b) }
    }

    override fun toHumanReadable() = "( ${a.toHumanReadable()} OR ${b.toHumanReadable()} )"
}

// Returns list of only ORs of anything in [LogicalVariable, NOT(LogicalVariable), OR(following the same rules)] linked by ANDs
// exprCNF has already distributed NOTs
// Not recursive because would cause stack overflows
fun toORArray(exprCNF: LogicalExpression): List<LogicalExpression> {
    val orList = mutableListOf<LogicalExpression>()
    val exprPile = mutableListOf(exprCNF)

    while (exprPile.isNotEmpty()) {
        when (val currentExpr = exprPile.removeLast()) {
            is LogicalVariable, is NOT, is OR -> orList += currentExpr
            is AND -> exprPile += listOf(currentExpr.a, currentExpr.b)
            else -> throw IllegalStateException("Not a valid CNF for toORArray(), should be in [PrimitiveLogicalExpression, NOT, OR, AND]: $exprCNF")
        }
    }

    return orList
}

fun toPrimitiveArray(exprCNF: LogicalExpression): List<LogicalExpression> =
    // List of only PrimitiveLogicalExpression or NOT
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

// TODO test
// TODO explain what it does
fun LogicalExpression.toFastCNF(newRandomVariable: () -> RandomVariable): LogicalExpression {
    return when (this) {
        is LogicalVariable -> this
        is AND -> AND(a.toFastCNF(newRandomVariable), b.toFastCNF(newRandomVariable))
        is XOR -> OR(
            AND(a.toFastCNF(newRandomVariable), NOT(b.toFastCNF(newRandomVariable))),
            AND(NOT(a.toFastCNF(newRandomVariable)), b.toFastCNF(newRandomVariable))
        ).toFastCNF(newRandomVariable)
        is OR -> {
            val aCNF: LogicalExpression = a.toFastCNF(newRandomVariable)
            val bCNF: LogicalExpression = b.toFastCNF(newRandomVariable)

            val simplifyingVariable = newRandomVariable()

            val aORArray = toORArray(aCNF)
            val bORArray = toORArray(bCNF)

            val developedExprArray = mutableListOf<LogicalExpression>()

            aORArray.forEach { aORValue ->
                developedExprArray += OR(simplifyingVariable, aORValue)
            }
            bORArray.forEach { bORValue ->
                developedExprArray += OR(NOT(simplifyingVariable), bORValue)
            }

            developedExprArray.joinToLogicalExpression { a, b -> AND(a, b) }
        }
        is NOT -> when (a) {
            is LogicalVariable -> this
            is NOT -> a.a.toCNF()
            is AND -> OR(NOT(a.a.toCNF()), NOT(a.b.toCNF())).toCNF()
            is OR -> AND(NOT(a.a.toCNF()), NOT(a.b.toCNF())).toCNF()
            is XOR -> NOT(a.toCNF()).toCNF()
            else -> throw IllegalStateException("Should not find logical expression not in [AND, OR, NOT, XOR] but found : $a")
        }

        else -> toCNF()
    }
}