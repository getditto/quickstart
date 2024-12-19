// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'task.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Task _$TaskFromJson(Map<String, dynamic> json) => Task(
      id: json['_id'] as String?,
      title: json['title'] as String,
      description: json['description'] as String,
      done: json['done'] as bool,
      deleted: json['deleted'] as bool,
    );

Map<String, dynamic> _$TaskToJson(Task instance) {
  final val = <String, dynamic>{};

  void writeNotNull(String key, dynamic value) {
    if (value != null) {
      val[key] = value;
    }
  }

  writeNotNull('_id', instance.id);
  val['title'] = instance.title;
  val['description'] = instance.description;
  val['done'] = instance.done;
  val['deleted'] = instance.deleted;
  return val;
}
