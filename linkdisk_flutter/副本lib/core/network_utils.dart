import 'dart:async';
import 'dart:convert';
import 'dart:io';

class NetworkUtils {
  static const int udpPort = 32000;
  static const int filePort = 32001;

  static Future<String?> getLocalIp() async {
    try {
      final list = await NetworkInterface.list(
        type: InternetAddressType.IPv4,
        includeLoopback: false,
      );
      for (var i in list) {
        for (var a in i.addresses) {
          if (a.address.startsWith('192.168.') || a.address.startsWith('10.')) {
            return a.address;
          }
        }
      }
    } catch (e) {}
    return null;
  }

  static void startBroadcastServer(Function(String, String) onDevice) {
    RawDatagramSocket.bind(InternetAddress.anyIPv4, udpPort).then((socket) {
      socket.listen((e) {
        if (e == RawSocketEvent.read) {
          var dg = socket.receive();
          if (dg == null) return;
          String ip = dg.address.address;
          String name = utf8.decode(dg.data);
          onDevice(ip, name);
        }
      });
    });
  }

  static Future<void> broadcast() async {
    String? ip = await getLocalIp();
    if (ip == null) return;
    try {
      final s = await RawDatagramSocket.bind(InternetAddress.anyIPv4, 0);
      s.broadcastEnabled = true;
      s.send(utf8.encode("MyPC_$ip"), InternetAddress("255.255.255.255"), udpPort);
      s.close();
    } catch (e) {}
  }
}