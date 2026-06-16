import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

// 依赖你之前已有的类
// import 'AuthManager.dart';
// import 'AppSettings.dart';
// import 'UdpListener.dart';

class TcpServer {
  static AuthManager? _authManager;
  static AuthCallback? _authCallback;
  static ReceiveCallback? _receiveCallback;
  static ServerSocket? _serverSocket;
  static bool _isRunning = false;

  // 启动服务（重载1）
  static void startServer(AuthCallback callback) {
    startServerWithReceive(callback, null);
  }

  // 启动服务（重载2）
  static void startServerWithReceive(
    AuthCallback callback,
    ReceiveCallback? receiveCallback,
  ) {
    if (_isRunning) return;

    _authManager = AuthManager();
    _authCallback = callback;
    _receiveCallback = receiveCallback;
    _isRunning = true;

    // 创建接收目录
    final dir = Directory(AppSettings.getReceiveDir());
    if (!dir.existsSync()) dir.createSync(recursive: true);

    // 启动服务
    _startServerSocket();
  }

  // 停止服务
  static void stopServer() {
    _isRunning = false;
    try {
      _serverSocket?.close();
      _serverSocket = null;
    } catch (e) {
      print(e);
    }
  }

  // 启动 socket 监听
  static Future<void> _startServerSocket() async {
    try {
      _serverSocket = await ServerSocket.bind(InternetAddress.anyIPv4, 6000);
      print("TCP服务器已启动，端口：6000");

      await for (final socket in _serverSocket!) {
        if (!_isRunning) break;

        final clientIp = socket.remoteAddress.address;
        print("收到连接请求来自：$clientIp");

        // 每客户端一个独立隔离处理
        _handleConnection(socket, clientIp);
      }
    } catch (e) {
      if (_isRunning) print(e);
    }
  }

  // 处理单个客户端连接
  static Future<void> _handleConnection(
    Socket socket,
    String clientIp,
  ) async {
    try {
      final inStream = socket;
      final outStream = socket;
      final utf8Decoder = Utf8Decoder();

      // 读取命令 AUTH / FILE
      final command = await _readUtf(inStream);

      // 授权检查
      bool authorized = true;
      if (!_authManager!.isTrusted(clientIp)) {
        authorized = false;
        if (_authCallback != null) {
          authorized = _authCallback!.onAuthRequest(clientIp);
        }
        if (authorized) {
          _authManager!.addTrustedDevice(clientIp);
          print("已授权设备：$clientIp");
        } else {
          print("拒绝设备连接：$clientIp");
        }
      } else {
        print("信任设备已连接：$clientIp");
      }

      // 未授权直接拒绝
      if (!authorized) {
        _writeUtf(outStream, "DENIED");
        await socket.flush();
        await socket.close();
        return;
      }

      // 处理 AUTH
      if (command == "AUTH") {
        _writeUtf(outStream, "OK");
        _writeUtf(outStream, UdpListener.getThisDeviceName());
        _writeUtf(outStream, UdpListener.getThisPlatform());
        await outStream.flush();
        print("设备 $clientIp 连接授权完成");
      }

      // 处理 FILE
      else if (command == "FILE") {
        _writeUtf(outStream, "OK");
        await outStream.flush();
        await _receiveFiles(inStream, clientIp);
      }

      // 未知命令
      else {
        _writeUtf(outStream, "DENIED");
        await outStream.flush();
        print("未知请求类型：$command");
      }

      await socket.close();
    } catch (e) {
      print("处理设备 $clientIp 连接时出错：${e.toString()}");
      await socket.close();
    }
  }

  // ------------------------------
  // 接收文件核心逻辑
  // ------------------------------
  static Future<void> _receiveFiles(Socket socket, String clientIp) async {
    try {
      // 读取文件数量
      final fileCount = await _readInt(socket);
      print("从 $clientIp 接收文件数量：$fileCount");

      final receiveDir = Directory(AppSettings.getReceiveDir());
      if (!receiveDir.existsSync()) {
        receiveDir.createSync(recursive: true);
      }

      for (int i = 0; i < fileCount; i++) {
        // 读取相对路径 & 文件大小
        final relativePath = await _readUtf(socket);
        final fileSize = await _readLong(socket);

        // 安全路径处理
        final safePath = _sanitizeRelativePath(relativePath);
        print("接收文件：$safePath 大小：${_formatFileSize(fileSize)}");

        // 构建保存路径
        final saveFile = File('${receiveDir.path}${Platform.pathSeparator}$safePath');
        final parent = saveFile.parent;
        if (!parent.existsSync()) parent.createSync(recursive: true);

        // 重命名避免覆盖
        final uniqueFile = _buildUniqueSaveFile(saveFile);
        final savePath = uniqueFile.path;

        // 临时文件
        final tempFile = File('$savePath.part');
        if (tempFile.existsSync()) await tempFile.delete();

        // 开始回调
        _receiveCallback?.onFileReceiveStart(
          clientIp,
          safePath,
          savePath,
          fileSize,
        );

        // 写入文件
        final fileOut = tempFile.openWrite();
        final buffer = Uint8List(8192);
        int received = 0;
        int lastProgress = -1;

        while (received < fileSize) {
          final need = (buffer.length < (fileSize - received))
              ? buffer.length
              : (fileSize - received);

          final bytes = await socket.read(need);
          if (bytes == null || bytes.isEmpty) break;

          fileOut.add(bytes);
          await fileOut.flush();

          received += bytes.length;

          // 进度
          final progress = (fileSize == 0)
              ? 100
              : (received * 100 ~/ fileSize);

          if (progress != lastProgress) {
            lastProgress = progress;
            _receiveCallback?.onFileReceiveProgress(
              clientIp,
              safePath,
              savePath,
              fileSize,
              received,
              progress,
            );
          }
        }

        await fileOut.close();

        // 接收完成
        if (received == fileSize) {
          await tempFile.rename(savePath);
          print("$safePath 接收完成");
          print("实际保存路径：$savePath");
          _receiveCallback?.onFileReceived(
            clientIp,
            safePath,
            savePath,
            received,
          );
        } else {
          if (await tempFile.exists()) await tempFile.delete();
          print("$safePath 接收失败：文件不完整");
        }
      }

      print("设备 $clientIp 所有文件接收完成\n");
    } catch (e) {
      print(e);
    }
  }

