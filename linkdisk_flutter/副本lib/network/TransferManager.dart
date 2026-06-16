import 'dart:collection';
import 'dart:async';

// 你需要配套的 TransferTask（我按通用结构写好）
class TransferTask {
  final String taskId;
  String status;
  int transferred;
  final int total;

  TransferTask({
    required this.taskId,
    this.status = "waiting",
    this.transferred = 0,
    required this.total,
  });

  void setStatus(String s) => status = s;
  void setTransferred(int t) => transferred = t;

  String getTaskId() => taskId;
  String getStatus() => status;
  int getTransferred() => transferred;
  int getTotal() => total;
}

// 传输监听器
abstract class TransferListener {
  void onProgress(TransferTask task, int progress);
  void onStatusChange(TransferTask task);
}

class TransferManager {
  // 单例
  static TransferManager? _instance;
  static TransferManager get instance => _instance ??= TransferManager._();

  // 3线程池
  final int _maxConcurrent = 3;
  final List<TransferTask> _activeTasks = [];
  final Queue<TransferTask> _waitingQueue = Queue();
  final List<TransferTask> _allTasks = [];

  TransferListener? _listener;

  TransferManager._();

  // 设置监听器
  void setListener(TransferListener listener) {
    _listener = listener;
  }

  // 添加任务
  void addTask(TransferTask task) {
    _allTasks.add(task);
    if (_activeTasks.length < _maxConcurrent) {
      _startTask(task);
    } else {
      _waitingQueue.add(task);
      task.setStatus("waiting");
      _listener?.onStatusChange(task);
    }
  }

  // 启动任务
  void _startTask(TransferTask task) {
    _activeTasks.add(task);
    task.setStatus("running");
    _listener?.onStatusChange(task);

    // 模拟任务执行（真实使用时替换成你的 TCP 发送逻辑）
    Future(() async {
      // 真实传输逻辑在这里执行
      await Future.delayed(const Duration(seconds: 2));

      // 完成
      task.setStatus("completed");
      _activeTasks.remove(task);
      _listener?.onStatusChange(task);
      _processNextTask();
    });
  }

  // 更新进度（外部 TCP 调用）
  void updateProgress(String taskId, int transferred, int total) {
    for (final task in _allTasks) {
      if (task.taskId == taskId) {
        task.setTransferred(transferred);
        final progress = (transferred * 100) ~/ total;
        _listener?.onProgress(task, progress);
        break;
      }
    }
  }

  // 执行下一个任务
  void _processNextTask() {
    if (_waitingQueue.isNotEmpty && _activeTasks.length < _maxConcurrent) {
      _startTask(_waitingQueue.removeFirst());
    }
  }

  // 暂停
  void pauseTask(String taskId) {
    for (final task in _allTasks) {
      if (task.taskId == taskId && task.status == "running") {
        task.setStatus("paused");
        _activeTasks.remove(task);
        _listener?.onStatusChange(task);
        break;
      }
    }
  }

  // 恢复
  void resumeTask(String taskId) {
    for (final task in _allTasks) {
      if (task.taskId == taskId && task.status == "paused") {
        task.setStatus("waiting");
        _waitingQueue.add(task);
        _listener?.onStatusChange(task);
        _processNextTask();
        break;
      }
    }
  }

  // 取消
  void cancelTask(String taskId) {
    for (final task in _allTasks) {
      if (task.taskId == taskId) {
        task.setStatus("cancelled");
        _waitingQueue.remove(task);
        _activeTasks.remove(task);
        _listener?.onStatusChange(task);
        _processNextTask();
        break;
      }
    }
  }

  // 获取所有任务
  List<TransferTask> getAllTasks() {
    return List.unmodifiable(_allTasks);
  }

  // 关闭（Dart 无需手动关闭线程池）
  void shutdown() {}
}