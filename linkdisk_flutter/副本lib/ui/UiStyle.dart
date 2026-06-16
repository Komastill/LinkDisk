import 'package:flutter/material.dart';

class UiStyle {
  // ===================== 颜色常量（完全和 Java 一致）=====================
  static const Color PAGE_BG = Color(0xFFF7FAFE);
  static const Color SIDEBAR_BG = Color(0xFFEEF4FC);
  static const Color CARD_BG = Colors.white;
  static const Color SOFT_BG = Color(0xFFF9FBFE);
  static const Color BORDER = Color(0xFFDCE4EE);
  static const Color TEXT = Color(0xFF182638);
  static const Color SUBTEXT = Color(0xFF5F6F84);
  static const Color PRIMARY = Color(0xFF3A78D5);
  static const Color PRIMARY_DARK = Color(0xFF2A5BA6);
  static const Color PRIMARY_SOFT = Color(0xFFE6F1FF);
  static const Color HOVER_BG = Color(0xFFF2F7FF);
  static const Color PRESSED_BG = Color(0xFFDBEAFE);
  static const Color SUCCESS = Color(0xFF21805B);
  static const Color WARNING = Color(0xFFAF7426);
  static const Color ACCENT_SOFT = Color(0xFFF4F8FD);

  // 私有构造，单例工具类
  UiStyle._();

  // ===================== 按钮样式 =====================

  /// 普通动作按钮（对应 createActionButton）
  static Widget actionButton({
    required String text,
    required VoidCallback onTap,
    double width = 120,
    double height = 40,
  }) {
    return _StatefulButton(
      text: text,
      width: width,
      height: height,
      isPrimary: false,
      onTap: onTap,
    );
  }

  /// 主要按钮（对应 createPrimaryButton）
  static Widget primaryButton({
    required String text,
    required VoidCallback onTap,
    double width = 180,
    double height = 46,
  }) {
    return _StatefulButton(
      text: text,
      width: width,
      height: height,
      isPrimary: true,
      onTap: onTap,
    );
  }

  /// 紧凑按钮（对应 createCompactButton）
  static Widget compactButton({
    required String text,
    required VoidCallback onTap,
    double width = 120,
    double height = 40,
  }) {
    return SizedBox(
      width: width,
      height: height,
      child: actionButton(
        text: text,
        onTap: onTap,
        width: width,
        height: height,
      ),
    );
  }

  // ===================== 容器样式 =====================

  /// 卡片样式（对应 setPanelCardStyle）
  static BoxDecoration cardDecoration() {
    return BoxDecoration(
      color: CARD_BG,
      border: Border.all(color: BORDER),
      borderRadius: BorderRadius.circular(6),
    );
  }

  /// 输入框 / 拖拽区域 普通边框
  static BoxDecoration dropNormalDecoration() {
    return BoxDecoration(
      color: SOFT_BG,
      border: Border.all(color: BORDER),
      borderRadius: BorderRadius.circular(6),
    );
  }

  /// 拖拽区域 激活高亮边框
  static BoxDecoration dropActiveDecoration() {
    return BoxDecoration(
      color: PRIMARY_SOFT,
      border: Border.all(color: PRIMARY, width: 2),
      borderRadius: BorderRadius.circular(6),
    );
  }
}

// ===================== 带 Hover / Pressed 状态的按钮 =====================
// 完全复刻 Java 按钮的鼠标进入、离开、按下、释放效果
class _StatefulButton extends StatefulWidget {
  final String text;
  final double width;
  final double height;
  final bool isPrimary;
  final VoidCallback onTap;

  const _StatefulButton({
    required this.text,
    required this.width,
    required this.height,
    required this.isPrimary,
    required this.onTap,
  });

  @override
  State<_StatefulButton> createState() => _StatefulButtonState();
}

class _StatefulButtonState extends State<_StatefulButton> {
  bool _isHovered = false;
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    Color bgColor;
    Color textColor;
    Border border;

    if (widget.isPrimary) {
      // 主按钮样式
      if (_isPressed) {
        bgColor = const Color(0xFF1D4E94);
      } else if (_isHovered) {
        bgColor = UiStyle.PRIMARY_DARK;
      } else {
        bgColor = UiStyle.PRIMARY;
      }
      textColor = Colors.white;
      border = Border(
        left: BorderSide(color: UiStyle.PRIMARY_DARK, width: 5),
        top: BorderSide(color: UiStyle.PRIMARY_DARK, width: 1),
        bottom: BorderSide(color: UiStyle.PRIMARY_DARK, width: 1),
        right: BorderSide(color: UiStyle.PRIMARY_DARK, width: 1),
      );
    } else {
      // 普通按钮样式
      if (_isPressed) {
        bgColor = UiStyle.PRESSED_BG;
        textColor = UiStyle.PRIMARY_DARK;
        border = _buttonBorder(UiStyle.PRIMARY_DARK);
      } else if (_isHovered) {
        bgColor = UiStyle.HOVER_BG;
        textColor = UiStyle.PRIMARY_DARK;
        border = _buttonBorder(UiStyle.PRIMARY);
      } else {
        bgColor = Colors.white;
        textColor = UiStyle.TEXT;
        border = _buttonBorder(UiStyle.BORDER);
      }
    }

    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: GestureDetector(
        onTapDown: (_) => setState(() => _isPressed = true),
        onTapUp: (_) => setState(() => _isPressed = false),
        onTapCancel: () => setState(() => _isPressed = false),
        onTap: widget.onTap,
        child: Container(
          width: widget.width,
          height: widget.height,
          decoration: BoxDecoration(
            color: bgColor,
            border: border,
          ),
          child: Center(
            child: Text(
              widget.text,
              style: TextStyle(
                fontSize: widget.isPrimary ? 15 : 14,
                fontWeight: FontWeight.bold,
                color: textColor,
              ),
            ),
          ),
        ),
      ),
    );
  }

  // 普通按钮左侧高亮边框（完全复刻 Java）
  Border _buttonBorder(Color leftColor) {
    return Border(
      left: BorderSide(color: leftColor, width: 4),
      top: BorderSide(color: leftColor, width: 1),
      bottom: BorderSide(color: leftColor, width: 1),
      right: BorderSide(color: UiStyle.BORDER, width: 1),
    );
  }
}