import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path/path.dart' as path;
import '../../../core/platform_utils.dart';
import '../../../core/file_utils.dart';
import '../../../core/network_utils.dart';
import '../../../widgets/common_widgets.dart';
import '../business/transfer_service.dart';
import '../business/windows_file_service.dart';
import '../business/android_file_service.dart';

class FileManagerPage extends StatefulWidget {
  const FileManagerPage({super.key});

  @override
  State<FileManagerPage> createState() => _FileManagerPageState();
}

class _FileManagerPageState extends State<FileManagerPage> {
  final WindowsFileService winFs = WindowsFileService();
  final AndroidFileService andFs = AndroidFileService();
  final TransferService transfer = TransferService();

  String currentPath = "";
  List<FileSystemEntity> files = [];
  List<Map<String, String>> devices = [];
  bool isScanning = false;
  bool lanMode = false;
  String? targetIp;
  String? selectedPath;
  bool showHidden = false;
  String viewMode = "grid";

  @override
  void initState() {
    super.initState();
    if (PlatformUtils.isWindows) {
      winFs.initDrives().then((_) {
        if (winFs.drives.isNotEmpty) {
          currentPath = "${winFs.drives.first}:/";
          reload();
        }
      });
    }
    NetworkUtils.startBroadcastServer((ip, name) {
      if (!devices.any((d) => d["ip"] == ip)) {
        setState(() => devices.add({"ip": ip, "name": name}));
      }
    });
    NetworkUtils.broadcast();
    TransferService.startReceiver((msg) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    });
  }

  Future<void> reload() async {
    if (PlatformUtils.isWindows) {
      final list = await winFs.load(currentPath, showHidden: showHidden);
      setState(() => files = list);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Row(
        children: [
          // 左边栏
          Container(
            width: 240,
            child: Column(
              children: [
                SizedBox(height: 20),
                Text("此电脑", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ...winFs.drives.map((d) => ListTile(
                      title: Text("$d 盘"),
                      onTap: () {
                        setState(() {
                          currentPath = "$d:/";
                          lanMode = false;
                        });
                        reload();
                      },
                    )),
                Divider(),
                Text("局域网设备"),
                ListTile(
                  title: Text("扫描"),
                  onTap: () async {
                    setState(() {
                      isScanning = true;
                      devices.clear();
                    });
                    await NetworkUtils.broadcast();
                    await Future.delayed(Duration(seconds: 8));
                    setState(() => isScanning = false);
                  },
                ),
                ...devices.map((d) => ListTile(
                      title: Text(d["name"]!),
                      subtitle: Text(d["ip"]!),
                      onTap: () {
                        setState(() {
                          lanMode = true;
                          targetIp = d["ip"];
                        });
                      },
                    )),
              ],
            ),
          ),
          VerticalDivider(),
          // 主内容
          Expanded(
            child: Column(
              children: [
                // 顶部栏
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Row(
                    children: [
                      IconButton(
                        icon: Icon(Icons.arrow_back),
                        onPressed: () {
                          if (lanMode) {
                            setState(() => lanMode = false);
                          } else {
                            final p = Directory(currentPath).parent.path;
                            setState(() => currentPath = p);
                            reload();
                          }
                        },
                      ),
                      Expanded(child: Text(currentPath)),
                      PopupMenuButton(
                        onSelected: (v) {
                          if (v == "hidden") setState(() => showHidden = !showHidden);
                          if (v == "list") setState(() => viewMode = "list");
                          if (v == "grid") setState(() => viewMode = "grid");
                          reload();
                        },
                        itemBuilder: (_) => [
                          PopupMenuItem(child: Text("显示隐藏文件"), value: "hidden"),
                          PopupMenuItem(child: Text("列表视图"), value: "list"),
                          PopupMenuItem(child: Text("网格视图"), value: "grid"),
                        ],
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: lanMode
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              ElevatedButton(
                                onPressed: transfer.pick,
                                child: Text("选择文件"),
                              ),
                              SizedBox(height: 20),
                              ElevatedButton(
                                onPressed: () => transfer.send(
                                  targetIp!,
                                  (p) => setState(() => transfer.progress = p),
                                  (msg) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg))),
                                ),
                                child: Text("发送到 $targetIp"),
                              ),
                              if (transfer.sending)
                                Padding(
                                  padding: const EdgeInsets.all(16.0),
                                  child: LinearProgressIndicator(value: transfer.progress),
                                ),
                            ],
                          ),
                        )
                      : viewMode == "grid"
                          ? GridView.builder(
                              padding: EdgeInsets.all(12),
                              gridDelegate: SliverGridDelegateWithMaxCrossAxisExtent(
                                maxCrossAxisExtent: 240,
                                mainAxisExtent: 60,
                              ),
                              itemCount: files.length,
                              itemBuilder: (_, i) {
                                var f = files[i];
                                return CommonWidgets.gridItem(
                                  context,
                                  f,
                                  selectedPath,
                                  (p) => setState(() => selectedPath = p),
                                  (p) {
                                    if (f is Directory) {
                                      setState(() => currentPath = p);
                                      reload();
                                    } else {
                                      winFs.open(p);
                                    }
                                  },
                                  (p) => CommonWidgets.showFileMenu(
                                    context,
                                    p,
                                    (p) {},
                                    (p) {},
                                    (p) {},
                                    (p) {},
                                    winFs.openInExplorer,
                                  ),
                                );
                              },
                            )
                          : ListView.builder(
                              itemCount: files.length,
                              itemBuilder: (_, i) {
                                var f = files[i];
                                return CommonWidgets.listItem(
                                  context,
                                  f,
                                  selectedPath,
                                  (p) => setState(() => selectedPath = p),
                                  (p) {
                                    if (f is Directory) {
                                      setState(() => currentPath = p);
                                      reload();
                                    } else {
                                      winFs.open(p);
                                    }
                                  },
                                  (p) => CommonWidgets.showFileMenu(
                                    context,
                                    p,
                                    (p) {},
                                    (p) {},
                                    (p) {},
                                    (p) {},
                                    winFs.openInExplorer,
                                  ),
                                );
                              },
                            ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}