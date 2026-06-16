import 'dart:uuid';
import 'package:intl/intl.dart';

class TransferTask {
  late String taskId;
  String? targetIp;
  String? fileName;
  int totalSize = 0;
  int transferred = 0;
  String? status; // waiting, running, paused, completed, failed, cancelled
  String? type; // upload, download
  late int createTime;

  // 无参构造
  TransferTask() {
    taskId = Uuid().v4();
    createTime = DateTime.now().millisecondsSinceEpoch;
    transferred = 0;
  }

  // 带参构造
  TransferTask.create(String targetIp, String fileName, int totalSize, String type) {
    // 调用基础初始化
    taskId = Uuid().v4();
    createTime = DateTime.now().millisecondsSinceEpoch;
    transferred = 0;

    this.targetIp = targetIp;
    this.fileName = fileName;
    this.totalSize = totalSize;
    this.type = type;
  }

  // Getters & Setters
  String getTaskId() => taskId;
  void setTaskId(String taskId) => this.taskId = taskId;

  String? getTargetIp() => targetIp;
  void setTargetIp(String? targetIp) => this.targetIp = targetIp;

  String? getFileName() => fileName;
  void setFileName(String? fileName) => this.fileName = fileName;

  int getTotalSize() => totalSize;
  void setTotalSize(int totalSize) => this.totalSize = totalSize;

  int getTransferred() => transferred;
  void setTransferred(int transferred) => this.transferred = transferred;

  String? getStatus() => status;
  void setStatus(String? status) => this.status = status;

  String? getType() => type;
  void setType(String? type) => this.type = type;

  int getCreateTime() => createTime;

  // 获取进度百分比
  int getProgress() {
    if (totalSize == 0) return 0;
    return (transferred * 100) ~/ totalSize;
  }
}