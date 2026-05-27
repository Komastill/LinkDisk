import 'package:flutter/material.dart';

import '../core/app_colors.dart';
import '../widgets/app_button.dart';
import '../widgets/info_card.dart';
import '../widgets/page_shell.dart';
import '../widgets/status_box.dart';

class FileTransferPage extends StatelessWidget {
  const FileTransferPage({super.key});

  @override
  Widget build(BuildContext context) {
    return PageShell(
      title: '文件传输',
      subtitle: '选择或拖拽文件，确认待发送列表后开始传输',
      child: Column(
        children: [
          Wrap(
            spacing: 14,
            runSpacing: 14,
            children: [
              AppButton(text: '选择文件', icon: Icons.upload_file_rounded, onTap: () {}),
              AppButton(text: '管理待发送', icon: Icons.account_tree_rounded, onTap: () {}),
              AppButton(text: '重置选择', icon: Icons.restart_alt_rounded, onTap: () {}),
              AppButton(text: '取消任务', icon: Icons.cancel_outlined, onTap: () {}),
              AppButton(text: '清空任务', icon: Icons.cleaning_services_rounded, onTap: () {}),
            ],
          ),
          const SizedBox(height: 24),
          Expanded(
            child: InfoCard(
              title: '待发送文件',
              child: Column(
                children: [
                  Row(
                    children: [
                      const Expanded(
                        child: Text(
                          '拖拽文件或文件夹到下方区域，确认列表后点击右侧“发送文件”。',
                          style: TextStyle(
                            fontSize: 15,
                            color: AppColors.subText,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      const SizedBox(width: 18),
                      AppPrimaryButton(text: '发送文件', onTap: () {}),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Container(
                    height: 120,
                    width: double.infinity,
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF8FAFE),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: const Text(
                      '暂无待发送文件。可以把文件或文件夹拖到这里。',
                      style: TextStyle(
                        fontSize: 15,
                        color: AppColors.subText,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  const SectionTitle('传输任务'),
                  const SizedBox(height: 12),
                  Expanded(
                    child: Container(
                      width: double.infinity,
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: AppColors.border),
                      ),
                      child: const Center(
                        child: Text(
                          '暂无传输任务',
                          style: TextStyle(
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
          ),
          const SizedBox(height: 22),
          Row(
            children: [
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(99),
                  child: const LinearProgressIndicator(
                    value: 0,
                    minHeight: 10,
                    backgroundColor: Color(0xFFE3E9F3),
                    color: AppColors.primary,
                  ),
                ),
              ),
              const SizedBox(width: 20),
              const Text(
                '0%',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.w900,
                  color: AppColors.text,
                ),
              ),
              const SizedBox(width: 28),
              const SizedBox(
                width: 390,
                child: StatusBox(text: '当前没有待发送文件。'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
