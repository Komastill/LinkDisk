import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'dart:io'; // 加上这个就不报错了！
import '../core/network_service.dart';
import '../core/file_service_impl.dart';

class TransferPage extends StatefulWidget {
  const TransferPage({super.key});

  @override
  State<TransferPage> createState() => _TransferPageState();
}

class _TransferPageState extends State<TransferPage> {
  final NetworkService net = NetworkService();
  final FileServiceImpl fileService = FileServiceImpl();
  List<PlatformFile> files = [];
  final ipCtrl = TextEditingController();
  double progress = 0;
  bool isSending = false;
  bool isReceiving = false;
  ServerSocket? server;

  @override
  void initState() {
    super.initState();
    startReceive();
  }

  Future<void> startReceive() async {
    try {
      server = await net.startServer();
      setState(() => isReceiving = true);
      server!.listen((client) async {
        final path = await fileService.getSaveDirectory();
        net.receiveFile(client, path, (name, size) {
          print("正在接收：$name");
        });
      });
    } catch (e) {
      print("启动接收失败：$e");
    }
  }

  Future<void> pickFiles() async {
    final res = await FilePicker.platform.pickFiles(allowMultiple: true);
    if (res != null) {
      setState(() => files = res.files);
    }
  }

  Future<void> send() async {
    final ip = ipCtrl.text.trim();
    if (ip.isEmpty || files.isEmpty) return;

    setState(() {
      isSending = true;
      progress = 0;
    });

    try {
      final socket = await net.connect(ip);
      int total = files.fold(0, (a, b) => a + b.size);
      int sent = 0;

      for (final f in files) {
        if (f.path == null) continue;
        final file = File(f.path!);
        if (!await file.exists()) continue;

        await net.sendFile(socket, file, f.name, f.size, (s) {
          setState(() {
            progress = (sent + s) / total;
          });
        });
        sent += f.size;
      }

      socket.destroy();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("✅ 发送完成")),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("发送失败：$e")),
      );
    } finally {
      setState(() => isSending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("文件传输工具")),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text("1. 选择文件"),
            ElevatedButton(
              onPressed: isSending ? null : pickFiles,
              child: const Text("选择文件"),
            ),
            if (files.isNotEmpty)
              Text("已选：${files.length} 个"),

            const SizedBox(height: 20),
            const Text("2. 输入目标设备IP"),
            TextField(
              controller: ipCtrl,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                hintText: "例如：192.168.1.105",
              ),
            ),

            const SizedBox(height: 20),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
              onPressed: isSending ? null : send,
              child: const Text("发送文件"),
            ),

            if (isSending)
              Column(
                children: [
                  const SizedBox(height: 10),
                  LinearProgressIndicator(value: progress),
                  Text("进度：${(progress * 100).toStringAsFixed(1)}%"),
                ],
              ),

            const SizedBox(height: 20),
            if (isReceiving)
              const Chip(
                label: Text("📥 已启动接收服务"),
                backgroundColor: Colors.lightGreenAccent,
              ),
          ],
        ),
      ),
    );
  }
}