package data

// TODO replace first and last names with only one name field
data class Person(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val requests: List<Request>
)