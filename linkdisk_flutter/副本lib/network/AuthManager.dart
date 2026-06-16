import 'dart:convert';
import 'dart:io';

class AuthManager {
  // 文件名和 Java 完全一致
  static const String _trustFile = "trusted_devices.dat";
  final Set<String> _trustedDevices = {};

  // 构造函数：加载信任列表
  AuthManager() {
    _loadTrustedDevices();
  }

  // 判断设备是否可信
  bool isTrusted(String ip) {
    return _trustedDevices.contains(ip);
  }

  // 添加信任设备
  void addTrustedDevice(String ip) {
    _trustedDevices.add(ip);
    _saveTrustedDevices();
    print("已添加信任设备：$ip");
  }

  // 移除信任设备
  void removeTrustedDevice(String ip) {
    _trustedDevices.remove(ip);
    _saveTrustedDevices();
    print("已移除信任设备：$ip");
  }

  // 获取所有信任设备（返回 List）
  List<String> getAllTrustedDevices() {
    return List.unmodifiable(_trustedDevices);
  }

  // 加载信任设备（替代 Java 序列化）
  void _loadTrustedDevices() {
    final file = File(_trustFile);
    if (!file.existsSync()) {
      return;
    }

    try {
      final jsonString = file.readAsStringSync();
      final List<dynamic> jsonList = jsonDecode(jsonString);
      _trustedDevices.addAll(List<String>.from(jsonList));
      print("加载信任设备：$_trustedDevices");
    } catch (e) {
      print("加载信任设备失败，将使用空列表");
      _trustedDevices.clear();
    }
  }

  // 保存信任设备（JSON 序列化）
  void _saveTrustedDevices() {
    final file = File(_trustFile);
    try {
      final jsonString = jsonEncode(_trustedDevices.toList());
      file.writeAsStringSync(jsonString);
    } catch (e) {
      print(e);
    }
  }
}