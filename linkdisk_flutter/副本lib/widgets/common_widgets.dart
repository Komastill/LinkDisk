import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as path;
import '../core/file_utils.dart';

class CommonWidgets {
  static void showFileMenu(
    BuildContext context,
    String path,
    Function(String) onCopy,
    Function(String) onCut,
    Function(String) onRename,
    Function(String) onDelete,
    Function(String) onOpenInExplorer,
  ) {
    showModalBottomSheet(
      context: context,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(12)),
      ),
      builder: (_) => SizedBox(
        height: 300,
        child: Column(
          children: [
            ListTile(
              leading: Icon(Icons.copy),
              title: Text("复制"),
              onTap: () {
                Navigator.pop(context);
                onCopy(path);
              },
            ),
            ListTile(
              leading: Icon(Icons.cut),
              title: Text("剪切"),
              onTap: () {
                Navigator.pop(context);
                onCut(path);
              },
            ),
            ListTile(
              leading: Icon(Icons.edit),
              title: Text("重命名"),
              onTap: () {
                Navigator.pop(context);
                onRename(path);
              },
            ),
            ListTile(
              leading: Icon(Icons.delete, color: Colors.red),
              title: Text("删除", style: TextStyle(color: red)),
              onTap: () {
                Navigator.pop(context);
                onDelete(path);
              },
            ),
            ListTile(
              leading: Icon(Icons.folder_open),
              title: Text("在资源管理器中打开"),
              onTap: () {
                Navigator.pop(context);
                onOpenInExplorer(path);
              },
            ),
          ],
        ),
      ),
    );
  }

  static Widget gridItem(
    BuildContext context,
    FileSystemEntity entity,
    String? selected,
    Function(String) onTap,
    Function(String) onDoubleTap,
    Function(String) onLongPress,
  ) {
    bool isDir = entity is Directory;
    bool sel = selected == entity.path;
    return GestureDetector(
      onTap: () => onTap(entity.path),
      onDoubleTap: () => onDoubleTap(entity.path),
      onLongPress: () => onLongPress(entity.path),
      child: Container(
        decoration: BoxDecoration(
          color: sel ? Colors.blue.shade50 : Colors.white,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: sel ? blue : Colors.grey.shade200),
        ),
        child: Row(
          children: [
            SizedBox(width: 12),
            Icon(isDir ? Icons.folder : Icons.insert_drive_file),
            SizedBox(width: 12),
            Expanded(child: Text(path.basename(entity.path))),
          ],
        ),
      ),
    );
  }

  static Widget listItem(
    BuildContext context,
    FileSystemEntity entity,
    String? selected,
    Function(String) onTap,
    Function(String) onDoubleTap,
    Function(String) onLongPress,
  ) {
    bool isDir = entity is Directory;
    bool sel = selected == entity.path;
    var stat = entity.statSync();
    return GestureDetector(
      onTap: () => onTap(entity.path),
      onDoubleTap: () => onDoubleTap(entity.path),
      onLongPress: () => onLongPress(entity.path),
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: sel ? Colors.blue.shade50 : Colors.white,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: sel ? blue : Colors.grey.shade200),
        ),
        child: Row(
          children: [
            Icon(isDir ? Icons.folder : Icons.insert_drive_file),
            SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(path.basename(entity.path)),
                  SizedBox(height: 2),
                  Text(
                    isDir
                        ? FileUtils.formatDate(stat.modified)
                        : "${FileUtils.formatFileSize(stat.size)} | ${FileUtils.formatDate(stat.modified)}",
                    style: TextStyle(fontSize: 11, color: Colors.grey),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  static const Color blue = Colors.blueAccent;
  static const Color red = Colors.red;
}