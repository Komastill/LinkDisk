import 'package:flutter/material.dart';

import '../core/app_colors.dart';

class AppButton extends StatefulWidget {
  final String text;
  final IconData icon;
  final VoidCallback onTap;

  const AppButton({
    super.key,
    required this.text,
    required this.icon,
    required this.onTap,
  });

  @override
  State<AppButton> createState() => _AppButtonState();
}

class _AppButtonState extends State<AppButton> {
  bool hovering = false;
  bool pressing = false;

  @override
  Widget build(BuildContext context) {
    Color bg;

    if (pressing) {
      bg = const Color(0xFFE7F0FF);
    } else if (hovering) {
      bg = AppColors.primaryLight;
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
          height: 48,
          padding: const EdgeInsets.symmetric(horizontal: 18),
          decoration: BoxDecoration(
            color: bg,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(
              color: hovering ? AppColors.primary : AppColors.border,
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.035),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(widget.icon, size: 19, color: AppColors.primary),
              const SizedBox(width: 8),
              Text(
                widget.text,
                style: const TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w900,
                  color: AppColors.text,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class AppPrimaryButton extends StatelessWidget {
  final String text;
  final VoidCallback onTap;

  const AppPrimaryButton({
    super.key,
    required this.text,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return FilledButton.icon(
      onPressed: onTap,
      icon: const Icon(Icons.send_rounded),
      label: Text(text),
      style: FilledButton.styleFrom(
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 26, vertical: 18),
        textStyle: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w900,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
    );
  }
}
