import 'package:flutter/material.dart';
import 'dart:io';
import 'dart:convert';
import 'dart:async';
import 'package:file_picker/file_picker.dart';
import 'package:path/path.dart' as path;

void main() {
  runApp(const MaterialApp(
    title: 'LinkDisk 局域网文件管理器',
    debugShowCheckedModeBanner: false,
    home: FileManagerPage(),
  ));
}

class FileManagerPage extends StatefulWidget {
  const FileManagerPage({super.key});

  @override
  State<FileManagerPage> createState() => _FileManagerPageState();
}

enum ClipType { none, copy, cut }

class _FileManagerPageState extends State<FileManagerPage> {
  String currentPath = '';
  List<FileSystemEntity> localFiles = [];
  List<String> drives = [];
  bool isScanning = false;
  List<Map<String, String>> lanDevices = [];
  String? selectedLanIp;
  bool isLanMode = false;
  List<PlatformFile> selectedFiles = [];
  double sendProgress = 0;
  bool isSending = false;
  static const int udpPort = 32000;
  static const int filePort = 32001;
  late String localIp = '';
  bool _isBroadcasting = false;
  Timer? _broadcastTimer;
  bool showHiddenFiles = false;
  String viewMode = "grid";
  ClipType currentClipType = ClipType.none;
  List<String> clipPaths = [];
  String? selectItemPath;
  final List<String> _riskyExtensions = ['.exe', '.ps1', '.bat', '.cmd', '.reg'];
  bool blockRiskyOpen = true;
  final List<String> _systemProtectPaths = [
    r"C:\Windows",
    r"C:\Program Files",
    r"C:\Program Files (x86)",
    r"C:\Users\Administrator",
    r"C:\Users\Public",
    r"C:\$Recycle.Bin",
    r"C:\System Volume Information",
    r"C:\ProgramData",
  ];

  bool isSystemProtectPath(String targetPath) {
    String absPath = targetPath.toLowerCase();
    for (String safePath in _systemProtectPaths) {
      if (absPath.startsWith(safePath.toLowerCase())) return true;
    }
    return false;
  }

  @override
  void initState() {
    super.initState();
    _initLocalIp();
    initLocalDisk();
    startLanServer();
    startContinuousBroadcast();
    startFileReceiver();
  }

  @override
  void dispose() {
    _isBroadcasting = false;
    _broadcastTimer?.cancel();
    super.dispose();
  }

