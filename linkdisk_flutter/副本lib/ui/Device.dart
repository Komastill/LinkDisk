import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// 设备展示文本提供者（对应 Java DeviceDisplayProvider）
typedef DeviceDisplayProvider = String Function(String ip);

class DevicePage extends StatefulWidget {
  final DeviceDisplayProvider displayProvider;

  const DevicePage({
    super.key,
    required this.displayProvider,
  });

  @override
  State<DevicePage> createState() => _DevicePageState();
}

class _DevicePageState extends State<DevicePage> {
  final List<String> _deviceList = [];
  final List<String> _selectedIps = [];

  final TextEditingController _localInfoController = TextEditingController();
  final TextEditingController _statusController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _localInfoController.text = "设备名：读取中...\n平台：读取中...\n本机 IP：读取中...";
    _statusController.text = "LinkDisk 已启动，等待设备发现。";
  }

  @override
  void dispose() {
    _localInfoController.dispose();
    _statusController.dispose();
    super.dispose();
  }

  // 对应 setLocalInfoText
  void setLocalInfoText(String text) {
    _localInfoController.text = text;
  }

  // 对应 setStatusMessage
  void setStatusMessage(String message) {
    _statusController.text = message;
  }

  // 对应 getDeviceListModel
  List<String> get deviceList => _deviceList;

  // 对应 getSelectedIp
  String? getSelectedIp() {
    if (_selectedIps.isNotEmpty) return _selectedIps.first;
    return null;
  }

  // 对应 getSelectedIps
  List<String> getSelectedIps() {
    return List.unmodifiable(_selectedIps);
  }

  // 刷新列表
  void repaintDeviceList() {
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: UiStyle.pageBg,
      body: Padding(
        padding: const EdgeInsets.all(26.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 标题区域
            _buildHeader(),
            const SizedBox(height: 20),

            // 主内容区
            Expanded(
              child: _buildMainContent(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          "设备连接",
          style: TextStyle(
            fontSize: 26,
            fontWeight: FontWeight.bold,
            color: UiStyle.text,
          ),
        ),
        SizedBox(height: 8),
        Text(
          "发现局域网设备，查看本机网络信息，建立可信连接",
          style: TextStyle(
            fontSize: 15,
            color: UiStyle.subtext,
          ),
        ),
      ],
    );
  }

  Widget _buildMainContent() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 左侧：本机信息 + 设备列表
        Expanded(
          child: Column(
            children: [
              // 本机信息卡片
              _buildLocalInfoCard(),
              const SizedBox(height: 16),

              // 设备列表卡片
              _buildDeviceListCard(),
            ],
          ),
        ),
        const SizedBox(width: 20),

        // 右侧：操作区 + 状态
        _buildActionCard(),
      ],
    );
  }

  Widget _buildLocalInfoCard() {
    return UiStyle.card(
      child: SizedBox(
        height: 142,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  "本机信息",
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: UiStyle.text,
                  ),
                ),
                UiStyle.compactButton(
                  "复制 IP",
                  onPressed: () async {
                    final ip = _localInfoController.text
                        .split("\n")
                        .last
                        .replaceAll("本机 IP：", "");
                    await Clipboard.setData(ClipboardData(text: ip));
                    setStatusMessage("IP 已复制到剪贴板");
                  },
                ),
              ],
            ),
            const SizedBox(height: 10),
            Expanded(
              child: TextField(
                controller: _localInfoController,
                style: const TextStyle(fontSize: 14, color: UiStyle.subtext),
                maxLines: null,
                expands: true,
                readOnly: true,
                decoration: const InputDecoration(
                  border: InputBorder.none,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDeviceListCard() {
    return UiStyle.card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            "可用设备",
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: UiStyle.text,
            ),
          ),
          const SizedBox(height: 4),
          const Text(
            "同一局域网内运行 LinkDisk 的设备会显示在这里",
            style: TextStyle(fontSize: 13, color: UiStyle.subtext),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: ListView.builder(
              itemCount: _deviceList.length,
              itemBuilder: (context, index) {
                final ip = _deviceList[index];
                final isSelected = _selectedIps.contains(ip);
                final displayText = widget.displayProvider(ip);

                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 10,
                  ),
                  tileColor: isSelected
                      ? UiStyle.primarySoft
                      : UiStyle.softBg,
                  title: Text(
                    displayText,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: UiStyle.text,
                    ),
                  ),
                  selected: isSelected,
                  onTap: () {
                    setState(() {
                      if (isSelected) {
                        _selectedIps.remove(ip);
                      } else {
                        _selectedIps.add(ip);
                      }
                    });
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionCard() {
    return UiStyle.card(
      child: SizedBox(
        width: 270,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                "设备操作",
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: UiStyle.text,
                ),
              ),
              const SizedBox(height: 18),

              // 按钮组
              _buildActionButton("连接设备", onPressed: () {}),
              const SizedBox(height: 10),
              _buildActionButton("断开设备", onPressed: () {}),
              const SizedBox(height: 10),
              _buildActionButton("手动添加", onPressed: () {}),
              const SizedBox(height: 10),
              _buildActionButton("删除设备", onPressed: () {}),
              const SizedBox(height: 10),
              _buildActionButton("刷新列表", onPressed: () {}),
              const SizedBox(height: 22),

              // 状态提示
              const Text(
                "状态提示",
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: UiStyle.primaryDark,
                ),
              ),
              const SizedBox(height: 8),
              Container(
                width: 218,
                height: 90,
                decoration: BoxDecoration(
                  color: const Color(0xFFF4F8FD),
                  border: Border.all(color: const Color(0xFFCCDDF4)),
                  borderRadius: BorderRadius.circular(6),
                ),
                padding: const EdgeInsets.all(12),
                child: TextField(
                  controller: _statusController,
                  readOnly: true,
                  maxLines: null,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: UiStyle.text,
                  ),
                  decoration: const InputDecoration(
                    border: InputBorder.none,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton(String text, {required VoidCallback onPressed}) {
    return SizedBox(
      width: 190,
      height: 44,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: UiStyle.primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
        ),
        child: Text(text),
      ),
    );
  }
}

// 对应 UiStyle
class UiStyle {
  static const Color pageBg = Color(0xFFF7F8FA);
  static const Color text = Color(0xFF222222);
  static const Color subtext = Color(0xFF666666);
  static const Color primary = Color(0xFF3B82F6);
  static const Color primarySoft = Color(0xFFDBEAFE);
  static const Color primaryDark = Color(0xFF1D4ED8);
  static const Color softBg = Color(0xFFF1F5F9);
  static const Color border = Color(0xFFE2E8F0);

  static Widget card({required Widget child}) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: border),
      ),
      padding: const EdgeInsets.all(14),
      child: child,
    );
  }

  static Widget compactButton(String text, {required VoidCallback onPressed}) {
    return SizedBox(
      width: 94,
      height: 34,
      child: OutlinedButton(
        onPressed: onPressed,
        style: OutlinedButton.styleFrom(
          side: BorderSide(color: primary),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        child: Text(
          text,
          style: const TextStyle(fontSize: 13, color: primary),
        ),
      ),
    );
  }
}