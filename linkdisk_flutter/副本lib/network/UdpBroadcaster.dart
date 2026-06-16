import 'dart:io';
import 'dart:async';

class UdpBroadcaster {
  static const int broadcastPort = 54321;
  static const String broadcastMessage = "LINKDISK_DEVICE";
  static const Duration broadcastInterval = Duration(seconds: 3);

  static Future<void> startBroadcast() async {
    // 创建 UDP Socket
    final socket = await RawDatagramSocket.bind(
      InternetAddress.anyIPv4,
      0, // 随机端口
    );

    // 开启广播模式（对应 Java setBroadcast(true)）
    socket.broadcastEnabled = true;
    print('UDP 广播已启动，每3秒发送一次...');

    // 目标广播地址 + 端口
    final address = InternetAddress('255.255.255.255');
    final messageBytes = broadcastMessage.codeUnits;

    // 循环广播
    Timer.periodic(broadcastInterval, (timer) {
      socket.send(
        messageBytes,
        address,
        broadcastPort,
      );
      print('广播已发送');
    });
  }
}

// 主函数（和 Java main 一样）
void main() async {
  await UdpBroadcaster.startBroadcast();
}