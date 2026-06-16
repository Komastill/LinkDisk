// 对应 Java 的 ProgressListener 接口
abstract class ProgressListener {
  void onTotalProgress(int progress);

  void onFileStart(int fileIndex, String fileName);

  void onFileProgress(int fileIndex, String fileName, int progress);

  void onFileComplete(int fileIndex, String fileName);
}