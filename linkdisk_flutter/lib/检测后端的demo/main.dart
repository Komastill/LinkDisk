import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:http/http.dart' as http;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LinkDisk',
      theme: ThemeData(primarySwatch: Colors.blue),
      debugShowCheckedModeBanner: false,
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<Map<String, String>> devices = [];
  String status = "就绪";

  // 启动设备发现
  Future<void> startDiscovery() async {
    setState(() => status = "正在发现设备...");
    try {
      final res = await http.get(Uri.parse("http://127.0.0.1:8080/api/startDiscovery"));
      if (res.statusCode == 200) {
        setState(() => status = "已启动设备发现");
      }
    } catch (e) {
      setState(() => status = "错误：请先启动 Java 后端");
    }
  }

  // 选择文件并发送
  Future<void> sendFile(String ip) async {
    final result = await FilePicker.platform.pickFiles();
    if (result == null) return;

    final path = result.files.single.path!;
    setState(() => status = "正在发送文件...");

    try {
      await http.get(Uri.parse("http://127.0.0.1:8080/api/sendFile?ip=$ip&path=$path"));
      setState(() => status = "已添加发送任务");
    } catch (e) {
      setState(() => status = "发送失败");
    }
  }

  @override
  void initState() {
    super.initState();
    startDiscovery();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("LinkDisk - 局域网文件传输")),
      body: Column(
        children: [
          // 状态栏
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text("状态：$status", style: const TextStyle(fontSize: 16)),
          ),

          // 设备列表
          Expanded(
            child: devices.isEmpty
                ? const Center(child: Text("等待发现设备..."))
                : ListView.builder(
                    itemCount: devices.length,
                    itemBuilder: (ctx, i) {
                      final dev = devices[i];
                      return ListTile(
                        title: Text(dev["name"]!),
                        subtitle: Text("${dev["platform"]} | ${dev["ip"]}"),
                        trailing: ElevatedButton(
                          onPressed: () => sendFile(dev["ip"]!),
                          child: const Text("发送文件"),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}