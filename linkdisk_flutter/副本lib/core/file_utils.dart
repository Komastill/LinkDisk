import 'dart:io';
import 'package:path/path.dart' as path;

class FileUtils {
  static String formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
  }

  static String formatDate(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, "0")}-${dt.day.toString().padLeft(2, "0")} ${dt.hour.toString().padLeft(2, "0")}:${dt.minute.toString().padLeft(2, "0")}';
  }

  static Future<void> copyDir(Directory src, Directory dest) async {
    await dest.create(recursive: true);
    await for (var entity in src.list()) {
      String newPath = path.join(dest.path, path.basename(entity.path));
      if (entity is File) {
        await entity.copy(newPath);
      } else if (entity is Directory) {
        await copyDir(entity, Directory(newPath));
      }
    }
  }

  static Future<String> getUniquePath(String dirPath, String fileName) async {
    String p = path.join(dirPath, fileName);
    int i = 1;
    while (await File(p).exists() || await Directory(p).exists()) {
      String name = path.basenameWithoutExtension(fileName);
      String ext = path.extension(fileName);
      p = path.join(dirPath, "$name($i)$ext");
      i++;
    }
    return p;
  }
}