  Future<void> startJavaLinkDisk() async {
    try {
      final String batFullPath = path.join(Directory.current.path, "src", "LinkDisk", "load.bat");

      Process.start(
        "cmd.exe",
        ["/c", "start", "", batFullPath],
        runInShell: true,
      );

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("✅ 已启动 Java 文件传输程序")),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("❌ 启动失败：${e.toString()}")),
      );
    }
  }

  Future<void> _initLocalIp() async {
    try {
      final interfaces = await NetworkInterface.list(
        type: InternetAddressType.IPv4,
        includeLoopback: false,
      );
      for (var interface in interfaces) {
        for (var addr in interface.addresses) {
          if (!addr.isLoopback &&
              (addr.address.startsWith('192.168.') ||
                  addr.address.startsWith('10.') ||
                  addr.address.startsWith('172.'))) {
            setState(() => localIp = addr.address);
            return;
          }
        }
      }
    } catch (e) {}
  }

  void startLanServer() {
    RawDatagramSocket.bind(InternetAddress.anyIPv4, udpPort).then((socket) {
      socket.listen((event) {
        if (event == RawSocketEvent.read) {
          Datagram? dg = socket.receive();
          if (dg != null) {
            String ip = dg.address.address;
            String name = utf8.decode(dg.data);
            if (ip == localIp || ip == InternetAddress.loopbackIPv4.address) return;
            bool exist = lanDevices.any((e) => e['ip'] == ip);
            if (!exist) setState(() => lanDevices.add({"name": name, "ip": ip}));
          }
        }
      });
    });
  }

  void startContinuousBroadcast() {
    if (_isBroadcasting) return;
    _isBroadcasting = true;
    _broadcastTimer = Timer.periodic(const Duration(seconds: 3), (_) => _sendSingleBroadcast());
  }

  Future<void> _sendSingleBroadcast() async {
    if (localIp.isEmpty) return;
    try {
      final socket = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
      socket.broadcastEnabled = true;
      socket.send(utf8.encode("MyPC_$localIp"), InternetAddress("255.255.255.255"), udpPort);
      socket.close();
    } catch (e) {}
  }

  Future<void> scanLan() async {
    setState(() {
      isScanning = true;
      lanDevices.clear();
    });
    _sendSingleBroadcast();
    await Future.delayed(const Duration(seconds: 8));
    setState(() => isScanning = false);
  }

  Future<void> initLocalDisk() async {
    drives.clear();
    for (int i = 67; i <= 90; i++) {
      String letter = String.fromCharCode(i);
      Directory dir = Directory("$letter:/");
      if (await dir.exists()) drives.add(letter);
    }
    if (drives.isNotEmpty) openDisk(drives.first);
    setState(() {});
  }

  Future<void> openDisk(String letter) async {
    currentPath = "$letter:/";
    selectItemPath = null;
    await loadLocalFiles(currentPath);
  }

  Future<void> loadLocalFiles(String dirPath) async {
    try {
      Directory dir = Directory(dirPath);
      List<FileSystemEntity> list = await dir.list().toList();
      list = list.where((e) {
        String name = path.basename(e.path);
        if (!showHiddenFiles) {
          if (name.startsWith(r'$') || name.startsWith('.')) return false;
        }
        return true;
      }).toList();
      list.sort((a, b) {
        bool aDir = a is Directory;
        bool bDir = b is Directory;
        if (aDir && !bDir) return -1;
        if (!aDir && bDir) return 1;
        return path.basename(a.path).compareTo(path.basename(b.path));
      });
      localFiles = list;
      setState(() {});
    } catch (e) {
      localFiles.clear();
      setState(() {});
    }
  }

  void goBack() {
    if (isLanMode) return;
    Directory parent = Directory(currentPath).parent;
    currentPath = parent.path;
    selectItemPath = null;
    loadLocalFiles(currentPath);
  }

  void connectLan(String ip) {
    selectedLanIp = ip;
    isLanMode = true;
    setState(() {});
  }

  void backToLocal() {
    isLanMode = false;
    selectedLanIp = null;
    setState(() {});
  }

  String formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
    return '${(bytes / 1024 / 1024 / 1024).toStringAsFixed(1)} GB';
  }

  String formatDate(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, "0")}-${dt.day.toString().padLeft(2, "0")} ${dt.hour.toString().padLeft(2, "0")}:${dt.minute.toString().padLeft(2, "0")}';
  }

  void setClipData(String filePath, ClipType type) {
    if (type == ClipType.cut && isSystemProtectPath(filePath)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("⚠️ 系统保护目录，禁止剪切移动")));
      return;
    }
    setState(() {
      currentClipType = type;
      clipPaths = [filePath];
    });
    String tip = type == ClipType.copy ? "已复制" : "已剪切";
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("$tip：${path.basename(filePath)}")));
  }

  Future<void> pasteFile() async {
    if (currentClipType == ClipType.none || clipPaths.isEmpty) return;
    for (String srcPath in clipPaths) {
      String fileName = path.basename(srcPath);
      String destPath = path.join(currentPath, fileName);
      int idx = 1;
      while (await File(destPath).exists() || await Directory(destPath).exists()) {
        String nameNoExt = path.basenameWithoutExtension(fileName);
        String ext = path.extension(fileName);
        destPath = path.join(currentPath, "$nameNoExt($idx)$ext");
        idx++;
      }
      if (await FileSystemEntity.type(srcPath) == FileSystemEntityType.file) {
        await File(srcPath).copy(destPath);
      } else {
        await _copyDir(Directory(srcPath), Directory(destPath));
      }
      if (currentClipType == ClipType.cut) {
        if (isSystemProtectPath(srcPath)) continue;
        await FileSystemEntity.type(srcPath) == FileSystemEntityType.file
            ? await File(srcPath).delete()
            : await Directory(srcPath).delete(recursive: true);
      }
    }
    setState(() {
      currentClipType = ClipType.none;
      clipPaths.clear();
    });
    loadLocalFiles(currentPath);
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("粘贴完成")));
  }

  Future<void> _copyDir(Directory src, Directory dest) async {
    await dest.create(recursive: true);
    await for (var entity in src.list()) {
      String newPath = path.join(dest.path, path.basename(entity.path));
      if (entity is File) {
        await entity.copy(newPath);
      } else if (entity is Directory) {
        await _copyDir(entity, Directory(newPath));
      }
    }
  }

  Future<void> deleteFile(String filePath) async {
    if (isSystemProtectPath(filePath)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("❌ 禁止删除系统保护文件/目录")));
      return;
    }
    bool? confirm = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("确认删除"),
        content: const Text("删除后不可恢复，确定继续？"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("取消")),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text("删除", style: TextStyle(color: Colors.red))),
        ],
      ),
    );
    if (confirm != true) return;
    try {
      final type = await FileSystemEntity.type(filePath);
      if (type == FileSystemEntityType.file) {
        await File(filePath).delete();
      } else {
        await Directory(filePath).delete(recursive: true);
      }
      loadLocalFiles(currentPath);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("删除成功")));
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("删除失败：$e")));
    }
  }

  Future<void> renameFile(String oldPath) async {
    if (isSystemProtectPath(oldPath)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("❌ 禁止修改系统保护目录名称")));
      return;
    }
    final ctrl = TextEditingController(text: path.basename(oldPath));
    String? newName = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("重命名"),
        content: TextField(controller: ctrl, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("取消")),
          TextButton(onPressed: () => Navigator.pop(ctx, ctrl.text.trim()), child: const Text("确定")),
        ],
      ),
    );
    if (newName == null || newName.isEmpty) return;
    String newPath = path.join(path.dirname(oldPath), newName);
    if (await File(newPath).exists() || await Directory(newPath).exists()) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("名称已存在")));
      return;
    }
    await FileSystemEntity.type(oldPath) == FileSystemEntityType.file
        ? await File(oldPath).rename(newPath)
        : await Directory(oldPath).rename(newPath);
    loadLocalFiles(currentPath);
  }

  Future<void> createNewFolder() async {
    final ctrl = TextEditingController(text: "新建文件夹");
    String? name = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("新建文件夹"),
        content: TextField(controller: ctrl, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("取消")),
          TextButton(onPressed: () => Navigator.pop(ctx, ctrl.text.trim()), child: const Text("创建")),
        ],
      ),
    );
    if (name == null || name.isEmpty) return;
    await Directory(path.join(currentPath, name)).create();
    loadLocalFiles(currentPath);
  }

  Future<void> openWithSelect(String filePath) async {
    if (isSystemProtectPath(filePath)) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("⚠️ 系统保护文件，禁止操作")));
      return;
    }

    String ext = path.extension(filePath).toLowerCase();
    if (blockRiskyOpen && _riskyExtensions.contains(ext)) {
      bool? allow = await showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text("警告"),
          content: Text("此文件类型($ext)存在安全风险，是否确定打开？"),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("取消")),
            TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text("确定", style: TextStyle(color: Colors.red))),
          ],
        ),
      );
      if (allow != true) return;
    }

    try {
      Process.run('cmd', ['/c', 'start', '', filePath]);
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("唤起打开方式失败")));
    }
  }

  Future<void> openLocationInExplorer(String filePath) async {
    try {
      await Process.run(
        'explorer.exe',
        ['/select,', filePath],
        runInShell: true,
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("打开失败")));
    }
  }

  void showCommonFileMenu(String filePath) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(12))),
      builder: (_) => SizedBox(
        height: 400,
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              child: Text(
                path.basename(filePath),
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.copy),
              title: const Text("复制"),
              onTap: () {
                Navigator.pop(context);
                setClipData(filePath, ClipType.copy);
              },
            ),
            ListTile(
              leading: const Icon(Icons.cut),
              title: const Text("剪切"),
              onTap: () {
                Navigator.pop(context);
                setClipData(filePath, ClipType.cut);
              },
            ),
            ListTile(
              leading: const Icon(Icons.edit),
              title: const Text("重命名"),
              onTap: () {
                Navigator.pop(context);
                renameFile(filePath);
              },
            ),
            ListTile(
              leading: const Icon(Icons.delete, color: Colors.red),
              title: const Text("删除", style: TextStyle(color: Colors.red)),
              onTap: () {
                Navigator.pop(context);
                deleteFile(filePath);
              },
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.folder_open),
              title: const Text("在资源管理器中打开"),
              onTap: () {
                Navigator.pop(context);
                openLocationInExplorer(filePath);
              },
            ),
            ListTile(
              leading: const Icon(Icons.open_in_new),
              title: const Text("打开方式"),
              onTap: () {
                Navigator.pop(context);
                openWithSelect(filePath);
              },
            ),
            ListTile(
              leading: const Icon(Icons.info),
              title: const Text("属性"),
              onTap: () {
                Navigator.pop(context);
                showFileProperties(filePath);
              },
            ),
          ],
        ),
      ),
    );
  }

  void showFileProperties(String filePath) {
    FileStat stat = File(filePath).statSync();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("文件属性"),
        content: SizedBox(
          width: 400,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text("路径：$filePath"),
              const SizedBox(height: 8),
              Text("类型：${stat.type == FileSystemEntityType.file ? '文件' : '文件夹'}"),
              const SizedBox(height: 8),
              if (stat.type == FileSystemEntityType.file)
                Text("大小：${formatFileSize(stat.size)}"),
              const SizedBox(height: 8),
              Text("创建时间：${formatDate(stat.changed)}"),
              const SizedBox(height: 8),
              Text("修改时间：${formatDate(stat.modified)}"),
              const SizedBox(height: 8),
              Text("访问时间：${formatDate(stat.accessed)}"),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("确定")),
        ],
      ),
    );
  }

  Future<void> pickFiles() async {
    FilePickerResult? res = await FilePicker.platform.pickFiles(allowMultiple: true);
    if (res != null) setState(() => selectedFiles = res.files);
  }

  Future<void> sendFiles() async {
    if (selectedFiles.isEmpty || selectedLanIp == null) return;
    setState(() {
      isSending = true;
      sendProgress = 0;
    });
    try {
      final socket = await Socket.connect(selectedLanIp!, filePort, timeout: const Duration(seconds: 5));
      int total = selectedFiles.fold(0, (sum, f) => sum + f.size);
      int sent = 0;
      socket.write(utf8.encode("FILE_COUNT:${selectedFiles.length}\n"));
      await socket.flush();
      for (final file in selectedFiles) {
        socket.write(utf8.encode("FILE_NAME:${file.name}\n"));
        await socket.flush();
        socket.write(utf8.encode("FILE_SIZE:${file.size}\n"));
        await socket.flush();
        final bytes = await File(file.path!).readAsBytes();
        socket.add(bytes);
        await socket.flush();
        sent += file.size;
        setState(() => sendProgress = sent / total);
      }
      socket.destroy();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("✅ 发送完成")));
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("❌ 发送失败：$e")));
    }
    setState(() => isSending = false);
  }

  Future<void> startFileReceiver() async {
    final server = await ServerSocket.bind(InternetAddress.anyIPv4, filePort);
    server.listen(handleReceive);
  }

  void handleReceive(Socket socket) {
    String buffer = "";
    int? fileCount;
    int received = 0;
    String? fileName;
    int? fileSize;
    List<int> bytes = [];
    socket.listen((data) {
      buffer += utf8.decode(data, allowMalformed: true);
      while (buffer.contains("\n")) {
        final lineEnd = buffer.indexOf("\n");
        final line = buffer.substring(0, lineEnd).trim();
        buffer = buffer.substring(lineEnd + 1);
        if (line.startsWith("FILE_COUNT:")) {
          fileCount = int.parse(line.split(":")[1]);
        } else if (line.startsWith("FILE_NAME:")) {
          fileName = line.split(":")[1];
          bytes.clear();
        } else if (line.startsWith("FILE_SIZE:")) {
          fileSize = int.parse(line.split(":")[1]);
        } else if (fileName != null && fileSize != null) {
          bytes.addAll(data);
          if (bytes.length >= fileSize!) {
            saveFile(fileName!, bytes);
            received++;
            fileName = null;
            fileSize = null;
            if (received >= fileCount!) socket.destroy();
          }
        }
      }
    });
  }

  void saveFile(String name, List<int> bytes) {
    final dir = Directory("D:\\LinkDisk_Received");
    if (!dir.existsSync()) dir.createSync(recursive: true);
    File(path.join(dir.path, name)).writeAsBytesSync(bytes);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("📥 已接收：$name")));
  }

  @override
  Widget build(BuildContext context) {
    final pathController = TextEditingController(text: currentPath);
    return Scaffold(
      body: Row(
        children: [
          Container(
            width: 240,
            color: Colors.white,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text("此电脑", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
                for (String drive in drives)
                  ListTile(
                    leading: const Icon(Icons.storage),
                    title: Text("$drive 盘"),
                    onTap: () {
                      backToLocal();
                      openDisk(drive);
                    },
                  ),
                const Divider(height: 20),
                const Padding(
                  padding: EdgeInsets.all(16),
                  child: Text("局域网设备", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                ),
                ListTile(
                  leading: const Icon(Icons.launch),
                  title: const Text("启动 Java 文件传输"),
                  onTap: startJavaLinkDisk,
                ),
                for (var dev in lanDevices)
                  ListTile(
                    leading: const Icon(Icons.computer),
                    title: Text(dev['name']!),
                    subtitle: Text(dev['ip']!),
                    onTap: () => connectLan(dev['ip']!),
                  ),
              ],
            ),
          ),
          const VerticalDivider(width: 1),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      if (!isLanMode)
                        IconButton(onPressed: goBack, icon: const Icon(Icons.arrow_back)),
                      if (isLanMode)
                        IconButton(onPressed: backToLocal, icon: const Icon(Icons.arrow_back)),
                      if (!isLanMode)
                        SizedBox(
                          width: MediaQuery.of(context).size.width * 0.35,
                          height: 28,
                          child: Align(
                            alignment: Alignment.centerLeft,
                            child: TextField(
                              controller: pathController,
                              decoration: const InputDecoration(
                                border: OutlineInputBorder(),
                                hintText: "输入路径跳转",
                                contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                isDense: true,
                              ),
                              onSubmitted: (value) {
                                if (Directory(value).existsSync()) {
                                  currentPath = value;
                                  selectItemPath = null;
                                  loadLocalFiles(value);
                                } else {
                                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("路径不存在")));
                                }
                              },
                            ),
                          ),
                        ),
                      if (isLanMode)
                        Expanded(
                          child: Text("局域网：$selectedLanIp", style: const TextStyle(fontSize: 13)),
                        ),
                      const Spacer(),
                      if (!isLanMode)
                        Row(
                          children: [
                            if (currentClipType != ClipType.none)
                              TextButton(onPressed: pasteFile, child: const Text("粘贴")),
                            IconButton(
                              onPressed: createNewFolder,
                              icon: const Icon(Icons.create_new_folder_outlined),
                            ),
                            PopupMenuButton<String>(
                              icon: const Icon(Icons.more_vert),
                              onSelected: (v) {
                                if (v == "hidden") {
                                  setState(() {
                                    showHiddenFiles = !showHiddenFiles;
                                    loadLocalFiles(currentPath);
                                  });
                                } else if (v == "list") {
                                  setState(() => viewMode = "list");
                                } else if (v == "grid") {
                                  setState(() => viewMode = "grid");
                                } else if (v == "risky") {
                                  setState(() => blockRiskyOpen = !blockRiskyOpen);
                                }
                              },
                              itemBuilder: (_) => [
                                PopupMenuItem(
                                  value: "hidden",
                                  child: Row(
                                    children: [
                                      const Text("显示隐藏文件"),
                                      if (showHiddenFiles) const Icon(Icons.check, size: 16, color: Colors.blue)
                                    ],
                                  ),
                                ),
                                const PopupMenuDivider(),
                                PopupMenuItem(
                                  value: "list",
                                  child: Row(
                                    children: [
                                      const Text("列表视图"),
                                      if (viewMode == "list") const Icon(Icons.check, size: 16, color: Colors.blue)
                                    ],
                                  ),
                                ),
                                PopupMenuItem(
                                  value: "grid",
                                  child: Row(
                                    children: [
                                      const Text("图标视图"),
                                      if (viewMode == "grid") const Icon(Icons.check, size: 16, color: Colors.blue)
                                    ],
                                  ),
                                ),
                                const PopupMenuDivider(),
                                PopupMenuItem(
                                  value: "risky",
                                  child: Row(
                                    children: [
                                      const Text("允许打开高危文件"),
                                      if (!blockRiskyOpen) const Icon(Icons.check, size: 16, color: Colors.blue)
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                    ],
                  ),
                ),
                Expanded(
                  child: isLanMode
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const Icon(Icons.cloud, size: 48, color: Colors.blue),
                              const SizedBox(height: 16),
                              const Text("已连接局域网设备"),
                              const SizedBox(height: 8),
                              ElevatedButton(onPressed: pickFiles, child: const Text("选择文件")),
                              const SizedBox(height: 16),
                              if (selectedFiles.isNotEmpty)
                                Column(
                                  children: [
                                    const Text("已选择文件："),
                                    for (var file in selectedFiles)
                                      ListTile(
                                        title: Text(file.name),
                                        subtitle: Text("${(file.size / 1024).toStringAsFixed(1)} KB"),
                                      ),
                                    const SizedBox(height: 8),
                                    if (isSending)
                                      Column(
                                        children: [
                                          LinearProgressIndicator(value: sendProgress),
                                          Text("${(sendProgress * 100).toStringAsFixed(0)}%"),
                                        ],
                                      )
                                    else
                                      ElevatedButton(onPressed: sendFiles, child: const Text("发送文件")),
                                  ],
                                ),
                            ],
                          ),
                        )
                      : viewMode == "grid"
                          ? GridView.builder(
                              padding: const EdgeInsets.all(12),
                              gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                                maxCrossAxisExtent: 240,
                                mainAxisExtent: 60,
                                crossAxisSpacing: 12,
                                mainAxisSpacing: 12,
                              ),
                              itemCount: localFiles.length,
                              itemBuilder: (ctx, i) {
                                var entity = localFiles[i];
                                bool isDir = entity is Directory;
                                String name = path.basename(entity.path);
                                bool isSelected = selectItemPath == entity.path;
                                return GestureDetector(
                                  onTap: () => setState(() => selectItemPath = entity.path),
                                  onDoubleTap: () async {
                                    if (isDir) {
                                      currentPath = entity.path;
                                      selectItemPath = null;
                                      loadLocalFiles(currentPath);
                                    } else {
                                      await openWithSelect(entity.path);
                                    }
                                  },
                                  onLongPress: () => showCommonFileMenu(entity.path),
                                  onSecondaryTap: () => showCommonFileMenu(entity.path),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 12),
                                    decoration: BoxDecoration(
                                      color: isSelected ? Colors.blue.shade50 : Colors.white,
                                      borderRadius: BorderRadius.circular(8),
                                      border: Border.all(color: isSelected ? Colors.blueAccent : Colors.grey.shade200),
                                    ),
                                    child: Row(
                                      children: [
                                        Icon(isDir ? Icons.folder : Icons.insert_drive_file),
                                        const SizedBox(width: 12),
                                        Expanded(child: Text(name, overflow: TextOverflow.ellipsis)),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            )
                          : ListView.builder(
                              padding: const EdgeInsets.all(12),
                              itemCount: localFiles.length,
                              itemBuilder: (ctx, i) {
                                var entity = localFiles[i];
                                bool isDir = entity is Directory;
                                String name = path.basename(entity.path);
                                FileStat stat = entity.statSync();
                                String info = isDir ? formatDate(stat.modified) : "${formatFileSize(stat.size)} | ${formatDate(stat.modified)}";
                                bool isSelected = selectItemPath == entity.path;
                                return GestureDetector(
                                  onTap: () => setState(() => selectItemPath = entity.path),
                                  onDoubleTap: () async {
                                    if (isDir) {
                                      currentPath = entity.path;
                                      selectItemPath = null;
                                      loadLocalFiles(currentPath);
                                    } else {
                                      await openWithSelect(entity.path);
                                    }
                                  },
                                  onLongPress: () => showCommonFileMenu(entity.path),
                                  onSecondaryTap: () => showCommonFileMenu(entity.path),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                                    margin: const EdgeInsets.only(bottom: 6),
                                    decoration: BoxDecoration(
                                      color: isSelected ? Colors.blue.shade50 : Colors.white,
                                      borderRadius: BorderRadius.circular(8),
                                      border: Border.all(color: isSelected ? Colors.blueAccent : Colors.grey.shade200),
                                    ),
                                    child: Row(
                                      children: [
                                        Icon(isDir ? Icons.folder : Icons.insert_drive_file),
                                        const SizedBox(width: 12),
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(name, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14)),
                                              const SizedBox(height: 2),
                                              Text(info, style: const TextStyle(fontSize: 11, color: Colors.grey)),
                                            ],
                                          ),
                                        ),
                                      ],
                                    ),
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