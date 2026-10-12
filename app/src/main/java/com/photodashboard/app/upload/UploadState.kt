package com.photodashboard.app.upload

enum class UploadState { PENDING, UPLOADING, UPLOADED, FAILED, REMOTE }

/** Steps of one upload attempt; the worker resumes from the first one not yet done. */
enum class UploadStep { REQUEST_URL, PUT_OBJECT, REGISTER, DONE }

/**
 * Pure transition rules for a photo's upload state, unit-tested without Android.
 * REMOTE marks a photo that exists only on the server (listed via GET /photos).
 */
object UploadStateMachine {
    const val MAX_ATTEMPTS = 10

    sealed interface Event {
        data object Enqueued : Event
        data object Started : Event
        data object Succeeded : Event
        data class Failed(val retryable: Boolean, val attempts: Int) : Event
        data object RetryRequested : Event
    }

    fun next(current: UploadState, event: Event): UploadState = when (event) {
        Event.Enqueued -> if (current == UploadState.UPLOADED || current == UploadState.REMOTE) current else UploadState.PENDING
        Event.Started -> when (current) {
            UploadState.PENDING, UploadState.FAILED, UploadState.UPLOADING -> UploadState.UPLOADING
            else -> current
        }
        Event.Succeeded -> if (current == UploadState.REMOTE) current else UploadState.UPLOADED
        is Event.Failed -> when {
            current == UploadState.UPLOADED || current == UploadState.REMOTE -> current
            event.retryable && event.attempts < MAX_ATTEMPTS -> UploadState.PENDING
            else -> UploadState.FAILED
        }
        Event.RetryRequested -> if (current == UploadState.FAILED) UploadState.PENDING else current
    }

    /** Where to resume, given what the Room row already records (AND-15). */
    fun resumeFrom(serverPhotoId: String?, objectUploaded: Boolean, registered: Boolean): UploadStep = when {
        registered -> UploadStep.DONE
        serverPhotoId == null -> UploadStep.REQUEST_URL
        !objectUploaded -> UploadStep.PUT_OBJECT
        else -> UploadStep.REGISTER
    }

    /** HTTP status codes worth retrying; 4xx other than 408/409/429 are permanent. */
    fun isRetryable(httpCode: Int?): Boolean = when (httpCode) {
        null -> true // network error
        408, 409, 429 -> true
        in 500..599 -> true
        else -> false
    }
}
