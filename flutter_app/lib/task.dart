import 'package:json_annotation/json_annotation.dart';

part 'task.g.dart';

@JsonSerializable()
class Task {
  @JsonKey(name: "_id", includeIfNull: false)
  final String? id;
  final String title;
  final bool done;
  final bool deleted;
  @JsonKey(includeIfNull: false)
  final Map<String, dynamic>? attachment;

  const Task({
    this.id,
    required this.title,
    required this.done,
    required this.deleted,
    this.attachment,
  });

  factory Task.fromJson(Map<String, dynamic> json) => _$TaskFromJson(json);
  Map<String, dynamic> toJson() => _$TaskToJson(this);
}
