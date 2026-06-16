import 'dart:io';

class AppSettings {
  // 配置文件名、键、默认值（和 Java 完全一致）
  static const String _settingsFile = "linkdisk_settings.properties";
  static const String _keyReceiveDir = "receiveDir";
  static const String _defaultReceiveDir = "received_files";

  // 获取接收目录（自动创建文件夹）
  static String getReceiveDir() {
    final props = _loadProperties();
    String? receiveDir = props[_keyReceiveDir];

    // 空值使用默认目录
    if (receiveDir == null || receiveDir.trim().isEmpty) {
      receiveDir = _defaultReceiveDir;
    }

    // 自动创建目录
    final folder = Directory(receiveDir);
    if (!folder.existsSync()) {
      folder.createSync(recursive: true);
    }

    return folder.path;
  }

  // 设置接收目录（自动保存到配置文件）
  static void setReceiveDir(String receiveDir) {
    if (receiveDir.trim().isEmpty) return;

    // 自动创建目录
    final folder = Directory(receiveDir);
    if (!folder.existsSync()) {
      folder.createSync(recursive: true);
    }

    final props = _loadProperties();
    props[_keyReceiveDir] = folder.absolute.path;
    _saveProperties(props);
  }

  // 加载 properties 文件
  static Map<String, String> _loadProperties() {
    final file = File(_settingsFile);
    if (!file.existsSync()) return {};

    try {
      final lines = file.readAsLinesSync();
      return _parseProperties(lines);
    } catch (e) {
      print(e);
      return {};
    }
  }

  // 保存 properties 文件
  static void _saveProperties(Map<String, String> props) {
    final file = File(_settingsFile);
    try {
      final buffer = StringBuffer();
      buffer.writeln("# LinkDisk Settings");

      props.forEach((key, value) {
        buffer.writeln("$key=$value");
      });

      file.writeAsStringSync(buffer.toString());
    } catch (e) {
      print(e);
    }
  }

  // 解析 .properties 格式（支持注释、空行）
  static Map<String, String> _parseProperties(List<String> lines) {
    final map = <String, String>{};
    for (final line in lines) {
      final trimmed = line.trim();
      if (trimmed.isEmpty || trimmed.startsWith('#') || trimmed.startsWith('!')) {
        continue;
      }
      final index = trimmed.indexOf('=');
      if (index == -1) continue;
      final key = trimmed.substring(0, index).trim();
      final value = trimmed.substring(index + 1).trim();
      map[key] = value;
    }
    return map;
  }
}