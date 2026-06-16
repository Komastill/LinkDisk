import 'dart:io';
import 'package:path/path.dart' as path;
import '../../../core/file_utils.dart';
import '../../../core/platform_utils.dart';

enum ClipType { none, copy, cut }

class WindowsFileService {
  List<String> drives = [];

  Future<void> initDrives() async {
    drives.clear();
    for (int c = 67; c <= 90; c++) {
      String d = String.fromCharCode(c);
      if (await Directory("$d:/").exists()) drives.add(d);
    }
  }

  Future<List<FileSystemEntity>> load(String path, {bool showHidden = false}) async {
    try {
      var list = await Directory(path).list().toList();
      list = list.where((e) {
        String n = path.basename(e.path);
        if (!showHidden && (n.startsWith('\$') || n.startsWith('.'))) return false;
        return true;
      }).toList();
      list.sort((a, b) {
        bool ad = a is Directory;
        bool bd = b is Directory;
        if (ad && !bd) return -1;
        if (!ad && bd) return 1;
        return path.basename(a.path).compareTo(path.basename(b.path));
      });
      return list;
    } catch (e) {
      return [];
    }
  }

  Future<void> openInExplorer(String filePath) async {
    await Process.run('explorer.exe', ["/select,\"$filePath\""], runInShell: true);
  }

  Future<void> open(String filePath) async {
    await Process.run('cmd', ['/c', 'start', '', filePath]);
  }
}