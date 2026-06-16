// 设备发现监听器（对应 Java interface）
abstract class DeviceFoundListener {
  void onDeviceFound(String ip, String deviceName, String platform);
}