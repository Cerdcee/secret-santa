package data

data class Request(
    val type: RequestType,
    val otherPersonId: String
)

enum class RequestType {
    GIFT_BY,
    GIFT_TO
}