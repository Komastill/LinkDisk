class Device {
  final String id;
  final String name;
  final String ip;
  final String platform;
  final bool trusted;
  final bool connected;

  const Device({
    required this.id,
    required this.name,
    required this.ip,
    required this.platform,
    this.trusted = false,
    this.connected = false,
  });
}
