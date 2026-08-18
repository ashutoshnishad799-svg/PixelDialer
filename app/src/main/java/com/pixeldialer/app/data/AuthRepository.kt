package com.pixeldialer.app.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class SignedInUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

sealed class SignInResult {
    data class Success(val user: SignedInUser) : SignInResult()
    data class Failure(val message: String) : SignInResult()
}

/**
 * Wraps Firebase Auth + Google Sign-In. This is the ONLY place in the app
 * that talks to FirebaseAuth directly — everything else goes through the
 * currentUser flow or the sign-in/sign-out functions here.
 *
 * Note for whoever builds this: Firebase requires a google-services.json
 * file in app/ generated from your own Firebase Console project (Project
 * Settings → Your apps → Android app with package com.pixeldialer.app).
 * That file is project-specific and can't be generated here — see the
 * README's Firebase setup section for the exact steps.
 */
class AuthRepository(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId())
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    /** The default_web_client_id string is auto-generated into strings.xml by the google-services plugin from google-services.json. */
    private fun webClientId(): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else ""
    }

    val currentUser: Flow<SignedInUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toSignedInUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun isSignedIn(): Boolean = firebaseAuth.currentUser != null

    fun signInIntent(): Intent = googleSignInClient.signInIntent

    suspend fun handleSignInResult(data: Intent?): SignInResult {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user?.toSignedInUser()
                ?: return SignInResult.Failure("Sign-in succeeded but no user was returned.")
            SignInResult.Success(user)
        } catch (e: Exception) {
            SignInResult.Failure(e.message ?: "Sign-in failed.")
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            // Non-fatal — Firebase-side sign-out already happened, which is what matters for app state.
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(IllegalStateException("No signed-in user"))
        return try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun FirebaseUser.toSignedInUser() = SignedInUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString()
    )
}
