package com.example.nexus.core.utils

/**
 * Generic sealed class for representing loading/success/error states
 * Used across all ViewModels and repositories for consistent state management.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    data object Idle : Resource<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorMessageOrNull(): String? = (this as? Error)?.message

    fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(message, exception)
        is Loading -> Loading
        is Idle -> Idle
    }

    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(message: String, exception: Throwable? = null) = Error(message, exception)
        fun loading() = Loading
        fun idle() = Idle
    }
}
