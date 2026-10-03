import DaliiniKit
import XCTest

/// The refresh secret in the Keychain, through the shared vault. These run inside the app,
/// because the simulator gives the Keychain only to a process that is an app (DECISION-093).
final class KeychainVaultTests: XCTestCase {
    private var service = ""

    override func setUp() {
        super.setUp()
        // A service of its own per test, so no test reads another's item or the app's.
        service = "com.servacode.directory.tests.\(UUID().uuidString)"
    }

    override func tearDown() {
        KeychainRefreshTokenVault(service: service).clear()
        super.tearDown()
    }

    func testNothingIsReadBeforeAnythingIsWritten() {
        XCTAssertNil(KeychainRefreshTokenVault(service: service).read())
    }

    func testTheSecretIsReadBackAndAWriteReplacesIt() {
        let vault = KeychainRefreshTokenVault(service: service)

        vault.write(value: "first-secret")
        XCTAssertEqual(vault.read(), "first-secret")

        vault.write(value: "second-secret")
        XCTAssertEqual(vault.read(), "second-secret")
    }

    func testTheSecretOutlivesTheObjectThatWroteIt() {
        // Kept by the Keychain, not in memory: the next launch's vault reads what this one wrote.
        KeychainRefreshTokenVault(service: service).write(value: "kept-secret")

        XCTAssertEqual(KeychainRefreshTokenVault(service: service).read(), "kept-secret")
    }

    func testClearingRemovesTheSecretAndClearingAgainIsHarmless() {
        let vault = KeychainRefreshTokenVault(service: service)
        vault.write(value: "secret")

        vault.clear()
        vault.clear()

        XCTAssertNil(vault.read())
    }

    func testArabicAndLongSecretsAreKeptExactly() {
        let vault = KeychainRefreshTokenVault(service: service)
        let secret = String(repeating: "رمز-٣٤٥-", count: 200)

        vault.write(value: secret)

        XCTAssertEqual(vault.read(), secret)
    }
}
