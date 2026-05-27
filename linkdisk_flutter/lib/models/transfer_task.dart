enum TransferDirection {
  send,
  receive,
}

enum TransferStatus {
  waiting,
  transferring,
  completed,
  failed,
  cancelled,
}

class TransferTask {
  final String id;
  final String fileName;
  final String targetDevice;
  final int sizeBytes;
  final TransferDirection direction;
  final TransferStatus status;
  final double progress;

  const TransferTask({
    required this.id,
    required this.fileName,
    required this.targetDevice,
    required this.sizeBytes,
    required this.direction,
    required this.status,
    this.progress = 0,
  });
}
