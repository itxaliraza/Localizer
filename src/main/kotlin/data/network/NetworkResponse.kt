package data.network
sealed class NetworkResponse<T>(
    val data: T? = null,
    val error: String? = null
) {
    class Success<T>(data: T) : NetworkResponse<T>(data = data)
    class Failure<T>(error: String) : NetworkResponse<T>(error = error)
    class Loading<T>() : NetworkResponse<T>()
    class Idle<T>() : NetworkResponse<T>()
}

inline fun <reified T> NetworkResponse<T>.doIfSuccess(mCallback: (T?) -> Unit) {
    when (this) {

        is NetworkResponse.Success -> {
            mCallback(this.data)
        }
        else->{}
    }
}

inline fun <reified T> NetworkResponse<T>.doIfSuccessOrFailure(
    mCallback: (T?) -> Unit,
    error: (String) -> Unit
) {
    when (this) {
        is NetworkResponse.Failure -> {
            error(this.error ?: "")
        }


        is NetworkResponse.Success -> {
            mCallback(this.data)
        }
        else->{}
    }
}