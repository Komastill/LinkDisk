import 'package:flutter/material.dart';

import '../core/app_colors.dart';
import '../widgets/app_button.dart';
import '../widgets/info_card.dart';
import '../widgets/page_shell.dart';

class TransferSettingPage extends StatelessWidget {
  const TransferSettingPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PageShell(
      title: '传输设置',
      subtitle: '配置接收目录，管理可信设备，再进入文件传输页面发送文件',
      child: Column(
        children: [
          SizedBox(
            height: 220,
            child: Row(
              children: [
                Expanded(
                  child: InfoCard(
                    title: '信任设备',
                    subtitle: '查看已授权设备，必要时删除信任关系。',
                    child: AppButton(
                      text: '管理信任设备',
                      icon: Icons.verified_user_rounded,
                      onTap: () {},
                    ),
                  ),
                ),
                const SizedBox(width: 24),
                Expanded(
                  child: InfoCard(
                    title: '接收目录',
                    subtitle: '设置接收文件保存位置，便于管理下载内容。',
                    child: AppButton(
                      text: '选择接收目录',
                      icon: Icons.folder_open_rounded,
                      onTap: () {},
                    ),
                  ),
                ),
                const SizedBox(width: 24),
                Expanded(
                  child: InfoCard(
                    title: '接收文件夹',
                    subtitle: '快速打开当前接收目录，查看已收到的文件。',
                    child: AppButton(
                      text: '打开接收文件夹',
                      icon: Icons.open_in_new_rounded,
                      onTap: () {},
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 26),
          Expanded(
            child: InfoCard(
              title: '推荐操作流程',
              child: Row(
                children: const [
                  StepCard(
                    number: '01',
                    title: '连接设备',
                    desc: '先在设备连接页面发现并连接目标设备。',
                  ),
                  SizedBox(width: 20),
                  StepCard(
                    number: '02',
                    title: '确认目录',
                    desc: '在当前页面确认接收文件保存位置。',
                  ),
                  SizedBox(width: 20),
                  StepCard(
                    number: '03',
                    title: '发送文件',
                    desc: '进入文件传输页，选择或拖拽文件后发送。',
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 18),
            decoration: BoxDecoration(
              color: AppColors.primaryLight,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: AppColors.primary.withOpacity(0.35)),
            ),
            child: const Text(
              '当前接收目录：received_files',
              style: TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.w800,
                color: AppColors.text,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
