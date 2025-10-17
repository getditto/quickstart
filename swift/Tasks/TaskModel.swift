import DittoSwift

/// A document in the `tasks` collection
struct TaskModel {
    // swiftlint:disable:next identifier_name
    let _id: String
    var title: String
    var done: Bool = false
    var deleted: Bool = false
}

enum TaskModelSearchScope: String, CaseIterable {
    case title = "By Title", id = "By ID"
}

extension TaskModel {

    /// Convenience initializer returns instance from `QueryResultItem.value`
    init(_ value: [String: Any?]) {
        // swiftlint:disable:next force_cast
        self._id = value["_id"] as! String
        self.title = value["title"] as? String ?? ""
        self.done = value["done"] as? Bool ?? false
        self.deleted = value["deleted"] as? Bool ?? false
    }
}

extension TaskModel {

    /// Returns properties as key/value pairs for DQL INSERT query
    var value: [String: Any?] {
        [
            "_id": _id,
            "title": title,
            "done": done,
            "deleted": deleted
        ]
    }
}

// MARK: - Identifiable

extension TaskModel: Identifiable {

    /// Required for SwiftUI List view
    var id: String {
        return _id
    }

}

// MARK: - Equatable

extension TaskModel: Equatable {
    /// Required for TaskListScreen List animation
    static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.id == rhs.id
    }
}

// MARK: - Codable

extension TaskModel: Codable {

    /// Returns optional instance decoded from `QueryResultItem.jsonData()`
    init?(_ jsonData: Data) {
        do {
            self = try JSONDecoder().decode(Self.self, from: jsonData)
        } catch {
            print("ERROR:", error.localizedDescription)
            return nil
        }
    }
}

// MARK: - Preview support

extension TaskModel {

    /// Convenience initializer with defaults for previews and instances generated for new tasks
    init(
        title: String = "", done: Bool = false, deleted: Bool = false
    ) {
        self._id = UUID().uuidString
        self.title = title
        self.done = done
        self.deleted = deleted
    }
}
