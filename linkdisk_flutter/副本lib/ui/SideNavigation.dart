import 'package:flutter/material.dart';

class SideNavigation extends StatelessWidget {
  final String active;
  final VoidCallback onDevice;
  final VoidCallback onTransfer;
  final VoidCallback onSettings;

  const SideNavigation({
    super.key,
    required this.active,
    required this.onDevice,
    required this.onTransfer,
    required this.onSettings,
  });

  // 完全对应 Java 中的颜色
  static const Color sidebarBg = Color(0xFFEBF2FB);
  static const Color text = Color(0xFF182638);
  static const Color buttonBg = Color(0xFFF8FBFF);
  static const Color buttonHoverBg = Color(0xFFECF6FF);
  static const Color buttonActiveBg = Color(0xFFDAEAFD);
  static const Color border = Color(0xFFD8E2EF);
  static const Color activeColor = Color(0xFF3877D2);
  static const Color dividerColor = Color(0xFFDCE5F0);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 230, // 完全和 Java 宽度一致
      decoration: const BoxDecoration(
        color: sidebarBg,
        border: Border(
          right: BorderSide(color: dividerColor, width: 1),
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 38),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 品牌标题 LinkDisk
            const Text(
              "LinkDisk",
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.bold,
                color: text,
              ),
            ),
            const SizedBox(height: 10),

            // 蓝色小横线
            Container(
              width: 46,
              height: 4,
              color: activeColor,
            ),

            const SizedBox(height: 42),

            // 导航按钮
            _navButton(
              title: "设备连接",
              isActive: active == "device",
              onTap: onDevice,
            ),
            const SizedBox(height: 18),

            _navButton(
              title: "传输设置",
              isActive: active == "settings",
              onTap: onSettings,
            ),
            const SizedBox(height: 18),

            _navButton(
              title: "文件传输",
              isActive: active == "transfer",
              onTap: onTransfer,
            ),
          ],
        ),
      ),
    );
  }

  // 导航按钮：完全还原样式、状态、边框
  Widget _navButton({
    required String title,
    required bool isActive,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      hoverColor: buttonHoverBg,
      borderRadius: BorderRadius.zero,
      child: Container(
        width: 186,
        height: 58,
        decoration: BoxDecoration(
          color: isActive ? buttonActiveBg : buttonBg,
          border: Border(
            left: BorderSide(
              color: isActive ? activeColor : border,
              width: 6,
            ),
            top: BorderSide(
              color: isActive ? activeColor : border,
              width: 1,
            ),
            bottom: BorderSide(
              color: isActive ? activeColor : border,
              width: 1,
            ),
            right: BorderSide(
              color: border,
              width: 1,
            ),
          ),
        ),
        child: Center(
          child: Text(
            title,
            style: const TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.bold,
              color: text,
            ),
          ),
        ),
      ),
    );
  }
}