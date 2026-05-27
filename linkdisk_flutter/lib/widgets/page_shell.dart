import 'package:flutter/material.dart';

import '../core/app_colors.dart';

class PageShell extends StatelessWidget {
  final String title;
  final String subtitle;
  final Widget child;

  const PageShell({
    super.key,
    required this.title,
    required this.subtitle,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.pageBg,
      padding: const EdgeInsets.fromLTRB(42, 38, 42, 34),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 34,
              fontWeight: FontWeight.w900,
              color: AppColors.text,
              letterSpacing: -0.6,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            subtitle,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: AppColors.subText,
            ),
          ),
          const SizedBox(height: 28),
          Expanded(child: child),
        ],
      ),
    );
  }
}
