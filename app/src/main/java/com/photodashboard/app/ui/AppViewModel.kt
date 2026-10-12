package com.photodashboard.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photodashboard.app.BuildConfig
import com.photodashboard.app.PhotoDashboardApp
import com.photodashboard.app.data.LoginRequest
import com.photodashboard.app.data.PhotoEntity
import com.photodashboard.app.data.RegisterRequest
import com.photodashboard.app.data.Session
import com.photodashboard.app.data.apiMessage
import com.photodashboard.app.exif.GeoFix
import com.photodashboard.app.upload.UploadWorker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val session: Session) : SessionState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as PhotoDashboardApp).container
    private val repo = container.repository

    val apiConfigured = BuildConfig.API_BASE_URL.isNotBlank()

    val session: StateFlow<SessionState> = MutableStateFlow<SessionState>(SessionState.Loading).also { state ->
        viewModelScope.launch {
            container.session.session.collect { s ->
                state.value = if (s == null) SessionState.SignedOut else SessionState.SignedIn(s)
            }
        }
    }.asStateFlow()

    val photos: StateFlow<List<PhotoEntity>> = container.session.session
        .flatMapLatest { s -> if (s == null) flowOf(emptyList()) else container.db.photos().observe(s.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val wifiOnly = container.session.wifiOnly.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    private fun launchBusy(block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value = true
        try {
            block()
        } catch (e: Exception) {
            _message.value = e.apiMessage()
        } finally {
            _busy.value = false
        }
    }

    fun signIn(email: String, password: String) = launchBusy {
        container.session.save(container.api.login(LoginRequest(email.trim(), password)))
        afterSignIn()
    }

    fun register(email: String, password: String, displayName: String) = launchBusy {
        require(password.length >= 8) { "Password must be at least 8 characters" }
        val auth = container.api.register(RegisterRequest(email.trim(), password, displayName.trim()))
        container.session.save(auth)
        afterSignIn()
    }

    private suspend fun afterSignIn() {
        repo.resumePending()
        runCatching { repo.refreshFromServer() }
    }

    /** AND-3: revoke the token, stop pending uploads, keep local photos. */
    fun signOut() = viewModelScope.launch {
        UploadWorker.cancelAll(getApplication())
        runCatching { container.api.logout() }
        container.session.clear()
    }

    fun refresh() = launchBusy { repo.refreshFromServer() }

    fun setWifiOnly(value: Boolean) = viewModelScope.launch { container.session.setWifiOnly(value) }

    fun newCaptureFile(): File = repo.newCaptureFile()

    suspend fun locationFix(): GeoFix? = container.location.fixForCapture()

    fun onCaptured(file: File, capturedAt: Long, fix: GeoFix?) = viewModelScope.launch {
        try {
            repo.onCaptured(file, capturedAt, fix)
            if (fix == null) _message.value = "Saved without location"
        } catch (e: Exception) {
            _message.value = "Could not save photo: ${e.apiMessage()}"
        }
    }

    fun photo(localId: String): Flow<PhotoEntity?> = container.db.photos().observeOne(localId)

    fun retry(localId: String) = viewModelScope.launch { repo.retry(localId) }

    fun delete(localId: String, onDone: () -> Unit) = launchBusy {
        repo.delete(localId)
        onDone()
    }
}
