//
//  ComposeViewControllerToSwiftUi.swift
//  Compose Rich Editor
//
//  Created by Mohamed Ben Rejeb on 1/5/2023.
//

import SwiftUI
import common

struct ComposeViewControllerToSwiftUI: UIViewControllerRepresentable {
    private let topSafeArea: Float
    private let bottomSafeArea: Float
    
    init(topSafeArea: Float, bottomSafeArea: Float) {
        self.topSafeArea = topSafeArea
        self.bottomSafeArea = bottomSafeArea
    }
    
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController(
            topSafeArea: topSafeArea,
            bottomSafeArea: bottomSafeArea
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
