package com.photodashboard.app

import android.content.Context
import com.photodashboard.app.data.ApiFactory
import com.photodashboard.app.data.PhotoApi
import com.photodashboard.app.data.PhotoDb
import com.photodashboard.app.data.PhotoRepository
import com.photodashboard.app.data.SessionStore
import com.photodashboard.app.location.LocationSource
import okhttp3.OkHttpClient

class AppContainer(context: Context) {
    val session = SessionStore(context)
    val http: OkHttpClient = ApiFactory.httpClient()
    val api: PhotoApi = ApiFactory.create(BuildConfig.API_BASE_URL.ifBlank { "https://api.invalid/v1" }, http) {
        session.cachedToken
    }
    val db = PhotoDb.create(context)
    val repository = PhotoRepository(context, db, api, session)
    val location = LocationSource(context)
}