  // ------------------------------
  // 工具：路径安全处理（防路径穿越）
  // ------------------------------
  static String _sanitizeRelativePath(String path) {
    if (path.trim().isEmpty) return "unnamed_file";

    var normalized = path.replaceAll('\\', '/');
    while (normalized.startsWith('/')) {
      normalized = normalized.substring(1);
    }

    final parts = normalized.split('/');
    final safe = StringBuffer();

    for (final part in parts) {
      if (part.isEmpty || part == '.' || part == '..') continue;

      var clean = part
          .replaceAll(':', '_')
          .replaceAll('*', '_')
          .replaceAll('?', '_')
          .replaceAll('"', '_')
          .replaceAll('<', '_')
          .replaceAll('>', '_')
          .replaceAll('|', '_');

      if (safe.isNotEmpty) safe.write(Platform.pathSeparator);
      safe.write(clean);
    }

    return safe.isEmpty ? "unnamed_file" : safe.toString();
  }

  // ------------------------------
  // 工具：自动重命名文件（避免覆盖）
  // ------------------------------
  static File _buildUniqueSaveFile(File file) {
    if (!file.existsSync()) return file;

    final name = file.path.split(Platform.pathSeparator).last;
    final dir = file.parent.path;

    final dot = name.lastIndexOf('.');
    var base = name;
    var ext = '';

    if (dot != -1) {
      base = name.substring(0, dot);
      ext = name.substring(dot);
    }

    int count = 1;
    while (true) {
      final newPath = '$dir${Platform.pathSeparator}$base($count)$ext';
      final f = File(newPath);
      if (!f.existsSync()) return f;
      count++;
    }
  }

  // ------------------------------
  // 工具：文件大小格式化
  // ------------------------------
  static String _formatFileSize(int size) {
    if (size < 1000) return '$size B';
    double v = size / 1000;
    if (v < 1000) return '${v.toStringAsFixed(2)} KB';
    v /= 1000;
    if (v < 1000) return '${v.toStringAsFixed(2)} MB';
    v /= 1000;
    return '${v.toStringAsFixed(2)} GB';
  }

  // ------------------------------
  // 底层读写工具（对应 Java DataInputStream）
  // ------------------------------
  static Future<String> _readUtf(Socket socket) async {
    final line = await utf8.decodeStream(socket);
    return line.trim();
  }

  static void _writeUtf(Socket socket, String text) {
    socket.write('$text\n');
  }

  static Future<int> _readInt(Socket socket) async {
    final b = await _readBytes(socket, 4);
    return ByteData.sublistView(b).getInt32(0, Endian.big);
  }

  static Future<int> _readLong(Socket socket) async {
    final b = await _readBytes(socket, 8);
    return ByteData.sublistView(b).getInt64(0, Endian.big);
  }

  static Future<Uint8List> _readBytes(Socket socket, int len) async {
    final b = BytesBuilder();
    while (b.length < len) {
      final chunk = await socket.first;
      b.add(chunk);
    }
    return b.toBytes().sublist(0, len);
  }
}

// ------------------------------
// 回调接口
// ------------------------------
abstract class AuthCallback {
  bool onAuthRequest(String ip);
}

abstract class ReceiveCallback {
  void onFileReceiveStart(
    String clientIp,
    String fileName,
    String savePath,
    int fileSize,
  );

  void onFileReceiveProgress(
    String clientIp,
    String fileName,
    String savePath,
    int fileSize,
    int receivedBytes,
    int progress,
  );

  void onFileReceived(
    String clientIp,
    String fileName,
    String savePath,
    int fileSize,
  );
}