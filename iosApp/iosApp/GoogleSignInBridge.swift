import GoogleSignIn
import ComposeApp
import UIKit

/// Bridges native GIDSignIn into the Kotlin AuthManager.
/// Call `GoogleSignInBridge.register(_:)` once at app startup, after Koin is
/// initialised, so that AuthManager.signInLauncher is set before the user
/// can tap "Sign In".
class GoogleSignInBridge {

    static func register(_ authManager: AuthManager) {
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
                if let error = error {
                    callback.onSignInFailed(error: error.localizedDescription)
                    return
                }
                guard let user = result?.user else {
                    callback.onSignInFailed(error: "No user returned by Google Sign-In")
                    return
                }
                // Refresh token to ensure it is valid before passing to Kotlin.
                user.refreshTokensIfNeeded { refreshedUser, refreshError in
                    if let refreshError = refreshError {
                        callback.onSignInFailed(error: refreshError.localizedDescription)
                        return
                    }
                    guard let token = refreshedUser?.accessToken.tokenString else {
                        callback.onSignInFailed(error: "Could not obtain access token")
                        return
                    }
                    callback.onSignInSuccess(
                        userId: refreshedUser?.userID ?? "",
                        userName: refreshedUser?.profile?.name,
                        userEmail: refreshedUser?.profile?.email,
                        photoUrl: refreshedUser?.profile?.imageURL(withDimension: 200)?.absoluteString,
                        accessToken: token
                    )
                }
            }
        }
    }
}
