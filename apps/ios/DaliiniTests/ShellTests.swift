import DaliiniKit
import XCTest

/// The app as it starts: its configuration read from its own Info.plist, and the shared
/// screens drawn in their view controller.
final class ShellTests: XCTestCase {
    func testTheBuildCarriesTheBackendItWasConfiguredWith() {
        let configuration = ShellConfiguration.companion.fromBundle(bundle: Bundle.main)

        // Config/Debug.xcconfig: a development backend on this machine.
        XCTAssertEqual(configuration.apiBaseUrl, "http://localhost:8000/")
        XCTAssertTrue(configuration.allowCleartext)
        XCTAssertEqual(configuration.appLinkHost, "staging.root-domain.invalid")
    }

    func testTheSharedScreensLoadInTheirViewController() {
        let controller = MainViewControllerKt.MainViewController()

        controller.loadViewIfNeeded()

        XCTAssertNotNil(controller.view)
    }
}
