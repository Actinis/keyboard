import SwiftUI
import ActinisKeyboardDemo

@main
struct iOSApp: App {
    init() {
        StartDiDarwinKt.startDi()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
