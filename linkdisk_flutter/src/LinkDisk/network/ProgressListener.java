package LinkDisk.network;

public interface ProgressListener {

    void onTotalProgress(int progress);

    void onFileStart(int fileIndex, String fileName);

    void onFileProgress(int fileIndex, String fileName, int progress);

    void onFileComplete(int fileIndex, String fileName);
}