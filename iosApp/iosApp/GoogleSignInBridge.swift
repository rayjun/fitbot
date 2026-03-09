import GoogleSignIn
import ComposeApp
import UIKit

/// Bridges native GIDSignIn into the Kotlin AuthManager.
/// Call `GoogleSignInBridge.register(_:)` once at app startup, after Koin is
/// initialised, so that AuthManager.signInLauncher is set before the user
/// can tap "Sign In".
class GoogleSignInBridge {

    static func register(_ authManager: AuthManager) {
        // Normal Sign In
        authManager.signInLauncher = { callback in
            guard let rootVC = UIApplication.shared
                    .connectedScenes
                    .compactMap({ $0 as? UIWindowScene })
                    .first?.windows.first?.rootViewController else {
                callback.onSignInFailed(error: "No root view controller found")
                return
            }

            GIDSignIn.sharedInstance.signIn(
                withPresenting: rootVC,
                hint: nil,
                additionalScopes: ["https://www.googleapis.com/auth/drive.file"]
            ) { result, error in
                handleSignInResult(result: result, error: error, callback: callback)
            }
        }

        // Restore Previous Sign In (Silent)
        authManager.restoreSignInLauncher = { callback in
            GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                if let error = error {
                    callback.onSignInFailed(error: error.localizedDescription)
                    return
                }
                guard let user = user else {
                    callback.onSignInFailed(error: "No user to restore")
                    return
                }
                refreshAndReturn(user: user, callback: callback)
            }
        }
    }

    private static func handleSignInResult(result: GIDSignInResult?, error: Error?, callback: GoogleAuthCallback) {
        if let error = error {
            callback.onSignInFailed(error: error.localizedDescription)
            return
        }
        guard let user = result?.user else {
            callback.onSignInFailed(error: "No user returned by Google Sign-In")
            return
        }
        refreshAndReturn(user: user, callback: callback)
    }

    private static func refreshAndReturn(user: GIDGoogleUser, callback: GoogleAuthCallback) {
        user.refreshTokensIfNeeded { refreshedUser, refreshError in
            if let refreshError = refreshError {
                callback.onSignInFailed(error: refreshError.localizedDescription)
                return
            }
            guard let validUser = refreshedUser else {
                callback.onSignInFailed(error: "Could not obtain user after refresh")
                return
            }
            
            let token = validUser.accessToken.tokenString
            callback.onSignInSuccess(
                userId: validUser.userID ?? "",
                userName: validUser.profile?.name,
                userEmail: validUser.profile?.email,
                photoUrl: validUser.profile?.imageURL(withDimension: 200)?.absoluteString,
                accessToken: token
            )
        }
    }
}
