import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

// 你之前已有的监听器
// abstract class DeviceFoundListener {
//   void onDeviceFound(String ip, String deviceName, String platform);
// }

class UdpListener {
  static RawDatagramSocket? _listenSocket;
  static bool _isRunning = false;
  static Timer? _broadcastTimer;

  static const int _udpPort = 54321;
  static const String _prefix = "LINKDISK_DEVICE";
  static const String _deviceIdFile = "local_device_id.txt";

  static final String _localDeviceId = _loadOrCreateDeviceId();
  static final String _localDeviceName = _getLocalDeviceName();
  static final String _localPlatform = _getPlatformName();

  // 启动监听 + 广播
  static void startListening(DeviceFoundListener listener) {
    if (_isRunning) return;
    _isRunning = true;

    _startUdpListener(listener);
    _startUdpBroadcaster();

    print("UDP监听已启动，端口：$_udpPort");
    print("本机设备ID：$_localDeviceId");
    print("本机设备名：$_localDeviceName");
    print("本机平台：$_localPlatform");
  }

  // 停止
  static void stopListening() {
    _isRunning = false;
    _listenSocket?.close();
    _broadcastTimer?.cancel();
  }

  // UDP 监听设备
  static void _startUdpListener(DeviceFoundListener listener) {
    RawDatagramSocket.bind(InternetAddress.anyIPv4, _udpPort).then((socket) {
      _listenSocket = socket;

      socket.listen((event) {
        if (!_isRunning) return;
        if (event == RawSocketEvent.read) {
          final datagram = socket.receive();
          if (datagram == null) return;

          final ip = datagram.address.address;
          final message = utf8.decode(datagram.data).trim();
          final parts = message.split('|');

          if (parts.length == 4 && parts[0] == _prefix) {
            final deviceId = parts[1];
            final deviceName = parts[2];
            final platform = parts[3];

            // 过滤自己
            if (deviceId == _localDeviceId) return;

            print("发现远程设备：$deviceName / $platform / $ip");
            listener.onDeviceFound(ip, deviceName, platform);
          }
        }
      });
    });
  }

  // 定时广播本机信息
  static void _startUdpBroadcaster() {
    RawDatagramSocket.bind(InternetAddress.anyIPv4, 0).then((socket) {
      socket.broadcastEnabled = true;
      final broadcastAddr = InternetAddress('255.255.255.255');

      _broadcastTimer = Timer.periodic(const Duration(seconds: 3), (timer) {
        if (!_isRunning) {
          timer.cancel();
          socket.close();
          return;
        }

        final msg = "$_prefix|$_localDeviceId|$_localDeviceName|$_localPlatform";
        final data = utf8.encode(msg);
        socket.send(data, broadcastAddr, _udpPort);
        print("广播已发送：$msg");
      });
    });
  }

  // 加载或创建设备唯一ID
  static String _loadOrCreateDeviceId() {
    final file = File(_deviceIdFile);
    try {
      if (file.existsSync()) {
        final id = file.readAsStringSync().trim();
        if (id.isNotEmpty) return id;
      }

      final newId = Uuid().v4();
      file.writeAsStringSync(newId);
      return newId;
    } catch (e) {
      return Uuid().v4();
    }
  }

  // 获取本机电脑名
  static String _getLocalDeviceName() {
    try {
      String? name = Platform.environment["COMPUTERNAME"];
      name ??= Platform.environment["HOSTNAME"];

      if (name == null || name.isEmpty) {
        name = InternetAddress.localHost.hostName;
      }

      name ??= "UnknownDevice";
      return name.replaceAll("|", "_");
    } catch (e) {
      return "UnknownDevice";
    }
  }

  // 获取平台名称
  static String _getPlatformName() {
    if (Platform.isWindows) return "Windows";
    if (Platform.isMacOS) return "macOS";
    if (Platform.isLinux) return "Linux";
    return Platform.operatingSystem;
  }

  // 对外提供的方法
  static String getThisDeviceName() => _localDeviceName;
  static String getThisPlatform() => _localPlatform;

  // 判断是否为本机IP
  static Future<bool> isLocalIp(String ip) async {
    try {
      for (final interface in await NetworkInterface.list()) {
        for (final addr in interface.addresses) {
          if (addr.address == ip) {
            return true;
          }
        }
      }
    } catch (e) {}
    return false;
  }
}

// 简易 UUID 实现（无需外部依赖）
class Uuid {
  String v4() {
    final rand = Random.secure();
    final bytes = Uint8List(16);
    for (int i = 0; i < 16; i++) bytes[i] = rand.nextInt(256);
    bytes[6] = (bytes[6] & 0x0F) | 0x40;
    bytes[8] = (bytes[8] & 0x3F) | 0x80;
    final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0,8)}-${hex.substring(8,12)}-${hex.substring(12,16)}-${hex.substring(16,20)}-${hex.substring(20)}';
  }
}