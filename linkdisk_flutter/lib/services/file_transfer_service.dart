import '../models/transfer_task.dart';

class FileTransferService {
  Future<void> sendFiles(List<String> paths, String targetIp) async {
    // TODO: 后续迁移 Java MVP 中的 TCP 文件发送逻辑
  }

  Stream<TransferTask> watchTransferTasks() async* {
    // TODO: 后续接入真实传输任务状态
  }
}
