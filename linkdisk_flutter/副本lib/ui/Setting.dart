import 'package:flutter/material.dart';

class SettingPage extends StatefulWidget {
  const SettingPage({super.key});

  @override
  State<SettingPage> createState() => _SettingPageState();
}

class _SettingPageState extends State<SettingPage> {
  String _receivePath = "received_files";

  // 三个按钮回调（外部可绑定事件）
  VoidCallback? onTrustManager;
  VoidCallback? onChooseReceiveFolder;
  VoidCallback? onOpenReceiveFolder;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: UiStyle.pageBg,
      padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 26),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 标题区域
          _buildHeader(),
          const SizedBox(height: 20),

          // 主卡片
          _buildMainCard(),
        ],
      ),
    );
  }

  // 顶部标题
  Widget _buildHeader() {
    return const Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          "传输设置",
          style: TextStyle(
            fontSize: 26,
            fontWeight: FontWeight.bold,
            color: UiStyle.text,
          ),
        ),
        SizedBox(height: 8),
        Text(
          "先确认接收位置和可信设备，再进入文件传输页面发送文件",
          style: TextStyle(
            fontSize: 15,
            color: UiStyle.subtext,
          ),
        ),
      ],
    );
  }

  // 主卡片
  Widget _buildMainCard() {
    return UiStyle.card(
      child: Column(
        children: [
          // 三个功能块
          _buildActionRow(),
          const SizedBox(height: 18),

          // 接收目录 + 流程指南
          Expanded(
            child: SingleChildScrollView(
              child: Column(
                children: [
                  _buildReceivePathPanel(),
                  const SizedBox(height: 16),
                  _buildWorkflowPanel(),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // 三个设置按钮行
  Widget _buildActionRow() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: _buildSettingBlock(
            "信任设备",
            "查看已授权设备，必要时删除信任关系。",
            UiStyle.actionButton(
              "管理信任设备",
              onPressed: () => onTrustManager?.call(),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _buildSettingBlock(
            "接收目录",
            "设置接收文件保存位置，避免文件散落。",
            UiStyle.actionButton(
              "选择接收目录",
              onPressed: () => onChooseReceiveFolder?.call(),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _buildSettingBlock(
            "接收文件夹",
            "快速打开当前接收目录，查看已收到文件。",
            UiStyle.actionButton(
              "打开接收文件夹",
              onPressed: () => onOpenReceiveFolder?.call(),
            ),
          ),
        ),
      ],
    );
  }

  // 设置块：标题 + 描述 + 按钮
  Widget _buildSettingBlock(String title, String desc, Widget button) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: UiStyle.softBg,
        border: Border.all(color: UiStyle.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: UiStyle.text,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            desc,
            style: const TextStyle(
              fontSize: 13,
              color: UiStyle.subtext,
            ),
          ),
          const SizedBox(height: 12),
          button,
        ],
      ),
    );
  }

  // 当前接收目录面板
  Widget _buildReceivePathPanel() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: const Color(0xFFE8F3FF),
        border: Border.all(color: UiStyle.primary),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            "当前接收目录",
            style: TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.bold,
              color: UiStyle.text,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _receivePath,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.bold,
              color: UiStyle.primaryDark,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            "收到的文件会保存在这里。需要修改位置时，点击上方“选择接收目录”。",
            style: TextStyle(
              fontSize: 13,
              color: UiStyle.subtext,
            ),
          ),
        ],
      ),
    );
  }

  // 推荐操作流程
  Widget _buildWorkflowPanel() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: UiStyle.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            "推荐操作流程",
            style: TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.bold,
              color: UiStyle.text,
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _buildStepCard(
                  "1",
                  "连接设备",
                  "在“设备连接”页面发现并连接目标设备。",
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: _buildStepCard(
                  "2",
                  "确认设置",
                  "确认接收目录，必要时管理信任设备。",
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: _buildStepCard(
                  "3",
                  "选择并发送",
                  "进入“文件传输”页面，拖拽或选择文件后发送。",
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // 步骤卡片
  Widget _buildStepCard(String number, String title, String desc) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: UiStyle.softBg,
        border: Border.all(color: UiStyle.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "$number. $title",
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: UiStyle.text,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            desc,
            style: const TextStyle(
              fontSize: 12.5,
              color: UiStyle.subtext,
            ),
          ),
        ],
      ),
    );
  }

  // 供外部调用
  void setReceivePathText(String path) {
    setState(() {
      _receivePath = path.trim().isEmpty ? "未设置" : path;
    });
  }

  void setStatusMessage(String msg) {}
  void clearLog() {}
}

// 样式统一管理
class UiStyle {
  static const Color pageBg = Color(0xFFF7F8FA);
  static const Color text = Color(0xFF222222);
  static const Color subtext = Color(0xFF666666);
  static const Color primary = Color(0xFF3B82F6);
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
      padding: const EdgeInsets.all(18),
      child: child,
    );
  }

  static Widget actionButton(String text, {required VoidCallback onPressed}) {
    return SizedBox(
      width: double.infinity,
      height: 42,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(6),
          ),
        ),
        child: Text(
          text,
          style: const TextStyle(fontSize: 14, color: Colors.white),
        ),
      ),
    );
  }
}