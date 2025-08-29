import 'package:flutter/material.dart';
import 'dart:typed_data';

Future<void> showImageDialog(BuildContext context, Uint8List imageData) {
  return showDialog(
    context: context,
    builder: (context) => _ImageDialog(imageData),
  );
}

class _ImageDialog extends StatelessWidget {
  final Uint8List imageData;

  const _ImageDialog(this.imageData);

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      content: Image.memory(imageData),
    );
  }
}