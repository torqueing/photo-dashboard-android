package com.photodashboard.app.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val password: String,
    @param:Json(name = "display_name") val displayName: String,
    @param:Json(name = "invite_code") val inviteCode: String?,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: Long,
    val email: String,
    @param:Json(name = "display_name") val displayName: String,
    val role: String,
    @param:Json(name = "org_id") val orgId: Long,
    @param:Json(name = "org_name") val orgName: String?,
)

@JsonClass(generateAdapter = true)
data class AuthResponse(val token: String, val user: UserDto)

@JsonClass(generateAdapter = true)
data class UploadUrlRequest(@param:Json(name = "photo_id") val photoId: String?)

@JsonClass(generateAdapter = true)
data class UploadUrlResponse(
    @param:Json(name = "photo_id") val photoId: String,
    @param:Json(name = "s3_key") val s3Key: String,
    @param:Json(name = "upload_url") val uploadUrl: String,
    @param:Json(name = "public_url") val publicUrl: String,
    val status: String,
)

@JsonClass(generateAdapter = true)
data class RegisterPhotoRequest(
    @param:Json(name = "photo_id") val photoId: String,
    @param:Json(name = "s3_key") val s3Key: String,
    @param:Json(name = "captured_at") val capturedAt: String?,
    val latitude: Double?,
    val longitude: Double?,
    @param:Json(name = "altitude_m") val altitudeM: Double?,
    @param:Json(name = "accuracy_m") val accuracyM: Float?,
    @param:Json(name = "bearing_deg") val bearingDeg: Float?,
    @param:Json(name = "speed_mps") val speedMps: Float?,
    @param:Json(name = "location_source") val locationSource: String?,
    @param:Json(name = "device_make") val deviceMake: String?,
    @param:Json(name = "device_model") val deviceModel: String?,
    val width: Int?,
    val height: Int?,
    @param:Json(name = "size_bytes") val sizeBytes: Long?,
)

@JsonClass(generateAdapter = true)
data class PhotoDto(
    val id: String,
    @param:Json(name = "s3_key") val s3Key: String,
    @param:Json(name = "public_url") val publicUrl: String,
    val status: String,
    @param:Json(name = "captured_at") val capturedAt: String?,
    val latitude: Double?,
    val longitude: Double?,
    @param:Json(name = "altitude_m") val altitudeM: Double?,
    @param:Json(name = "accuracy_m") val accuracyM: Double?,
    @param:Json(name = "device_model") val deviceModel: String?,
)

@JsonClass(generateAdapter = true)
data class PhotoPage(val items: List<PhotoDto>, @param:Json(name = "next_cursor") val nextCursor: String?)

@JsonClass(generateAdapter = true)
data class ApiErrorBody(val error: ApiErrorDetail?)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(val code: String?, val message: String?)
