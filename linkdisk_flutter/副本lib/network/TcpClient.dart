import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

// 你之前已经有的类（确保已导入）
// import 'TransferFileItem.dart';
// import 'ProgressListener.dart';

class TcpClient {
  static const int tcpPort = 6000;
  static const int connectTimeout = 5000;
  static const int readTimeout = 15000;

  // 连接结果
  static ConnectResult connectDevice(String ip) {
    Socket? socket;
    try {
      socket = SocketSync.connect(
        ip,
        tcpPort,
        timeout: Duration(milliseconds: connectTimeout),
      );

      socket.setOption(SocketOption.tcpNoDelay, true);
      socket.writeMode = WriteMode.block;

      final out = socket;
      final inStream = socket;

      // 发送 AUTH 指令
      out.write(utf8.encode('AUTH\n'));
      out.flush();

      // 读取响应
      final result = _readUtfLine(inStream);
      if (result != 'OK') {
        return ConnectResult(false, null, null, '对方拒绝连接');
      }

      final deviceName = _readUtfLine(inStream);
      final platform = _readUtfLine(inStream);

      return ConnectResult(true, deviceName, platform, '连接成功');
    } on SocketException {
      return ConnectResult(
        false,
        null,
        null,
        '连接失败：目标设备未启动 LinkDisk 或端口未开放',
      );
    } on TimeoutException {
      return ConnectResult(
        false,
        null,
        null,
        '连接超时：目标设备无响应',
      );
    } catch (e) {
      print(e);
      return ConnectResult(false, null, null, '连接失败：${e.toString()}');
    } finally {
      socket?.close();
    }
  }

  // 发送文件（重载1）
  static SendResult sendFiles(
    List<TransferFileItem> items,
    String ip,
    ProgressListener listener,
  ) {
    return sendFilesWithCancel(items, ip, listener, null);
  }

  // 发送文件（重载2：带取消检查）
  static SendResult sendFilesWithCancel(
    List<TransferFileItem> items,
    String ip,
    ProgressListener listener,
    CancelChecker? cancelChecker,
  ) {
    Socket? socket;
    try {
      socket = SocketSync.connect(
        ip,
        tcpPort,
        timeout: Duration(milliseconds: connectTimeout),
      );
      socket.setOption(SocketOption.tcpNoDelay, true);

      // 发送 FILE 指令
      socket.write(utf8.encode('FILE\n'));
      socket.flush();

      final resp = _readUtfLine(socket);
      if (resp != 'OK') {
        return SendResult(false, '对方拒绝连接，文件未发送');
      }

      // 发送文件数量
      _writeInt(socket, items.length);

      // 计算总大小
      int totalBytes = items.fold(0, (sum, item) => sum + item.size);
      int sentBytes = 0;
      final buffer = Uint8List(8192);

      for (int i = 0; i < items.length; i++) {
        final item = items[i];
        final file = item.sourceFile;
        final relativePath = item.relativePath;

        // 取消检查
        if (cancelChecker != null &&
            cancelChecker.isCancelled(i, relativePath)) {
          print('跳过已取消文件：$relativePath');
          sentBytes += item.size;
          final totalProgress =
              totalBytes == 0 ? 100 : (sentBytes * 100 ~/ totalBytes);
          listener.onTotalProgress(totalProgress);
          continue;
        }

        print('准备发送文件：${file.path}');
        print('发送相对路径：$relativePath');
        print('发送文件大小：${_formatFileSize(item.size)}');

        listener.onFileStart(i, relativePath);

        // 发送相对路径 + 文件大小
        _writeUtfLine(socket, relativePath);
        _writeLong(socket, item.size);

        // 读取文件并发送
        final fileStream = file.openRead();
        final reader = BytesReader(fileStream);
        int fileSentBytes = 0;

        while (true) {
          final readBytes = reader.readInto(buffer);
          if (readBytes == 0) break;

          socket.add(buffer.sublist(0, readBytes));
          await socket.flush(); // 确保发送

          sentBytes += readBytes;
          fileSentBytes += readBytes;

          // 总进度
          final totalProgress =
              totalBytes == 0 ? 100 : (sentBytes * 100 ~/ totalBytes);
          // 文件进度
          final fileProgress =
              item.size == 0 ? 100 : (fileSentBytes * 100 ~/ item.size);

          listener.onTotalProgress(totalProgress);
          listener.onFileProgress(i, relativePath, fileProgress);
        }

        listener.onFileComplete(i, relativePath);
      }

      return SendResult(true, '文件发送完成');
    } on SocketException {
      return SendResult(
        false,
        '发送失败：目标设备未启动 LinkDisk 或端口未开放',
      );
    } on TimeoutException {
      return SendResult(false, '发送失败：连接超时或对方无响应');
    } catch (e) {
      print(e);
      return SendResult(false, '发送失败：${e.toString()}');
    } finally {
      socket?.close();
    }
  }

  // ------------------------------
  // 工具：读写 UTF 行（对应 writeUTF / readUTF）
  // ------------------------------
  static String _readUtfLine(Socket socket) {
    final line = utf8.decode(socket.readSync() ?? []);
    return line.trim();
  }

  static void _writeUtfLine(Socket socket, String text) {
    socket.write('$text\n');
    socket.flush();
  }

  static void _writeInt(Socket socket, int value) {
    final b = ByteData(4)..setInt32(0, value, Endian.big);
    socket.add(b.buffer.asUint8List());
  }

  static void _writeLong(Socket socket, int value) {
    final b = ByteData(8)..setInt64(0, value, Endian.big);
    socket.add(b.buffer.asUint8List());
  }

  // 文件大小格式化
  static String _formatFileSize(int size) {
    if (size < 1000) return '$size B';
    double v = size / 1000;
    if (v < 1000) return '${v.toStringAsFixed(2)} KB';
    v /= 1000;
    if (v < 1000) return '${v.toStringAsFixed(2)} MB';
    v /= 1000;
    return '${v.toStringAsFixed(2)} GB';
  }
}

// ------------------------------
// 数据类
// ------------------------------
class ConnectResult {
  final bool success;
  final String? deviceName;
  final String? platform;
  final String message;

  ConnectResult(this.success, this.deviceName, this.platform, this.message);
}

class SendResult {
  final bool success;
  final String message;

  SendResult(this.success, this.message);
}

// ------------------------------
// 取消检查接口
// ------------------------------
abstract class CancelChecker {
  bool isCancelled(int fileIndex, String relativePath);
}

// ------------------------------
// 同步 Socket 辅助（Dart 原生不提供，我给你补全）
// ------------------------------
class SocketSync {
  static Socket connect(String host, int port,
      {Duration timeout = const Duration(seconds: 5)}) {
    return Socket.connect(host, port, timeout: timeout).sync;
  }
}

// 辅助：流同步读取
class BytesReader {
  final Stream<List<int>> _stream;
  final List<int> _buffer = [];

  BytesReader(this._stream);

  int readInto(Uint8List buffer) {
    if (_buffer.isEmpty) {
      return 0;
    }
    final len = _buffer.length;
    buffer.setRange(0, len, _buffer);
    _buffer.clear();
    return len;
  }
}