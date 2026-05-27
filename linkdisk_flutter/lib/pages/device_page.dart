import 'package:flutter/material.dart';

import '../core/app_colors.dart';
import '../widgets/app_button.dart';
import '../widgets/info_card.dart';
import '../widgets/page_shell.dart';
import '../widgets/status_box.dart';

class DevicePage extends StatelessWidget {
  const DevicePage({super.key});

  @override
  Widget build(BuildContext context) {
    return PageShell(
      title: '设备连接',
      subtitle: '发现局域网设备，查看本机网络信息，建立可信连接',
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Expanded(
            child: Column(
              children: [
                SizedBox(
                  height: 215,
                  child: InfoCard(
                    title: '本机信息',
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        const Expanded(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              InfoLine(label: '设备名', value: 'MacBook-Air-7.local'),
                              InfoLine(label: '平台', value: 'macOS'),
                              InfoLine(label: '本机 IP', value: '172.20.10.3'),
                            ],
                          ),
                        ),
                        const SizedBox(width: 18),
                        AppButton(
                          text: '复制 IP',
                          icon: Icons.copy_rounded,
                          onTap: () {},
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                Expanded(
                  child: InfoCard(
                    title: '可用设备',
                    subtitle: '同一局域网内运行 LinkDisk 的设备会显示在这里',
                    child: Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(18),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF8FAFE),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.border),
                      ),
                      child: const Text(
                        '暂无设备，等待局域网发现...',
                        style: TextStyle(
                          fontSize: 15,
                          color: AppColors.subText,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 28),
          SizedBox(
            width: 320,
            child: InfoCard(
              title: '设备操作',
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  AppButton(text: '连接设备', icon: Icons.link_rounded, onTap: () {}),
                  const SizedBox(height: 14),
                  AppButton(text: '断开设备', icon: Icons.link_off_rounded, onTap: () {}),
                  const SizedBox(height: 14),
                  AppButton(text: '手动添加', icon: Icons.add_rounded, onTap: () {}),
                  const SizedBox(height: 14),
                  AppButton(text: '删除设备', icon: Icons.delete_outline_rounded, onTap: () {}),
                  const SizedBox(height: 14),
                  AppButton(text: '刷新列表', icon: Icons.refresh_rounded, onTap: () {}),
                  const Spacer(),
                  const StatusBox(text: 'LinkDisk 已启动，等待设备发现。'),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
