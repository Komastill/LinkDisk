import 'dart:io';

class TransferFileItem {
  // 私有属性（对应 Java private）
  final File _sourceFile;
  final String _relativePath;
  final int _size;

  // 构造函数
  TransferFileItem(this._sourceFile, this._relativePath)
      : _size = _sourceFile.lengthSync();

  // getter 方法（对应 Java getXxx）
  File get sourceFile => _sourceFile;
  String get relativePath => _relativePath;
  int get size => _size;
}