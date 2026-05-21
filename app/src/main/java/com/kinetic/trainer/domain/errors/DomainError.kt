package com.kinetic.trainer.domain.errors

/**
 * Unified error type hierarchy for domain layer.
 * Prevents raw exception leakage to UI layer.
 * 
 * NOTE: This is duplicated from kinetic app (Issue #9).
 * TODO: Move to shared module kinetic-common
 */
sealed class DomainError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /** Network-related errors */
    sealed class NetworkError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
        data class NoConnection(override val message: String = "No internet connection") : NetworkError(message)
        data class Timeout(override val message: String = "Request timed out") : NetworkError(message)
        data class ServerError(val code: Int, override val message: String = "Server error") : NetworkError(message)
        data class Unknown(override val cause: Throwable) : NetworkError("Network error occurred", cause)
    }

    /** Authentication/Authorization errors */
    sealed class AuthError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
        object Unauthorized : AuthError("Please log in to continue")
        object SessionExpired : AuthError("Your session has expired")
        object InvalidCredentials : AuthError("Invalid email or password")
        data class Unknown(override val cause: Throwable) : AuthError("Authentication error", cause)
    }

    /** Data validation errors */
    sealed class ValidationError(message: String) : DomainError(message) {
        data class InvalidField(val field: String, val reason: String) : ValidationError("Invalid: " + field)
        data class MissingRequired(val field: String) : ValidationError(field + " is required")
        data class InvalidRange(val field: String, val min: Int, val max: Int) : ValidationError("Range error: " + field)
    }

    /** Data access errors */
    sealed class DataError(message: String, cause: Throwable? = null) : DomainError(message, cause) {
        data class NotFound(val entity: String, val id: String) : DataError("Not found: " + entity)
        data class AlreadyExists(val entity: String) : DataError(entity + " already exists")
        data class DatabaseError(override val message: String, override val cause: Throwable? = null) : DataError(message, cause)
    }

    /** Business logic errors */
    sealed class BusinessError(message: String) : DomainError(message) {
        data class InsufficientPermissions(val action: String) : BusinessError("Permission denied")
        data class InvalidState(val reason: String) : BusinessError(reason)
        data class QuotaExceeded(val resource: String) : BusinessError("Quota exceeded: " + resource)
    }

    /** Catchall for unexpected errors */
    data class Unknown(override val message: String = "An unexpected error occurred", override val cause: Throwable? = null) : DomainError(message, cause)

    /** Convert to user-friendly message */
    fun toUserMessage(): String = when (this) {
        is NetworkError.NoConnection -> "Please check your internet connection and try again"
        is NetworkError.Timeout -> "Request timed out. Please try again"
        is NetworkError.ServerError -> "Server temporarily unavailable. Please try later"
        is AuthError.Unauthorized -> "Please log in to continue"
        is AuthError.SessionExpired -> "Your session has expired. Please log in again"
        is AuthError.InvalidCredentials -> "Invalid email or password"
        is ValidationError -> message
        is DataError.NotFound -> message
        is BusinessError.InsufficientPermissions -> message
        is BusinessError.QuotaExceeded -> message
        else -> "Something went wrong. Please try again"
    }
}

/** Map Throwable to DomainError */
fun Throwable.toDomainError(): DomainError = when (this) {
    is java.net.UnknownHostException -> DomainError.NetworkError.NoConnection()
    is java.net.SocketTimeoutException -> DomainError.NetworkError.Timeout()
    is java.io.IOException -> DomainError.NetworkError.Unknown(this)
    else -> DomainError.Unknown(message ?: "Unknown error", this)
}
