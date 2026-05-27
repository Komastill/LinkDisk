import 'package:flutter/material.dart';

import '../core/app_colors.dart';

class Sidebar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onChanged;

  const Sidebar({
    super.key,
    required this.currentIndex,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final menus = [
      _MenuItem('设备连接', Icons.devices_rounded),
      _MenuItem('传输设置', Icons.tune_rounded),
      _MenuItem('文件传输', Icons.folder_copy_rounded),
    ];

    return Container(
      width: 260,
      color: AppColors.sidebarBg,
      padding: const EdgeInsets.fromLTRB(28, 42, 28, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const BrandHeader(),
          const SizedBox(height: 52),
          for (int i = 0; i < menus.length; i++) ...[
            NavButton(
              title: menus[i].title,
              icon: menus[i].icon,
              selected: currentIndex == i,
              onTap: () => onChanged(i),
            ),
            const SizedBox(height: 18),
          ],
          const Spacer(),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white.withOpacity(0.70),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: AppColors.border),
            ),
            child: const Text(
              'Java MVP 已完成\nFlutter 跨平台版开发中',
              style: TextStyle(
                height: 1.5,
                fontSize: 13,
                color: AppColors.subText,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class BrandHeader extends StatelessWidget {
  const BrandHeader({super.key});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          height: 54,
          width: 7,
          decoration: BoxDecoration(
            color: AppColors.primary,
            borderRadius: BorderRadius.circular(99),
          ),
        ),
        const SizedBox(width: 16),
        const Text(
          'LinkDisk',
          style: TextStyle(
            fontSize: 33,
            fontWeight: FontWeight.w900,
            color: AppColors.text,
            letterSpacing: -1.2,
          ),
        ),
      ],
    );
  }
}

class NavButton extends StatefulWidget {
  final String title;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  const NavButton({
    super.key,
    required this.title,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  @override
  State<NavButton> createState() => _NavButtonState();
}

class _NavButtonState extends State<NavButton> {
  bool hovering = false;
  bool pressing = false;

  @override
  Widget build(BuildContext context) {
    final active = widget.selected;

    Color bg;
    if (active) {
      bg = AppColors.primaryLight;
    } else if (pressing) {
      bg = const Color(0xFFE9F2FF);
    } else if (hovering) {
      bg = const Color(0xFFF5F9FF);
    } else {
      bg = Colors.white;
    }

    return MouseRegion(
      cursor: SystemMouseCursors.click,
      onEnter: (_) => setState(() => hovering = true),
      onExit: (_) => setState(() {
        hovering = false;
        pressing = false;
      }),
      child: GestureDetector(
        onTapDown: (_) => setState(() => pressing = true),
        onTapUp: (_) => setState(() => pressing = false),
        onTapCancel: () => setState(() => pressing = false),
        onTap: widget.onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          height: 66,
          padding: const EdgeInsets.symmetric(horizontal: 18),
          decoration: BoxDecoration(
            color: bg,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(
              color: active ? AppColors.primary : AppColors.border,
              width: active ? 2 : 1,
            ),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(active ? 0.07 : 0.035),
                blurRadius: active ? 16 : 10,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Row(
            children: [
              Icon(
                widget.icon,
                size: 23,
                color: active ? AppColors.primary : AppColors.subText,
              ),
              const SizedBox(width: 14),
              Text(
                widget.title,
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w900,
                  color: active ? AppColors.text : const Color(0xFF2D384B),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _MenuItem {
  final String title;
  final IconData icon;

  _MenuItem(this.title, this.icon);
}
