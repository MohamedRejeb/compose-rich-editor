//
//  Compose_Rich_EditorApp.swift
//  Compose Rich Editor
//
//  Created by Mohamed Ben Rejeb on 1/5/2023.
//

import SwiftUI

@main
struct Compose_Rich_EditorApp: App {
    var body: some Scene {
        WindowGroup {
            GeometryReader { geo in
                ComposeViewControllerToSwiftUI(
                    topSafeArea: Float(geo.safeAreaInsets.top),
                    bottomSafeArea: Float(geo.safeAreaInsets.bottom)
                )
                .ignoresSafeArea()
            }
        }
    }
}
