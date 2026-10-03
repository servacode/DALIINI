import DaliiniKit
import SwiftUI

/// The iPhone app. Everything it shows is the shared Kotlin code's (DaliiniKit); this file only
/// gives it a window. DECISION-093.
@main
struct DaliiniApp: App {
    var body: some Scene {
        WindowGroup {
            SharedScreens()
                // Compose draws edge to edge and keeps its own content clear of the notch and the
                // home indicator (safeDrawingPadding), as Android's screens do.
                .ignoresSafeArea()
        }
    }
}

/// The Compose Multiplatform screens, inside SwiftUI.
struct SharedScreens: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
