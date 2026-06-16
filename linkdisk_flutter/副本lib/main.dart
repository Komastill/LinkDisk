import 'package:flutter/material.dart';
import 'dart:io';
import 'dart:convert';
import 'dart:async';
import 'package:path/path.dart' as path;

void main() {
  runApp(const MaterialApp(
    title: '局域网文件传输',
    debugShowCheckedModeBanner: false,
    home: FileTransferPage(),
  ));
}

class FileTransferPage extends StatefulWidget {
  const FileTransferPage({super.key});

  @override
  State<FileTransferPage> createState() => _FileTransferPageState();
}

class _FileTransferPageState extends State<FileTransferPage> {
  // 网络配置
  static const int udpPort = 32000;
  static const int tcpPort = 32001;
  String? localIp;
  List<Map<String, String>> devices = [];
  String? selectedDeviceIp;
  bool isScanning = false;

  // 文件传输
  bool isTransferring = false;
  double transferProgress = 0;
  String transferStatus = "就绪";
  List<String> localFiles = [];
  String currentPath = "";

  // 系统安全
  final List<String> riskyExtensions = ['.exe', '.bat', '.cmd', '.sh', '.apk'];
  final List<String> systemPaths = ['Windows', 'Program Files', 'System32'];

  // 套接字
  RawDatagramSocket? udpSocket;
  ServerSocket? tcpServer;
  StreamSubscription? deviceScanSub;

  @override
  void initState() {
    super.initState();
    _initNetwork();
    _startTcpServer();
    _startUdpListener();
    _loadLocalFiles();
  }

  @override
  void dispose() {
    udpSocket?.close();
    tcpServer?.close();
    deviceScanSub?.cancel();
    super.dispose();
  }

  // 初始化本地IP
  Future<void> _initNetwork() async {
    try {
      for (var interface in await NetworkInterface.list()) {
        for (var addr in interface.addresses) {
          if (addr.type == InternetAddressType.IPv4 &&
              !addr.address.startsWith("127.") &&
              !addr.address.startsWith("169.") &&
              !addr.address.startsWith("::")) {
            setState(() => localIp = addr.address);
            return;
          }
        }
      }
    } catch (e) {
      debugPrint("IP初始化失败: $e");
    }
  }

  // 启动TCP服务端（接收文件）
  Future<void> _startTcpServer() async {
    try {
      tcpServer = await ServerSocket.bind(InternetAddress.anyIPv4, tcpPort);
      tcpServer!.listen((Socket socket) async {
        setState(() => transferStatus = "设备已连接");
        final file = File(path.join(Directory.systemTemp.path, "received_${DateTime.now().millisecondsSinceEpoch}"));
        final sink = file.openWrite();
        int received = 0;
        int total = 0;

        socket.listen((data) {
          if (total == 0) {
            final header = utf8.decode(data.sublist(0, 30)).trim();
            total = int.parse(header.split(',').last);
            setState(() => transferStatus = "开始接收...");
          } else {
            sink.add(data);
            received += data.length;
            setState(() => transferProgress = received / total);
          }
        }, onDone: () {
          sink.close();
          socket.close();
          setState(() {
            transferStatus = "接收完成";
            transferProgress = 0;
            isTransferring = false;
          });
        }, onError: (e) {
          sink.close();
          setState(() => transferStatus = "接收失败");
        });
      });
    } catch (e) {
      debugPrint("TCP服务启动失败: $e");
    }
  }

