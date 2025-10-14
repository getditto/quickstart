import 'package:json_annotation/json_annotation.dart';

part 'task.g.dart';

@JsonSerializable()
class Task {
  @JsonKey(name: "_id", includeIfNull: false)
  final String? id;
  final String title;
  final bool done;
  final bool deleted;

  const Task({
    this.id,
    required this.title,
    required this.done,
    required this.deleted,
  });

  factory Task.fromJson(Map<String, dynamic> json) => _$TaskFromJson(json);
  Map<String, dynamic> toJson() => _$TaskToJson(this);

  /// Creates a random task for testing (used in crash scenario simulations)
  factory Task.random() {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    return Task(
      title: 'Task ${timestamp % 10000}',
      done: false,
      deleted: false,
    );
  }
}
