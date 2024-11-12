package data.network.client

sealed class RequestTypes {
    data object Get : RequestTypes()
    data class Post(val body: Any) : RequestTypes()
}
