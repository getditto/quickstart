//
//  View+DittoTools.swift
//  Tasks
//
//
//  Created by Jimmy Zeisweiss on 10/10/25.
//

import SwiftUI
import DittoAllToolsMenu

private struct DittoToolsModifier: ViewModifier {
    @State var showDittoTools: Bool = false

    func body(content: Content) -> some View {
        content
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        showDittoTools = true
                    } label: {
                        Image(systemName: "slider.horizontal.3")
                    }
                    .accessibilityLabel("Show Ditto Tools")
                }
            }
            .sheet(isPresented: $showDittoTools) {
                NavigationView {
                    AllToolsMenu(ditto: DittoManager.shared.ditto)
                }
            }
    }

    private var dittoToolsButton: some View {
        Button {
            showDittoTools = true
        } label: {
            Image(systemName: "slider.horizontal.3")
        }
    }
}

extension View {
    func dittoTools() -> some View {
        modifier(DittoToolsModifier())
    }
}
