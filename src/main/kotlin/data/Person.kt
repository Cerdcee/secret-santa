package data

data class Person(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val requests: List<Request>
)