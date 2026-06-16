import 'package:flutter/material.dart';
import 'dart:io';
import 'dart:convert';
import 'dart:async';
import 'package:path/path.dart' as path;
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;

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
  // ======== 本地文件浏览 ========
  String currentPath = '';
  List<FileSystemEntity> localFiles = [];
  List<String> drives = [];
  late String localIp = '';
  bool showHiddenFiles = false;
  String viewMode = "grid";
  ClipType currentClipType = ClipType.none;
  List<String> clipPaths = [];
  String? selectItemPath;
  String? hoverPath;
  String hoverSideItem = '';

  // ======== 本地多选模式 ========
  bool multiSelectMode = false;
  final Set<String> selectedPaths = {};

  // ======== 远程设备浏览 ========
  bool isRemoteMode = false;
  String? remoteIp;
  String remotePath = '';
  List<Map<String, dynamic>> remoteFiles = [];
  List<String> trustedDevices = [];
  String? remoteHoverPath;
  String? remoteSelectPath;

  // ======== 远程多选模式 ========
  bool remoteMultiSelectMode = false;
  final Set<String> remoteSelectedPaths = {};

  // ======== 远程剪贴板（支持批量） ========
  List<String> remoteClipPaths = [];
  bool remoteClipIsCut = false;

  // ======== 安全 ========
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

  // ==========  Java 集成 ==========
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

  Future<void> _addToSendList(List<String> filePaths) async {
    if (filePaths.isEmpty) return;
    const url = 'http://127.0.0.1:8080/api/addFiles';
    final body = jsonEncode({'filePaths': filePaths});
    try {
      final response = await http.post(Uri.parse(url), headers: {'Content-Type': 'application/json'}, body: body);
      if (response.statusCode == 200) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("已添加 ${filePaths.length} 个文件到 Java 传输列表")),
        );
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("添加失败，请检查 Java 程序")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("Java 服务未响应，正在尝试启动...")));
      await startJavaLinkDisk();
      await Future.delayed(const Duration(seconds: 3));
      try {
        final retryResponse = await http.post(Uri.parse(url), headers: {'Content-Type': 'application/json'}, body: body);
        if (retryResponse.statusCode == 200) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text("已添加 ${filePaths.length} 个文件到 Java 传输列表")),
          );
        }
      } catch (_) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("无法连接 Java 服务")));
      }
    }
  }

  // ========== 本地磁盘操作 ==========
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
    selectedPaths.clear();
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
    if (isRemoteMode) {
      if (remotePath.isEmpty) {
        exitRemoteMode();
      } else {
        backRemote();
      }
      return;
    }
    Directory parent = Directory(currentPath).parent;
    currentPath = parent.path;
    selectItemPath = null;
    selectedPaths.clear();
    loadLocalFiles(currentPath);
  }

  // ========== 远程设备操作 ==========
  Future<void> fetchTrustedDevices() async {
    try {
      final response = await http.get(Uri.parse('http://127.0.0.1:8080/api/trustedDevices'));
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        setState(() {
          trustedDevices = List<String>.from(data['devices']);
        });
      }
    } catch (e) {
      print('获取信任设备失败: $e');
    }
  }

  Future<void> connectRemoteDevice(String ip) async {
    setState(() {
      isRemoteMode = true;
      remoteIp = ip;
      remotePath = '';
      remoteFiles.clear();
      remoteMultiSelectMode = false;
      remoteSelectedPaths.clear();
      remoteClipPaths.clear();
    });
    await fetchRemoteDrives();
  }

  Future<void> fetchRemoteDrives() async {
    try {
      final response = await http.get(
        Uri.parse('http://127.0.0.1:8080/api/remoteDrives?targetIp=$remoteIp'),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        List<String> drives = List<String>.from(data['drives']);
        setState(() {
          remoteFiles = drives.map((d) => {
            'name': d,
            'isDir': true,
            'size': 0,
            'modified': '',
          }).toList();
          remotePath = '';
        });
      }
    } catch (e) {
      print('获取远程盘符失败: $e');
    }
  }

  Future<void> navigateRemote(String newPath) async {
    try {
      final response = await http.get(
        Uri.parse('http://127.0.0.1:8080/api/listRemoteFiles?targetIp=$remoteIp&path=${Uri.encodeComponent(newPath)}'),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        List<Map<String, dynamic>> files = (data['files'] as List).map((f) => {
          'name': f['name'],
          'isDir': f['isDir'],
          'size': f['size'],
          'modified': f['modified'],
        }).toList();
        setState(() {
          remoteFiles = files;
          remotePath = newPath;
          remoteSelectPath = null;
        });
      }
    } catch (e) {
      print('获取远程文件列表失败: $e');
    }
  }

  void backRemote() {
    if (remotePath.isEmpty) return;
    String parentPath = path.dirname(remotePath);
    if (parentPath == remotePath) return;
    navigateRemote(parentPath);
  }

  void exitRemoteMode() {
    setState(() {
      isRemoteMode = false;
      remoteIp = null;
      remotePath = '';
      remoteFiles.clear();
      remoteSelectPath = null;
      remoteMultiSelectMode = false;
      remoteSelectedPaths.clear();
      remoteClipPaths.clear();
    });
  }

  // ========== 远程文件操作 ==========
  Future<void> _remoteDownload(String remoteFilePath) async {
    try {
      final exeDir = path.dirname(Platform.resolvedExecutable);
      final saveDir = path.join(exeDir, "received_files");
      final dir = Directory(saveDir);
      if (!await dir.exists()) {
        await dir.create(recursive: true);
      }
      final uri = Uri.parse(
          'http://127.0.0.1:8080/api/remoteDownload?targetIp=$remoteIp&path=${Uri.encodeComponent(remoteFilePath)}&saveDir=${Uri.encodeComponent(saveDir)}');
      final response = await http.get(uri);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text("${data['msg']}：${data['localPath']}")));
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("下载请求失败")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("下载出错：$e")));
    }
  }

  Future<void> _remoteDelete(String path) async {
    try {
      final response = await http.get(Uri.parse(
          'http://127.0.0.1:8080/api/remoteDelete?targetIp=$remoteIp&path=${Uri.encodeComponent(path)}'));
      if (response.statusCode == 200 && jsonDecode(response.body)['code'] == 0) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("删除成功")));
        navigateRemote(remotePath);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("删除失败")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("操作失败：$e")));
    }
  }

  Future<void> _remoteRename(String oldPath) async {
    TextEditingController ctrl = TextEditingController(text: path.basename(oldPath));
    String? newName = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("重命名远程文件"),
        content: TextField(controller: ctrl, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("取消")),
          TextButton(onPressed: () => Navigator.pop(ctx, ctrl.text.trim()), child: const Text("确定")),
        ],
      ),
    );
    if (newName == null || newName.isEmpty) return;
    String newPath = path.join(path.dirname(oldPath), newName);
    try {
      final response = await http.get(Uri.parse(
          'http://127.0.0.1:8080/api/remoteRename?targetIp=$remoteIp&oldPath=${Uri.encodeComponent(oldPath)}&newPath=${Uri.encodeComponent(newPath)}'));
      if (response.statusCode == 200 && jsonDecode(response.body)['code'] == 0) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("重命名成功")));
        navigateRemote(remotePath);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("重命名失败")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("操作失败：$e")));
    }
  }

  Future<void> _remoteCreateFolder(String parentPath) async {
    TextEditingController ctrl = TextEditingController(text: "新建文件夹");
    String? name = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("创建远程文件夹"),
        content: TextField(controller: ctrl, autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("取消")),
          TextButton(onPressed: () => Navigator.pop(ctx, ctrl.text.trim()), child: const Text("创建")),
        ],
      ),
    );
    if (name == null || name.isEmpty) return;
    String newDir = path.join(parentPath, name);
    try {
      final response = await http.get(Uri.parse(
          'http://127.0.0.1:8080/api/remoteMkdir?targetIp=$remoteIp&path=${Uri.encodeComponent(newDir)}'));
      if (response.statusCode == 200 && jsonDecode(response.body)['code'] == 0) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("文件夹已创建")));
        navigateRemote(remotePath);
      } else {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("创建失败")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("操作失败：$e")));
    }
  }

  // ========== 远程剪贴板操作（批量） ==========
  Future<void> _remotePaste() async {
    if (remoteClipPaths.isEmpty || remoteIp == null) return;
    int success = 0;
    for (String srcPath in remoteClipPaths) {
      String srcName = path.basename(srcPath);
      String destPath = path.join(remotePath, srcName);
      if (destPath == srcPath) {
        destPath = _generateCopyName(srcPath);
      }
      String command = remoteClipIsCut ? "MOVE" : "COPY";
      try {
        final uri = Uri.parse(
          'http://127.0.0.1:8080/api/remoteMove?targetIp=$remoteIp&src=${Uri.encodeComponent(srcPath)}&dest=${Uri.encodeComponent(destPath)}&cmd=$command',
        );
        final response = await http.get(uri);
        if (response.statusCode == 200 && jsonDecode(response.body)['code'] == 0) {
          success++;
        }
      } catch (_) {}
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text("${remoteClipIsCut ? "移动" : "复制"}完成：$success / ${remoteClipPaths.length} 项")),
    );
    setState(() { remoteClipPaths.clear(); });
    navigateRemote(remotePath);
  }

  String _generateCopyName(String originalPath) {
    String dir = path.dirname(originalPath);
    String nameWithoutExt = path.basenameWithoutExtension(originalPath);
    String ext = path.extension(originalPath);
    int idx = 1;
    String newPath;
    do {
      newPath = path.join(dir, "$nameWithoutExt - 副本${idx > 1 ? ' ($idx)' : ''}$ext");
      idx++;
    } while (remoteFiles.any((f) => (path.join(remotePath, f['name'])) == newPath));
    return newPath;
  }

  void _setRemoteClip(List<String> paths, bool isCut) {
    setState(() {
      remoteClipPaths = List.from(paths);
      remoteClipIsCut = isCut;
    });
    String action = isCut ? "剪切" : "复制";
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text("已$action ${paths.length} 个项目，请导航到目标文件夹后粘贴")),
    );
  }

  // ========== 远程右键菜单 ==========
  void _showRemoteFileMenu(String fullPath, bool isDir, String name) {
    if (remoteMultiSelectMode) {
      _showRemoteMultiSelectMenu();
      return;
    }
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(12))),
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              child: Text(name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ),
            const Divider(height: 1),
            if (!isDir)
              ListTile(
                leading: const Icon(Icons.download),
                title: const Text("下载文件"),
                onTap: () { Navigator.pop(context); _remoteDownload(fullPath); },
              ),
            ListTile(leading: const Icon(Icons.copy), title: const Text("复制"), onTap: () { Navigator.pop(context); _setRemoteClip([fullPath], false); }),
            ListTile(leading: const Icon(Icons.cut), title: const Text("剪切"), onTap: () { Navigator.pop(context); _setRemoteClip([fullPath], true); }),
            const Divider(height: 1),
            ListTile(leading: const Icon(Icons.edit), title: const Text("重命名"), onTap: () { Navigator.pop(context); _remoteRename(fullPath); }),
            ListTile(leading: const Icon(Icons.delete, color: Colors.red), title: const Text("删除"), onTap: () { Navigator.pop(context); _remoteDelete(fullPath); }),
          ],
        ),
      ),
    );
  }

  void _showRemoteMultiSelectMenu() {
    if (remoteSelectedPaths.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("请先选择文件或文件夹")));
      return;
    }
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(12))),
      builder: (_) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Container(padding: const EdgeInsets.all(16), child: Text("已选择 ${remoteSelectedPaths.length} 个项目", style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold))),
          const Divider(height: 1),
          ListTile(leading: const Icon(Icons.copy), title: const Text("复制所选"), onTap: () { Navigator.pop(context); _setRemoteClip(remoteSelectedPaths.toList(), false); }),
          ListTile(leading: const Icon(Icons.cut), title: const Text("剪切所选"), onTap: () { Navigator.pop(context); _setRemoteClip(remoteSelectedPaths.toList(), true); }),
          const Divider(height: 1),
          ListTile(leading: const Icon(Icons.download), title: const Text("下载所选"), onTap: () { Navigator.pop(context); remoteSelectedPaths.toList().forEach((p) => _remoteDownload(p)); }),
          ListTile(leading: const Icon(Icons.delete, color: Colors.red), title: const Text("删除所选"), onTap: () { Navigator.pop(context); remoteSelectedPaths.toList().forEach((p) => _remoteDelete(p)); remoteSelectedPaths.clear(); navigateRemote(remotePath); }),
        ]),
      ),
    );
  }

  void toggleRemoteMultiSelectMode() {
    setState(() {
      if (remoteMultiSelectMode) {
        remoteMultiSelectMode = false;
        remoteSelectedPaths.clear();
      } else {
        remoteMultiSelectMode = true;
        remoteSelectPath = null;
      }
    });
  }

  void toggleRemoteSelection(String path) {
    setState(() {
      if (remoteSelectedPaths.contains(path)) { remoteSelectedPaths.remove(path); }
      else { remoteSelectedPaths.add(path); }
    });
  }

  // ========== 本地文件操作 ==========
  String formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
    return '${(bytes / 1024 / 1024 / 1024).toStringAsFixed(1)} GB';
  }

  String formatDate(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, "0")}-${dt.day.toString().padLeft(2, "0")} ${dt.hour.toString().padLeft(2, "0")}:${dt.minute.toString().padLeft(2, "0")}';
  }

  void setClipDataList(List<String> paths, ClipType type) {
    if (type == ClipType.cut) {
      List<String> safe = [];
      List<String> blocked = [];
      for (var p in paths) {
        if (isSystemProtectPath(p)) {
          blocked.add(path.basename(p));
        } else {
          safe.add(p);
        }
      }
      if (blocked.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("⚠️ 以下文件/文件夹受系统保护，已跳过剪切：${blocked.join('、')}")),
        );
      }
      if (safe.isEmpty) return;
      paths = safe;
    }
    setState(() {
      currentClipType = type;
      clipPaths = List.from(paths);
    });
    String tip = type == ClipType.copy ? "已复制" : "已剪切";
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text("$tip：${paths.length} 个项目")),
    );
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
    setState(() { currentClipType = ClipType.none; clipPaths.clear(); });
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

  Future<void> deleteSelectedFiles() async {
    if (selectedPaths.isEmpty) return;
    List<String> protected = selectedPaths.where(isSystemProtectPath).toList();
    if (protected.isNotEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("❌ 包含系统保护文件/目录，操作取消")),
      );
      return;
    }
    bool? confirm = await showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("批量删除"),
        content: Text("确定删除选中的 ${selectedPaths.length} 个项目吗？"),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text("取消")),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text("删除", style: TextStyle(color: Colors.red))),
        ],
      ),
    );
    if (confirm != true) return;
    int success = 0;
    for (String p in selectedPaths.toList()) {
      try {
        final type = await FileSystemEntity.type(p);
        if (type == FileSystemEntityType.file) {
          await File(p).delete();
        } else {
          await Directory(p).delete(recursive: true);
        }
        success++;
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("删除失败：$p")));
      }
    }
    selectedPaths.clear();
    loadLocalFiles(currentPath);
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("成功删除 $success 个项目")));
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

  Future<void> openFile(String filePath) async {
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
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("打开文件失败")));
    }
  }

  // ========== 本地右键菜单 ==========
  void showCommonFileMenu(String filePath) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(12))),
      builder: (_) => ConstrainedBox(
        constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.8),
        child: SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Container(
              padding: const EdgeInsets.all(16),
              child: Text(path.basename(filePath), style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold), overflow: TextOverflow.ellipsis),
            ),
            const Divider(height: 1),
            ListTile(leading: const Icon(Icons.copy), title: const Text("复制"), onTap: () { Navigator.pop(context); setClipDataList([filePath], ClipType.copy); }),
            ListTile(leading: const Icon(Icons.cut), title: const Text("剪切"), onTap: () { Navigator.pop(context); setClipDataList([filePath], ClipType.cut); }),
            ListTile(leading: const Icon(Icons.edit), title: const Text("重命名"), onTap: () { Navigator.pop(context); renameFile(filePath); }),
            ListTile(leading: const Icon(Icons.delete, color: Colors.red), title: const Text("删除", style: TextStyle(color: Colors.red)), onTap: () { Navigator.pop(context); deleteFile(filePath); }),
            const Divider(height: 1),
            ListTile(leading: const Icon(Icons.open_in_new), title: const Text("打开"), onTap: () { Navigator.pop(context); openFile(filePath); }),
            ListTile(leading: const Icon(Icons.send), title: const Text("添加到发送列表"), onTap: () { Navigator.pop(context); _addToSendList([filePath]); }),
            ListTile(leading: const Icon(Icons.copy), title: const Text("复制文件路径"), onTap: () { Navigator.pop(context); Clipboard.setData(ClipboardData(text: filePath)); ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("路径已复制"))); }),
            ListTile(leading: const Icon(Icons.info), title: const Text("属性"), onTap: () { Navigator.pop(context); showFileProperties(filePath); }),
          ]),
        ),
      ),
    );
  }

  void showMultiSelectMenu() {
    if (selectedPaths.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("请先选择文件或文件夹")));
      return;
    }
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(12))),
      builder: (_) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Container(padding: const EdgeInsets.all(16), child: Text("已选择 ${selectedPaths.length} 个项目", style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold))),
          const Divider(height: 1),
          ListTile(leading: const Icon(Icons.copy), title: const Text("复制所选"), onTap: () { Navigator.pop(context); setClipDataList(selectedPaths.toList(), ClipType.copy); }),
          ListTile(leading: const Icon(Icons.cut), title: const Text("剪切所选"), onTap: () { Navigator.pop(context); setClipDataList(selectedPaths.toList(), ClipType.cut); }),
          ListTile(leading: const Icon(Icons.delete, color: Colors.red), title: const Text("删除所选", style: TextStyle(color: Colors.red)), onTap: () { Navigator.pop(context); deleteSelectedFiles(); }),
          const Divider(height: 1),
          ListTile(leading: const Icon(Icons.send), title: const Text("添加到发送列表"), onTap: () { Navigator.pop(context); _addToSendList(selectedPaths.toList()); }),
        ]),
      ),
    );
  }

  void showFileProperties(String filePath) {
    FileStat stat = File(filePath).statSync();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("文件属性"),
        content: SizedBox(width: 400, child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text("路径：$filePath"),
          const SizedBox(height: 8),
          Text("类型：${stat.type == FileSystemEntityType.file ? '文件' : '文件夹'}"),
          const SizedBox(height: 8),
          if (stat.type == FileSystemEntityType.file) Text("大小：${formatFileSize(stat.size)}"),
          const SizedBox(height: 8),
          Text("创建时间：${formatDate(stat.changed)}"),
          const SizedBox(height: 8),
          Text("修改时间：${formatDate(stat.modified)}"),
          const SizedBox(height: 8),
          Text("访问时间：${formatDate(stat.accessed)}"),
        ])),
        actions: [TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("确定"))],
      ),
    );
  }

  void toggleMultiSelectMode() {
    setState(() {
      if (multiSelectMode) {
        multiSelectMode = false;
        selectedPaths.clear();
      } else {
        multiSelectMode = true;
        selectItemPath = null;
      }
    });
  }

  void toggleSelection(String path) {
    setState(() {
      if (selectedPaths.contains(path)) { selectedPaths.remove(path); }
      else { selectedPaths.add(path); }
    });
  }

  @override
  Widget build(BuildContext context) {
    final pathController = TextEditingController(text: isRemoteMode ? remotePath : currentPath);
    return Scaffold(
      body: Row(
        children: [
          Container(
            width: 240,
            color: Colors.white,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(padding: EdgeInsets.all(16), child: Text("此电脑", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold))),
                for (String drive in drives)
                  MouseRegion(
                    cursor: SystemMouseCursors.click,
                    onEnter: (_) => setState(() => hoverSideItem = drive),
                    onExit: (_) => setState(() { if (hoverSideItem == drive) hoverSideItem = ''; }),
                    child: Container(
                      color: hoverSideItem == drive ? Colors.grey.shade100 : Colors.transparent,
                      child: ListTile(
                        leading: const Icon(Icons.storage),
                        title: Text("$drive 盘"),
                        onTap: () { exitRemoteMode(); openDisk(drive); },
                      ),
                    ),
                  ),
                const Divider(height: 20),
                MouseRegion(
                  cursor: SystemMouseCursors.click,
                  onEnter: (_) => setState(() => hoverSideItem = 'java'),
                  onExit: (_) => setState(() { if (hoverSideItem == 'java') hoverSideItem = ''; }),
                  child: Container(
                    color: hoverSideItem == 'java' ? Colors.grey.shade100 : Colors.transparent,
                    child: ListTile(
                      leading: const Icon(Icons.launch),
                      title: const Text("启动 Java 文件传输"),
                      onTap: startJavaLinkDisk,
                    ),
                  ),
                ),
                const Divider(height: 20),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Row(children: [
                    const Text("局域网设备", style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    const Spacer(),
                    IconButton(icon: const Icon(Icons.refresh, size: 20), onPressed: fetchTrustedDevices, tooltip: "刷新"),
                  ]),
                ),
                Expanded(
                  child: ListView.builder(
                    itemCount: trustedDevices.length,
                    itemBuilder: (ctx, i) {
                      final ip = trustedDevices[i];
                      return ListTile(
                        leading: const Icon(Icons.computer, color: Colors.blue),
                        title: Text(ip),
                        onTap: () => connectRemoteDevice(ip),
                      );
                    },
                  ),
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
                      IconButton(onPressed: goBack, icon: const Icon(Icons.arrow_back)),
                      if (isRemoteMode)
                        Text("远程：$remoteIp", style: const TextStyle(fontSize: 13)),
                      if (!isRemoteMode)
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
                                if (Directory(value).existsSync()) { currentPath = value; selectItemPath = null; loadLocalFiles(value); }
                                else { ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("路径不存在"))); }
                              },
                            ),
                          ),
                        ),
                      const Spacer(),
                      // 远程剪贴板粘贴/取消
                      if (isRemoteMode && remoteClipPaths.isNotEmpty) ...[
                        TextButton(onPressed: () => setState(() => remoteClipPaths.clear()), child: const Text("取消")),
                        ElevatedButton.icon(icon: const Icon(Icons.content_paste, size: 18), label: Text("粘贴 (${remoteClipPaths.length})"), onPressed: _remotePaste),
                      ],
                      // 远程多选模式
                      if (isRemoteMode) ...[
                        if (remoteMultiSelectMode) Text("已选 ${remoteSelectedPaths.length} 项"),
                        if (remoteMultiSelectMode)
                          TextButton(
                            onPressed: () {
                              if (remoteSelectedPaths.isNotEmpty) {
                                _showRemoteMultiSelectMenu();
                              } else {
                                toggleRemoteMultiSelectMode();
                              }
                            },
                            child: Text(remoteSelectedPaths.isNotEmpty ? "操作" : "完成"),
                          ),
                        IconButton(
                          icon: Icon(remoteMultiSelectMode ? Icons.check_box : Icons.check_box_outline_blank),
                          onPressed: toggleRemoteMultiSelectMode,
                          tooltip: "批量选择",
                        ),
                      ],
                      // 本地模式
                      if (!isRemoteMode) ...[
                        if (multiSelectMode) Text("已选 ${selectedPaths.length} 项"),
                        if (multiSelectMode)
                          TextButton(
                            onPressed: () {
                              if (selectedPaths.isNotEmpty) {
                                showMultiSelectMenu();
                              } else {
                                toggleMultiSelectMode();
                              }
                            },
                            child: Text(selectedPaths.isNotEmpty ? "操作" : "完成"),
                          ),
                        IconButton(
                          icon: Icon(multiSelectMode ? Icons.check_box : Icons.check_box_outline_blank),
                          onPressed: toggleMultiSelectMode,
                          tooltip: "批量选择",
                        ),
                        IconButton(onPressed: createNewFolder, icon: const Icon(Icons.create_new_folder_outlined)),
                        if (currentClipType != ClipType.none)
                          TextButton(onPressed: pasteFile, child: const Text("粘贴")),
                      ] else ...[
                        IconButton(
                          icon: const Icon(Icons.create_new_folder_outlined),
                          tooltip: "新建文件夹",
                          onPressed: () {
                            if (remotePath.isNotEmpty) { _remoteCreateFolder(remotePath); }
                            else { ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("请先进入一个远程文件夹"))); }
                          },
                        ),
                      ],
                      PopupMenuButton<String>(
                        icon: const Icon(Icons.more_vert),
                        onSelected: (v) {
                          if (v == "hidden") { setState(() { showHiddenFiles = !showHiddenFiles; if (isRemoteMode) navigateRemote(remotePath); else loadLocalFiles(currentPath); }); }
                          else if (v == "list") setState(() => viewMode = "list");
                          else if (v == "grid") setState(() => viewMode = "grid");
                          else if (v == "risky") setState(() => blockRiskyOpen = !blockRiskyOpen);
                        },
                        itemBuilder: (_) => [
                          PopupMenuItem(value: "hidden", child: Row(children: [const Text("显示隐藏文件"), if (showHiddenFiles) const Icon(Icons.check, size: 16, color: Colors.blue)])),
                          const PopupMenuDivider(),
                          PopupMenuItem(value: "list", child: Row(children: [const Text("列表视图"), if (viewMode == "list") const Icon(Icons.check, size: 16, color: Colors.blue)])),
                          PopupMenuItem(value: "grid", child: Row(children: [const Text("图标视图"), if (viewMode == "grid") const Icon(Icons.check, size: 16, color: Colors.blue)])),
                          const PopupMenuDivider(),
                          PopupMenuItem(value: "risky", child: Row(children: [const Text("允许打开高危文件"), if (!blockRiskyOpen) const Icon(Icons.check, size: 16, color: Colors.blue)])),
                        ],
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: _buildFileList(),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFileList() {
    if (isRemoteMode) {
      if (remoteFiles.isEmpty) return const Center(child: CircularProgressIndicator());
      return viewMode == "grid" ? _buildGridView(remoteFiles, isRemote: true) : _buildListView(remoteFiles, isRemote: true);
    }
    if (localFiles.isEmpty) return const Center(child: Text("空文件夹"));
    return viewMode == "grid" ? _buildGridView(localFiles) : _buildListView(localFiles);
  }

  Widget _buildGridView(List<dynamic> items, {bool isRemote = false}) {
    return GridView.builder(
      padding: const EdgeInsets.all(12),
      gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
        maxCrossAxisExtent: 240, mainAxisExtent: 60, crossAxisSpacing: 12, mainAxisSpacing: 12,
      ),
      itemCount: items.length,
      itemBuilder: (ctx, i) {
        dynamic entity = items[i];
        String fullPath, name;
        bool isDir;
        if (isRemote) {
          var map = entity as Map<String, dynamic>;
          name = map['name'];
          fullPath = path.join(remotePath, name);
          isDir = map['isDir'];
        } else {
          var e = entity as FileSystemEntity;
          fullPath = e.path;
          name = path.basename(fullPath);
          isDir = e is Directory;
        }
        bool isSelected;
        if (isRemote) {
          isSelected = remoteMultiSelectMode ? remoteSelectedPaths.contains(fullPath) : remoteSelectPath == fullPath;
        } else {
          isSelected = multiSelectMode ? selectedPaths.contains(fullPath) : selectItemPath == fullPath;
        }
        bool isHovered = hoverPath == fullPath;
        return MouseRegion(
          cursor: SystemMouseCursors.click,
          onEnter: (_) => setState(() => hoverPath = fullPath),
          onExit: (_) => setState(() { if (hoverPath == fullPath) hoverPath = null; }),
          child: GestureDetector(
            onTap: () {
              if (isRemote) {
                if (remoteMultiSelectMode) { toggleRemoteSelection(fullPath); }
                else { setState(() => remoteSelectPath = fullPath); }
              } else {
                if (multiSelectMode) { toggleSelection(fullPath); }
                else { setState(() => selectItemPath = fullPath); }
              }
            },
            onDoubleTap: () async {
              if (isRemote) {
                if (isDir) { navigateRemote(fullPath); }
                else { _remoteDownload(fullPath); }
              } else {
                if (isDir) { currentPath = fullPath; selectItemPath = null; loadLocalFiles(currentPath); }
                else { await openFile(fullPath); }
              }
            },
            onLongPress: () {
              if (isRemote) { _showRemoteFileMenu(fullPath, isDir, name); }
              else if (multiSelectMode) { showMultiSelectMenu(); }
              else { showCommonFileMenu(fullPath); }
            },
            onSecondaryTap: () {
              if (isRemote) { _showRemoteFileMenu(fullPath, isDir, name); }
              else if (multiSelectMode) { showMultiSelectMenu(); }
              else { showCommonFileMenu(fullPath); }
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: isSelected ? Colors.blue.shade50 : (isHovered ? Colors.grey.shade100 : Colors.white),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: isSelected ? Colors.blueAccent : Colors.grey.shade200),
              ),
              child: Row(children: [
                if ((!isRemote && multiSelectMode) || (isRemote && remoteMultiSelectMode))
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Icon(isSelected ? Icons.check_circle : Icons.radio_button_unchecked, color: isSelected ? Colors.blue : Colors.grey, size: 20),
                  ),
                Icon(isDir ? Icons.folder : Icons.insert_drive_file, color: isDir ? Colors.blue : null),
                const SizedBox(width: 12),
                Expanded(child: Text(name, overflow: TextOverflow.ellipsis)),
              ]),
            ),
          ),
        );
      },
    );
  }

  Widget _buildListView(List<dynamic> items, {bool isRemote = false}) {
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: items.length,
      itemBuilder: (ctx, i) {
        dynamic entity = items[i];
        String fullPath, name, info;
        bool isDir;
        if (isRemote) {
          var map = entity as Map<String, dynamic>;
          name = map['name'];
          fullPath = path.join(remotePath, name);
          isDir = map['isDir'];
          info = map['modified'].toString();
        } else {
          var e = entity as FileSystemEntity;
          fullPath = e.path;
          name = path.basename(fullPath);
          isDir = e is Directory;
          FileStat stat = e.statSync();
          info = isDir ? formatDate(stat.modified) : "${formatFileSize(stat.size)} | ${formatDate(stat.modified)}";
        }
        bool isSelected;
        if (isRemote) {
          isSelected = remoteMultiSelectMode ? remoteSelectedPaths.contains(fullPath) : remoteSelectPath == fullPath;
        } else {
          isSelected = multiSelectMode ? selectedPaths.contains(fullPath) : selectItemPath == fullPath;
        }
        bool isHovered = hoverPath == fullPath;
        return MouseRegion(
          cursor: SystemMouseCursors.click,
          onEnter: (_) => setState(() => hoverPath = fullPath),
          onExit: (_) => setState(() { if (hoverPath == fullPath) hoverPath = null; }),
          child: GestureDetector(
            onTap: () {
              if (isRemote) {
                if (remoteMultiSelectMode) { toggleRemoteSelection(fullPath); }
                else { setState(() => remoteSelectPath = fullPath); }
              } else {
                if (multiSelectMode) { toggleSelection(fullPath); }
                else { setState(() => selectItemPath = fullPath); }
              }
            },
            onDoubleTap: () async {
              if (isRemote) {
                if (isDir) { navigateRemote(fullPath); }
                else { _remoteDownload(fullPath); }
              } else {
                if (isDir) { currentPath = fullPath; selectItemPath = null; loadLocalFiles(currentPath); }
                else { await openFile(fullPath); }
              }
            },
            onLongPress: () {
              if (isRemote) { _showRemoteFileMenu(fullPath, isDir, name); }
              else if (multiSelectMode) { showMultiSelectMenu(); }
              else { showCommonFileMenu(fullPath); }
            },
            onSecondaryTap: () {
              if (isRemote) { _showRemoteFileMenu(fullPath, isDir, name); }
              else if (multiSelectMode) { showMultiSelectMenu(); }
              else { showCommonFileMenu(fullPath); }
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
              margin: const EdgeInsets.only(bottom: 6),
              decoration: BoxDecoration(
                color: isSelected ? Colors.blue.shade50 : (isHovered ? Colors.grey.shade100 : Colors.white),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: isSelected ? Colors.blueAccent : Colors.grey.shade200),
              ),
              child: Row(children: [
                if ((!isRemote && multiSelectMode) || (isRemote && remoteMultiSelectMode))
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: Icon(isSelected ? Icons.check_circle : Icons.radio_button_unchecked, color: isSelected ? Colors.blue : Colors.grey, size: 20),
                  ),
                Icon(isDir ? Icons.folder : Icons.insert_drive_file, color: isDir ? Colors.blue : null),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(name, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 14)),
                    const SizedBox(height: 2),
                    Text(info, style: const TextStyle(fontSize: 11, color: Colors.grey)),
                  ]),
                ),
              ]),
            ),
          ),
        );
      },
    );
  }
}