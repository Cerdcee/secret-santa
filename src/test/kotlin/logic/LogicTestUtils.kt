package logic

data class DummyExpression(
    val a: String,
    val b: String
) : LogicalVariable() {

    override fun toHumanReadable(): String = "${a}-->${b}"
}