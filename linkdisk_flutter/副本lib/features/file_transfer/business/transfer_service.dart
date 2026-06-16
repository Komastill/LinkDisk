import 'dart:io';
import 'dart:convert';
import 'package:file_picker/file_picker.dart';
import '../../../core/network_utils.dart';

class TransferService {
  List<PlatformFile> files = [];
  double progress = 0;
  bool sending = false;

  Future<void> pick() async {
    final res = await FilePicker.platform.pickFiles(allowMultiple: true);
    if (res != null) files = res.files;
  }

  Future<void> send(String ip, Function(double) onProgress, Function(String) onResult) async {
    if (files.isEmpty || ip.isEmpty) return;
    sending = true;
    try {
      final socket = await Socket.connect(ip, NetworkUtils.filePort, timeout: Duration(seconds: 5));
      int total = files.fold(0, (s, e) => s + e.size);
      int sent = 0;
      for (var f in files) {
        socket.writeln("NAME:${f.name}");
        await socket.flush();
        socket.writeln("SIZE:${f.size}");
        await socket.flush();
        final bytes = await File(f.path!).readAsBytes();
        socket.add(bytes);
        await socket.flush();
        sent += f.size;
        onProgress(sent / total);
      }
      socket.close();
      onResult("✅ 发送完成");
    } catch (e) {
      onResult("❌ 失败：$e");
    } finally {
      sending = false;
    }
  }

  static Future<void> startReceiver(Function(String) onReceived) async {
    final server = await ServerSocket.bind(InternetAddress.anyIPv4, NetworkUtils.filePort);
    server.listen((socket) {
      String? name;
      int? size;
      List<int> bytes = [];
      socket.listen((data) {
        if (name == null) {
          String msg = utf8.decode(data).trim();
          if (msg.startsWith("NAME:")) {
            name = msg.split(":")[1];
          } else if (msg.startsWith("SIZE:")) {
            size = int.parse(msg.split(":")[1]);
          }
        } else {
          bytes.addAll(data);
          if (bytes.length >= size!) {
            File("D:\\LinkDisk_Received\\$name").writeAsBytesSync(bytes);
            onReceived("📥 已接收：$name");
            name = null;
            size = null;
            bytes.clear();
          }
        }
      });
    });
  }
}