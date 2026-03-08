import SwiftUI
import ComposeApp
import GoogleSignIn

@main
struct iosApp: App {
    init() {
        MainViewControllerKt.setupKoin()
        let authManager = MainViewControllerKt.getAuthManager()
        GoogleSignInBridge.register(authManager)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
