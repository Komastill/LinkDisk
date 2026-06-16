import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:io';

class TransferringPage extends StatefulWidget {
  const TransferringPage({super.key});

  @override
  State<TransferringPage> createState() => _TransferringPageState();
}

class _TransferringPageState extends State<TransferringPage> {
  // 对应 Java 接口
  typedef FileDropListener = void Function(List<File> files);
  FileDropListener? fileDropListener;

  final TextEditingController _selectedFilesController = TextEditingController();
  final TextEditingController _statusController = TextEditingController();

  final List<List<String>> _transferTableData = [];
  double _progressValue = 0;

  bool _isDragging = false;
  String? _textBeforeDrag;

  final ScrollController _tableScrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _selectedFilesController.text = "暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。";
    _statusController.text = "请选择文件并连接目标设备。";
  }

  // 对外方法（和 Java 完全同名）
  void setSelectedFilesText(String text) {
    setState(() {
      _textBeforeDrag = null;
      if (text.isEmpty) {
        _selectedFilesController.text = "暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。";
      } else {
        _selectedFilesController.text = text;
      }
    });
  }

  void clearSelectedFilesText() {
    setState(() {
      _textBeforeDrag = null;
      _selectedFilesController.text = "暂无待发送文件。请点击“选择文件”，或把文件/文件夹拖到这里。";
    });
  }

  void setStatusMessage(String message) {
    setState(() {
      _statusController.text = message;
    });
  }

  int addTransferRow(
    String direction,
    String fileName,
    String deviceIp,
    String fileSize,
    String status,
    int progress,
  ) {
    setState(() {
      _transferTableData.add([
        direction,
        fileName,
        deviceIp,
        fileSize,
        status,
        "$progress%",
      ]);
    });
    return _transferTableData.length - 1;
  }

  void updateTransferRow(int rowIndex, String status, int progress) {
    if (rowIndex < 0 || rowIndex >= _transferTableData.length) return;
    setState(() {
      _transferTableData[rowIndex][4] = status;
      _transferTableData[rowIndex][5] = "$progress%";
    });
  }

  void clearTasks() {
    setState(() {
      _transferTableData.clear();
      _progressValue = 0;
    });
  }

  void setProgress(int progress) {
    setState(() {
      _progressValue = progress.toDouble();
    });
  }

  void setFileDropListener(FileDropListener listener) {
    fileDropListener = listener;
  }

  void _setDropHighlight(bool active) {
    setState(() {
      _isDragging = active;
      if (active) {
        _textBeforeDrag ??= _selectedFilesController.text;
        _selectedFilesController.text = "松开鼠标即可添加文件或文件夹...\n\n提示：只有拖到这个待发送文件区域才会加入发送列表。";
      } else {
        if (_textBeforeDrag != null) {
          _selectedFilesController.text = _textBeforeDrag!;
          _textBeforeDrag = null;
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: UiStyle.PAGE_BG,
      padding: const EdgeInsets.fromLTRB(28, 26, 28, 26),
      child: Column(
        children: [
          // 顶部标题 + 按钮
          _buildHeader(),
          const SizedBox(height: 20),

          // 中间卡片
          _buildMainCard(),
          const SizedBox(height: 20),

          // 底部进度 + 状态
          _buildBottomPanel(),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return SizedBox(
      height: 120,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            "文件传输",
            style: TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: UiStyle.TEXT,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            "选择或拖拽文件，确认待发送列表后再开始传输",
            style: TextStyle(
              fontSize: 15,
              color: UiStyle.SUBTEXT,
            ),
          ),
          const SizedBox(height: 20),
          Wrap(
            spacing: 12,
            children: [
              _actionButton("选择文件", 128, () {}),
              _actionButton("管理待发送", 140, () {}),
              _actionButton("重置选择", 128, () {}),
              _actionButton("取消任务", 128, () {}),
              _actionButton("清空任务", 128, () {}),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMainCard() {
    return Container(
      decoration: BoxDecoration(
        color: UiStyle.CARD_BG,
        border: Border.all(color: UiStyle.BORDER),
        borderRadius: BorderRadius.circular(8),
      ),
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 待发送文件标题 + 发送按钮
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                "待发送文件",
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: UiStyle.TEXT,
                ),
              ),
              ElevatedButton(
                onPressed: () {},
                style: ElevatedButton.styleFrom(
                  backgroundColor: UiStyle.PRIMARY,
                  minimumSize: const Size(180, 46),
                ),
                child: const Text(
                  "发送文件",
                  style: TextStyle(color: Colors.white, fontSize: 15),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),

          // 拖拽提示
          Text(
            _isDragging
                ? "已识别拖拽文件，松开鼠标即可加入待发送列表。"
                : "拖拽文件或文件夹到下方区域，确认列表后点击右侧“发送文件”。",
            style: TextStyle(
              fontSize: 13,
              color: _isDragging ? UiStyle.PRIMARY_DARK : UiStyle.SUBTEXT,
            ),
          ),
          const SizedBox(height: 6),

          // 拖拽区域
          _buildDropArea(),
          const SizedBox(height: 16),

          // 传输任务标题
          const Text(
            "传输任务",
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: UiStyle.TEXT,
            ),
          ),
          const SizedBox(height: 12),

          // 传输表格
          Expanded(child: _buildTable()),
        ],
      ),
    );
  }

  Widget _buildDropArea() {
    return DragTarget<dynamic>(
      onWillAccept: (_) {
        _setDropHighlight(true);
        return true;
      },
      onLeave: (_) => _setDropHighlight(false),
      onAccept: (data) {
        _setDropHighlight(false);
        if (fileDropListener != null && data is List<File>) {
          fileDropListener!(data);
        }
      },
      builder: (context, _, __) {
        return Container(
          height: 90,
          decoration: BoxDecoration(
            color: _isDragging
                ? const Color(0xFFE8F3FF)
                : UiStyle.SOFT_BG,
            border: Border.all(
              color: _isDragging ? UiStyle.PRIMARY : UiStyle.BORDER,
              width: 2,
            ),
            borderRadius: BorderRadius.circular(6),
          ),
          child: TextField(
            controller: _selectedFilesController,
            readOnly: true,
            maxLines: null,
            style: const TextStyle(fontSize: 14, color: UiStyle.SUBTEXT),
            decoration: const InputDecoration(
              contentPadding: EdgeInsets.all(12),
              border: InputBorder.none,
            ),
          ),
        );
      },
    );
  }

  Widget _buildTable() {
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: const Color(0xFFEBF1F8)),
      ),
      child: SingleChildScrollView(
        controller: _tableScrollController,
        child: DataTable(
          columnSpacing: 20,
          headingRowColor: MaterialStateColor.resolveWith(
            (_) => const Color(0xFFF4F8FD),
          ),
          headingTextStyle: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
            color: UiStyle.TEXT,
          ),
          dataTextStyle: const TextStyle(fontSize: 14),
          dataRowHeight: 30,
          columns: const [
            DataColumn(label: Text("方向")),
            DataColumn(label: Text("文件名")),
            DataColumn(label: Text("对方设备")),
            DataColumn(label: Text("大小")),
            DataColumn(label: Text("状态")),
            DataColumn(label: Text("进度")),
          ],
          rows: _transferTableData.map((row) {
            return DataRow(
              cells: [
                DataCell(Text(row[0])),
                DataCell(Text(row[1])),
                DataCell(Text(row[2])),
                DataCell(Text(row[3])),
                DataCell(Text(row[4])),
                DataCell(Text(row[5])),
              ],
            );
          }).toList(),
        ),
      ),
    );
  }

  Widget _buildBottomPanel() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.end,
      children: [
        Expanded(
          child: SizedBox(
            height: 26,
            child: LinearProgressIndicator(
              value: _progressValue / 100,
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation(UiStyle.PRIMARY),
            ),
          ),
        ),
        const SizedBox(width: 16),

        // 状态面板
        SizedBox(
          width: 440,
          height: 88,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                "操作提示",
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: UiStyle.PRIMARY_DARK,
                ),
              ),
              const SizedBox(height: 6),
              Container(
                width: 440,
                height: 62,
                decoration: BoxDecoration(
                  color: const Color(0xFFF4F8FD),
                  border: Border.all(const Color(0xFFCCDDF4)),
                ),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                child: TextField(
                  controller: _statusController,
                  readOnly: true,
                  maxLines: null,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: UiStyle.TEXT,
                  ),
                  decoration: const InputDecoration(border: InputBorder.none),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _actionButton(String text, double width, VoidCallback onTap) {
    return SizedBox(
      width: width,
      height: 40,
      child: OutlinedButton(
        onPressed: onTap,
        child: Text(text),
      ),
    );
  }
}

// 完全沿用你 Java 中的颜色
class UiStyle {
  static const Color PAGE_BG = Color(0xFFF7F8FA);
  static const Color CARD_BG = Colors.white;
  static const Color BORDER = Color(0xFFE2E8F0);
  static const Color TEXT = Color(0xFF222222);
  static const Color SUBTEXT = Color(0xFF666666);
  static const Color PRIMARY = Color(0xFF3B82F6);
  static const Color PRIMARY_DARK = Color(0xFF1D4ED8);
  static const Color SOFT_BG = Color(0xFFF1F5F9);
  static const Color PRIMARY_SOFT = Color(0xFFDBEAFE);
}