// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'task.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Task _$TaskFromJson(Map<String, dynamic> json) => Task(
  id: json['_id'] as String?,
  title: json['title'] as String,
  done: json['done'] as bool,
  deleted: json['deleted'] as bool,
  image: json['image'] as Map<String, dynamic>,
);

Map<String, dynamic> _$TaskToJson(Task instance) => <String, dynamic>{
  '_id': ?instance.id,
  'title': instance.title,
  'done': instance.done,
  'deleted': instance.deleted,
  'image': instance.image,
};
