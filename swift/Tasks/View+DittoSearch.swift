//
//  View+DittoSearch.swift
//  Tasks
//
//
//  Created by Jimmy Zeisweiss on 10/10/25.
//

import SwiftUI

extension View {
    /// Adds a toolbar search with enum-based scopes.
    func dittoTaskSearch<Scope: CaseIterable & Hashable & RawRepresentable>(
        text: Binding<String>,
        scope: Binding<Scope>,
        placement: SearchFieldPlacement = .toolbar
    ) -> some View where Scope.RawValue == String {
        self
            .searchable(text: text, placement: placement)
            .searchScopes(scope) {
                ForEach(TaskModelSearchScope.allCases, id: \.self) { scope in
                    Text(scope.rawValue)
                }
            }
            .animation(.default, value: text.wrappedValue)
    }
}