  // UDP监听（发现设备）
  Future<void> _startUdpListener() async {
    try {
      udpSocket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, udpPort);
      udpSocket!.broadcastEnabled = true;
      udpSocket!.listen((event) {
        final datagram = udpSocket!.receive();
        if (datagram != null) {
          final msg = utf8.decode(datagram.data).trim();
          final ip = datagram.address.address;
          if (msg == "LINKDISK_DEVICE" && ip != localIp) {
            setState(() {
              if (!devices.any((d) => d["ip"] == ip)) {
                devices.add({"name": "设备 $ip", "ip": ip});
              }
            });
          }
        }
      });
    } catch (e) {
      debugPrint("UDP监听失败: $e");
    }
  }

  // 扫描局域网设备
  Future<void> scanDevices() async {
    if (localIp == null) return;
    setState(() {
      devices.clear();
      isScanning = true;
      transferStatus = "扫描中...";
    });

    final prefix = localIp!.substring(0, localIp!.lastIndexOf('.') + 1);
    for (int i = 1; i <= 255; i++) {
      final target = "$prefix$i";
      try {
        final socket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
        socket.broadcastEnabled = true;
        socket.send(utf8.encode("LINKDISK_DEVICE"), InternetAddress(target), udpPort);
        socket.close();
      } catch (_) {}
    }

    await Future.delayed(const Duration(seconds: 2));
    setState(() {
      isScanning = false;
      transferStatus = "扫描完成，共${devices.length}台设备";
    });
  }

  // 加载本地文件
  Future<void> _loadLocalFiles([String? path]) async {
    try {
      currentPath = path ?? Directory.systemTemp.path;
      final dir = Directory(currentPath);
      final list = await dir.list().toList();
      setState(() {
        localFiles = list.map((e) => e.path).toList();
      });
    } catch (e) {
      debugPrint("加载文件失败: $e");
    }
  }

  // 发送文件到目标设备
  Future<void> sendFile(String filePath, String targetIp) async {
    if (isTransferring || targetIp.isEmpty) return;
    final file = File(filePath);
    if (!await file.exists()) return;

    setState(() {
      isTransferring = true;
      transferStatus = "连接中...";
      transferProgress = 0;
    });

    try {
      final socket = await Socket.connect(targetIp, tcpPort, timeout: const Duration(seconds: 3));
      setState(() => transferStatus = "已连接，开始发送");

      final totalBytes = await file.length();
      socket.write(utf8.encode("FILE,${file.path.split('/').last},$totalBytes"));
      await Future.delayed(const Duration(milliseconds: 100));

      final stream = file.openRead();
      int sent = 0;
      stream.listen((data) {
        socket.add(data);
        sent += data.length;
        setState(() => transferProgress = sent / totalBytes);
      }, onDone: () {
        socket.flush();
        socket.close();
        setState(() {
          transferStatus = "发送完成";
          transferProgress = 0;
          isTransferring = false;
        });
      }, onError: (e) {
        socket.close();
        setState(() {
          transferStatus = "发送失败";
          isTransferring = false;
        });
      });
    } catch (e) {
      setState(() {
        transferStatus = "连接失败";
        isTransferring = false;
      });
    }
  }

  // 检查是否为危险文件
  bool _isRiskyFile(String path) {
    return riskyExtensions.any((ext) => path.toLowerCase().endsWith(ext));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("局域网文件传输"),
        actions: [
          if (localIp != null)
            Center(child: Padding(padding: const EdgeInsets.only(right: 16), child: Text("本机IP: $localIp"))),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // 设备扫描
            ElevatedButton(
              onPressed: isScanning ? null : scanDevices,
              child: isScanning ? const Text("扫描中...") : const Text("扫描局域网设备"),
            ),
            const SizedBox(height: 10),

            // 设备列表
            const Text("在线设备:"),
            SizedBox(
              height: 100,
              child: devices.isEmpty
                  ? const Center(child: Text("暂无设备"))
                  : ListView.builder(
                      itemCount: devices.length,
                      itemBuilder: (c, i) {
                        final dev = devices[i];
                        return ListTile(
                          title: Text(dev["name"]!),
                          subtitle: Text(dev["ip"]!),
                          selected: selectedDeviceIp == dev["ip"],
                          onTap: () => setState(() => selectedDeviceIp = dev["ip"]),
                        );
                      },
                    ),
            ),
            const Divider(height: 20),

            // 传输状态
            Text("状态: $transferStatus", textAlign: TextAlign.center),
            if (isTransferring)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: LinearProgressIndicator(value: transferProgress),
              ),
            const Divider(height: 20),

            // 本地文件
            const Text("本地文件:"),
            Expanded(
              child: localFiles.isEmpty
                  ? const Center(child: Text("无文件"))
                  : ListView.builder(
                      itemCount: localFiles.length,
                      itemBuilder: (c, i) {
                        final file = localFiles[i];
                        final name = file.split(Platform.pathSeparator).last;
                        final risky = _isRiskyFile(file);
                        return ListTile(
                          title: Text(name),
                          leading: risky ? const Icon(Icons.warning, color: Colors.red) : const Icon(Icons.file_present),
                          onTap: selectedDeviceIp == null
                              ? null
                              : () => sendFile(file, selectedDeviceIp!),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}