package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.PermissionKeys
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.AuthAndUserRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(val userProfile: UserProfile) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val authAndUserRepository: AuthAndUserRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                try {
                    val profile = authAndUserRepository.syncOrCreateUserProfile(user)
                    _uiState.value = AuthUiState.Authenticated(profile)
                } catch (e: Exception) {
                    Log.e("Auth", "Failed to sync profile: ${e.message}", e)
                    _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to load user profile")
                }
            }
        } else {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun enterGuestCustomerMode() {
        _isGuestMode.value = true
    }

    fun exitGuestCustomerMode() {
        _isGuestMode.value = false
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val profile = authAndUserRepository.syncOrCreateUserProfile(firebaseUser)
                        _uiState.value = AuthUiState.Authenticated(profile)
                        _isGuestMode.value = false
                        onSuccess()
                    } else {
                        onError("Firebase user is null")
                    }
                } else {
                    onError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(context: Context? = null, credentialManager: CredentialManager? = null) {
        auth.signOut()
        viewModelScope.launch {
            try {
                credentialManager?.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Clear credentials failed", e)
            } finally {
                _uiState.value = AuthUiState.Unauthenticated
                _isGuestMode.value = false
            }
        }
    }
}
