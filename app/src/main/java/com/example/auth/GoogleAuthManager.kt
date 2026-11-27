package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "Auth"

data class GoogleAccountInfo(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?
)

fun FirebaseUser.toGoogleAccountInfo(): GoogleAccountInfo = GoogleAccountInfo(
    uid = uid,
    displayName = displayName?.takeIf { it.isNotBlank() } ?: "Google User",
    email = email.orEmpty(),
    photoUrl = photoUrl?.toString()
)

fun isFirebaseConfigured(context: Context): Boolean {
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) != null
        } else {
            true
        }
    } catch (e: Exception) {
        Log.w(TAG, "FirebaseApp unavailable: ${e.message}")
        false
    }
}

fun getSafeCurrentGoogleAccount(context: Context): GoogleAccountInfo? {
    if (!isFirebaseConfigured(context)) return null
    return try {
        Firebase.auth.currentUser?.toGoogleAccountInfo()
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read Firebase currentUser: ${e.message}")
        null
    }
}

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: (GoogleAccountInfo) -> Unit,
    onAuthCancelled: (String) -> Unit,
    onAuthError: (String) -> Unit
) {
    if (!isFirebaseConfigured(context)) {
        onAuthError("Firebase not configured (google-services.json missing). Offline mode remains active.")
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.w(TAG, "default_web_client_id unavailable: ${e.message}")
        null
    }

    if (clientId == null) {
        onAuthError("Google Sign-In unavailable: default_web_client_id not found in google-services.json. Offline mode remains active.")
        return
    }

    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Auth Error: Unable to resolve host Activity for Credential Manager.")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    onAuthSuccess(user.toGoogleAccountInfo())
                } else {
                    onAuthError("Auth Error: Firebase returned no user after Google credential exchange.")
                }
            } else {
                onAuthError("Auth Error: Unexpected credential type (${credential.type}).")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled("Google Sign-In cancelled. All offline features remain active.")
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials available: ${e.message}", e)
            onAuthError("No Google account available on device (${e.localizedMessage ?: "offline or not signed in"}). Offline mode remains active.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            onAuthError("Auth Error (${e.javaClass.simpleName}): ${e.localizedMessage ?: "Sign-in unavailable offline"}")
        }
    }
}

fun signOutGoogle(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: (String) -> Unit
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                if (isFirebaseConfigured(context)) {
                    try {
                        Firebase.auth.signOut()
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase signOut error: ${e.message}")
                    }
                }
                onSignOutComplete("Signed out. Your local Room workspace stays safely on this device.")
            }
        }
    }
}
