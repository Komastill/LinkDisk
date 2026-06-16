package LinkDisk.model;

import java.io.File;

public class TransferFileItem {

    private File sourceFile;

    private String relativePath;

    private long size;

    public TransferFileItem(File sourceFile, String relativePath) {
        this.sourceFile = sourceFile;
        this.relativePath = relativePath;
        this.size = sourceFile.length();
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }
}