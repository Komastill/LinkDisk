import 'dart:io';
class AndroidFileService {
  Future<List<FileSystemEntity>> load(String path) async {
    try {
      return await Directory(path).list().toList();
    } catch (e) {
      return [];
    }
  }
}