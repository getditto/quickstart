import 'package:json_annotation/json_annotation.dart';
import 'package:ditto_live/ditto_live.dart';

part 'task.g.dart';

@JsonSerializable()
class Task {
  @JsonKey(name: "_id", includeIfNull: false)
  final String? id;
  final String title;
  final bool done;
  final bool deleted;
  final AttachmentToken? image;

  const Task({
    this.id,
    required this.title,
    required this.done,
    required this.deleted,
    required this.image,
  });

  factory Task.fromJson(Map<String, dynamic> json) => _$TaskFromJson(json);
  Map<String, dynamic> toJson() => _$TaskToJson(this);
}
