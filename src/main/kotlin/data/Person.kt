package data

data class Person(
    val id: String,
    val name: String,
    val email: String,
    val requests: List<Request>
)