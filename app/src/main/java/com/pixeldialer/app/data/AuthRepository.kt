package com.pixeldialer.app.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
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

    /**
     * Firebase requires a valid app/google-services.json (matching this app's
     * applicationId) to be present at build time. Without it, FirebaseApp is
     * never registered and any FirebaseAuth.getInstance() / FirebaseFirestore
     * .getInstance() call throws IllegalStateException("Default FirebaseApp
     * is not initialized..."). That used to happen here, in the constructor
     * — which runs eagerly from PixelDialerApp.onCreate() — so the crash
     * surfaced later, whenever `currentUser` was first collected (i.e. as
     * soon as the Home screen composed after permissions were granted),
     * even though the real cause was Firebase setup at app-launch time.
     *
     * We now guard on FirebaseApp actually being initialized and fail soft:
     * firebaseAuth stays null, sign-in/account features quietly no-op, and
     * the rest of the app (dialer, contacts, recents, recording) is
     * unaffected. See the README's Firebase setup section to add a real
     * google-services.json and restore cloud sign-in/backup.
     */
    private val firebaseAuth: FirebaseAuth? = try {
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAuth.getInstance() else null
    } catch (e: IllegalStateException) {
        Log.w("AuthRepository", "Firebase not configured — sign-in disabled. Add google-services.json.", e)
        null
    }

    private val googleSignInClient: GoogleSignInClient? by lazy {
        if (firebaseAuth == null) return@lazy null
        val webId = webClientId()
        if (webId.isEmpty()) return@lazy null
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }

    /** The default_web_client_id string is auto-generated into strings.xml by the google-services plugin from google-services.json. */
    private fun webClientId(): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else ""
    }

    val currentUser: Flow<SignedInUser?> = firebaseAuth?.let { auth ->
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { a ->
                trySend(a.currentUser?.toSignedInUser())
            }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    } ?: flowOf(null)

    fun isSignedIn(): Boolean = firebaseAuth?.currentUser != null

    /** Null when Firebase isn't configured — caller should treat this as "sign-in unavailable". */
    fun signInIntent(): Intent? = googleSignInClient?.signInIntent

    suspend fun handleSignInResult(data: Intent?): SignInResult {
        val auth = firebaseAuth
            ?: return SignInResult.Failure("Sign-in isn't set up yet.")
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user?.toSignedInUser()
                ?: return SignInResult.Failure("Sign-in succeeded but no user was returned.")
            SignInResult.Success(user)
        } catch (e: Exception) {
            SignInResult.Failure(e.message ?: "Sign-in failed.")
        }
    }

    suspend fun signOut() {
        firebaseAuth?.signOut()
        try {
            googleSignInClient?.signOut()?.await()
        } catch (e: Exception) {
            // Non-fatal — Firebase-side sign-out already happened, which is what matters for app state.
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = firebaseAuth?.currentUser ?: return Result.failure(IllegalStateException("No signed-in user"))
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
