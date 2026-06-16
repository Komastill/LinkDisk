package LinkDisk.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class TcpServer {

    private static AuthManager authManager = new AuthManager();

    private static AuthCallback authCallback;

    private static ReceiveCallback receiveCallback;

    private static ServerSocket serverSocket;

    private static boolean isRunning = false;

    public interface AuthCallback {
        boolean onAuthRequest(String ip);
    }

    public interface ReceiveCallback {

        void onFileReceiveStart(
                String clientIp,
                String fileName,
                String savePath,
                long fileSize);

        void onFileReceiveProgress(
                String clientIp,
                String fileName,
                String savePath,
                long fileSize,
                long receivedBytes,
                int progress);

        void onFileReceived(
                String clientIp,
                String fileName,
                String savePath,
                long fileSize);
    }

    private static String formatFileSize(long size) {
        double value = size;

        if (size < 1000) {
            return size + " B";
        }

        value = value / 1000;
        if (value < 1000) {
            return String.format(java.util.Locale.US, "%.2f KB", value);
        }

        value = value / 1000;
        if (value < 1000) {
            return String.format(java.util.Locale.US, "%.2f MB", value);
        }

        value = value / 1000;
        return String.format(java.util.Locale.US, "%.2f GB", value);
    }

    public static void startServer(AuthCallback callback) {
        startServer(callback, null);
    }

    public static void startServer(AuthCallback callback, ReceiveCallback receiveCallbackParam) {
        if (isRunning) {
            return;
        }

        authCallback = callback;
        receiveCallback = receiveCallbackParam;
        isRunning = true;

        new File(AppSettings.getReceiveDir()).mkdirs();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(6000);
                    System.out.println("TCP服务器已启动，端口：6000");

                    while (isRunning) {

                        Socket socket = serverSocket.accept();

                        String clientIp = socket.getInetAddress().getHostAddress();

                        System.out.println("收到连接请求来自：" + clientIp);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                handleConnection(socket, clientIp);
                            }
                        }).start();
                    }

                } catch (Exception e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static void stopServer() {
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleConnection(Socket socket, String clientIp) {

        try {

            DataInputStream in = new DataInputStream(socket.getInputStream());

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String command = in.readUTF();

            System.out.println("收到命令：" + command + " 来自：" + clientIp);

            boolean authorized = true;

            if (!authManager.isTrusted(clientIp)) {

                authorized = false;

                if (authCallback != null) {
                    authorized = authCallback.onAuthRequest(clientIp);
                }

                if (authorized) {
                    authManager.addTrustedDevice(clientIp);
                    System.out.println("已授权设备：" + clientIp);
                } else {
                    System.out.println("拒绝设备连接：" + clientIp);
                }
            } else {
                System.out.println("信任设备已连接：" + clientIp);
            }

            if (!authorized) {
                out.writeUTF("DENIED");
                out.flush();
                in.close();
                out.close();
                socket.close();
                return;
            }

            if ("AUTH".equals(command)) {

                out.writeUTF("OK");
                out.writeUTF(UdpListener.getThisDeviceName());
                out.writeUTF(UdpListener.getThisPlatform());
                out.flush();

                System.out.println("设备 " + clientIp + " 连接授权完成");

            } else if ("FILE".equals(command)) {

                out.writeUTF("OK");
                out.flush();

                receiveFiles(in, clientIp);

            } else if ("LIST".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String path = normalizeRemotePath(in.readUTF());
                String json = listDirectory(path);
                out.writeUTF(json);
                out.flush();

            } else if ("DRIVES".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String json = listDrives();
                out.writeUTF(json);
                out.flush();

            } else if ("DELETE".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String targetPath = normalizeRemotePath(in.readUTF());
                boolean success = false;
                try {
                    if (isSafePath(targetPath)) {
                        File f = new File(targetPath);
                        success = f.delete();
                        if (f.isDirectory() && !success) {
                            success = deleteDir(f);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                out.writeUTF(success ? "OK" : "FAILED");
                out.flush();

            } else if ("MKDIR".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String dirPath = normalizeRemotePath(in.readUTF());
                boolean success = false;
                try {
                    if (isSafePath(dirPath)) {
                        success = new File(dirPath).mkdirs();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                out.writeUTF(success ? "OK" : "FAILED");
                out.flush();

            } else if ("RENAME".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String oldPath = normalizeRemotePath(in.readUTF());
                String newPath = normalizeRemotePath(in.readUTF());
                boolean success = false;
                try {
                    if (isSafePath(oldPath) && isSafePath(newPath)) {
                        success = new File(oldPath).renameTo(new File(newPath));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                out.writeUTF(success ? "OK" : "FAILED");
                out.flush();

            } else if ("MOVE".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String src = normalizeRemotePath(in.readUTF());
                String dest = normalizeRemotePath(in.readUTF());
                boolean success = false;
                try {
                    if (isSafePath(src) && isSafePath(dest)) {
                        Files.move(new File(src).toPath(), new File(dest).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        success = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                out.writeUTF(success ? "OK" : "FAILED");
                out.flush();

            } else if ("COPY".equals(command)) {
                out.writeUTF("OK");
                out.flush();
                String src = normalizeRemotePath(in.readUTF());
                String dest = normalizeRemotePath(in.readUTF());
                boolean success = false;
                try {
                    if (isSafePath(src) && isSafePath(dest)) {
                        Files.copy(new File(src).toPath(), new File(dest).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        success = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                out.writeUTF(success ? "OK" : "FAILED");
                out.flush();

            } else if ("DOWNLOAD".equals(command)) {
                String filePath = normalizeRemotePath(in.readUTF());
                File file = new File(filePath);
                if (file.exists() && file.isFile() && isSafePath(filePath)) {
                    out.writeUTF("OK");
                    out.writeLong(file.length());
                    out.flush();
                    FileInputStream fileIn = new FileInputStream(file);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fileIn.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    fileIn.close();
                } else {
                    out.writeUTF("FAILED");
                }
                out.flush();

            } else {
                out.writeUTF("DENIED");
                out.flush();
                System.out.println("未知请求类型：" + command);
            }

            in.close();
            out.close();
            socket.close();

        } catch (Exception e) {
            System.err.println("处理设备 " + clientIp + " 连接时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String normalizeRemotePath(String path) {
        if (path == null || path.length() == 0) {
            return System.getProperty("user.home");
        }

        String normalized = path.replace("\\", "/");

        if ("~".equals(normalized)) {
            return System.getProperty("user.home");
        }

        if (normalized.startsWith("~/")) {
            return System.getProperty("user.home") + normalized.substring(1);
        }

        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }

        return normalized;
    }

    private static boolean isSafePath(String path) {
        String lower = normalizeRemotePath(path).toLowerCase();
        String[] forbidden = {
                "c:/windows",
                "c:/program files",
                "c:/program files (x86)",
                "system32"
        };
        for (String f : forbidden) {
            if (lower.contains(f))
                return false;
        }
        return true;
    }

    private static boolean deleteDir(File dir) {
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!deleteDir(f))
                    return false;
            }
        }
        return dir.delete();
    }

    private static String sanitizeRelativePath(String path) {

        if (path == null || path.trim().length() == 0) {
            return "unnamed_file";
        }

        String normalized = path.replace("\\", "/");

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        String[] parts = normalized.split("/");

        StringBuilder safePath = new StringBuilder();

        for (String part : parts) {

            if (part == null || part.trim().length() == 0) {
                continue;
            }

            if (".".equals(part) || "..".equals(part)) {
                continue;
            }

            part = part.replace(":", "_")
                    .replace("*", "_")
                    .replace("?", "_")
                    .replace("\"", "_")
                    .replace("<", "_")
                    .replace(">", "_")
                    .replace("|", "_");

            if (safePath.length() > 0) {
                safePath.append(File.separator);
            }

            safePath.append(part);
        }

        if (safePath.length() == 0) {
            return "unnamed_file";
        }

        return safePath.toString();
    }

    private static File buildUniqueSaveFile(File originalFile) {

        if (!originalFile.exists()) {
            return originalFile;
        }

        File parent = originalFile.getParentFile();

        String name = originalFile.getName();

        String baseName = name;

        String ext = "";

        int dotIndex = name.lastIndexOf(".");

        if (dotIndex != -1) {
            baseName = name.substring(0, dotIndex);
            ext = name.substring(dotIndex);
        }

        int count = 1;

        File saveFile;

        do {
            saveFile = new File(parent, baseName + "(" + count + ")" + ext);
            count++;
        } while (saveFile.exists());

        return saveFile;
    }

    private static void receiveFiles(DataInputStream in, String clientIp) {

        try {

            int fileCount = in.readInt();

            System.out.println("从 " + clientIp + " 接收文件数量：" + fileCount);

            for (int i = 0; i < fileCount; i++) {

                String relativePath = in.readUTF();

                String safeRelativePath = sanitizeRelativePath(relativePath);

                long fileSize = in.readLong();

                System.out.println(
                        "接收文件：" +
                                safeRelativePath +
                                " 大小：" +
                                formatFileSize(fileSize));

                File receiveDir = new File(AppSettings.getReceiveDir());

                if (!receiveDir.exists()) {
                    receiveDir.mkdirs();
                }

                File saveFile = new File(receiveDir, safeRelativePath);

                File parentFolder = saveFile.getParentFile();

                if (parentFolder != null && !parentFolder.exists()) {
                    parentFolder.mkdirs();
                }

                saveFile = buildUniqueSaveFile(saveFile);

                String savePath = saveFile.getAbsolutePath();

                File tempFile = new File(savePath + ".part");

                if (tempFile.exists()) {
                    tempFile.delete();
                }

                if (receiveCallback != null) {
                    receiveCallback.onFileReceiveStart(
                            clientIp,
                            safeRelativePath,
                            savePath,
                            fileSize);
                }

                FileOutputStream fileOut = new FileOutputStream(tempFile);

                byte[] buffer = new byte[8192];

                long received = 0;

                int len;

                int lastProgress = -1;

                while (received < fileSize) {

                    len = in.read(
                            buffer,
                            0,
                            (int) Math.min(buffer.length, fileSize - received));

                    if (len == -1) {
                        break;
                    }

                    fileOut.write(buffer, 0, len);

                    received += len;

                    int progress;

                    if (fileSize == 0) {
                        progress = 100;
                    } else {
                        progress = (int) ((received * 100) / fileSize);
                    }

                    if (progress != lastProgress) {
                        lastProgress = progress;

                        if (receiveCallback != null) {
                            receiveCallback.onFileReceiveProgress(
                                    clientIp,
                                    safeRelativePath,
                                    savePath,
                                    fileSize,
                                    received,
                                    progress);
                        }
                    }
                }

                fileOut.close();

                if (received == fileSize) {

                    Files.move(
                            tempFile.toPath(),
                            saveFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    System.out.println(safeRelativePath + " 接收完成");
                    System.out.println("实际保存路径：" + saveFile.getAbsolutePath());
                    System.out.println("实际接收大小：" + formatFileSize(received));

                    if (receiveCallback != null) {
                        receiveCallback.onFileReceived(
                                clientIp,
                                safeRelativePath,
                                saveFile.getAbsolutePath(),
                                received);
                    }

                } else {

                    if (tempFile.exists()) {
                        tempFile.delete();
                    }

                    System.out.println(
                            safeRelativePath +
                                    " 接收失败：文件未完整接收，已删除临时文件");
                }
            }

            System.out.println("设备 " + clientIp + " 所有文件接收完成\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String listDirectory(String path) {
        String normalizedPath = normalizeRemotePath(path);
        File dir = new File(normalizedPath);

        System.out.println("LIST 请求路径：" + path);
        System.out.println("LIST 规范路径：" + normalizedPath);
        System.out.println("exists=" + dir.exists() + ", isDirectory=" + dir.isDirectory() + ", canRead=" + dir.canRead());

        StringBuilder json = new StringBuilder();
        json.append("{\"path\":\"").append(escapeJson(normalizedPath)).append("\",\"files\":[");

        boolean first = true;
        int addedCount = 0;
        int maxCount = 300;          // 防止目录内容太多，writeUTF 超过 64KB 后客户端一直转圈
        int maxJsonBytes = 58000;    // DataOutputStream.writeUTF 上限约 65535 bytes，留一点安全余量

        try {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();

                if (files != null) {
                    Arrays.sort(files, (a, b) -> {
                        try {
                            if (a.isDirectory() && !b.isDirectory())
                                return -1;
                            if (!a.isDirectory() && b.isDirectory())
                                return 1;
                            return a.getName().compareToIgnoreCase(b.getName());
                        } catch (Exception e) {
                            return 0;
                        }
                    });

                    for (File f : files) {
                        try {
                            if (addedCount >= maxCount) {
                                System.out.println("LIST 提前截断：文件数量超过 " + maxCount + "，路径：" + normalizedPath);
                                break;
                            }

                            String name = f.getName();
                            if (name == null || name.length() == 0) {
                                continue;
                            }

                            if (f.isHidden() || name.startsWith("$")) {
                                continue;
                            }

                            String modified;
                            try {
                                modified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                        .format(new Date(f.lastModified()));
                            } catch (Exception e) {
                                modified = "";
                            }

                            StringBuilder item = new StringBuilder();
                            if (!first) {
                                item.append(",");
                            }
                            item.append("{");
                            item.append("\"name\":\"").append(escapeJson(name)).append("\",");
                            item.append("\"isDir\":").append(f.isDirectory()).append(",");
                            item.append("\"size\":").append(f.isFile() ? f.length() : 0).append(",");
                            item.append("\"modified\":\"").append(modified).append("\"");
                            item.append("}");

                            int nextBytes = (json.length() + item.length() + 2) * 3;
                            if (nextBytes > maxJsonBytes) {
                                System.out.println("LIST 提前截断：JSON 过大，避免 writeUTF 超限。路径：" + normalizedPath);
                                break;
                            }

                            json.append(item);
                            first = false;
                            addedCount++;

                        } catch (Exception fileException) {
                            System.out.println("跳过无法读取的文件项：" + f.getAbsolutePath() + "，原因：" + fileException.getMessage());
                        }
                    }

                    System.out.println("LIST 返回数量：" + addedCount + "，路径：" + normalizedPath);
                } else {
                    System.out.println("LIST 失败：目录无法读取，可能是 macOS 权限未授予 Terminal / Java。路径：" + normalizedPath);
                }
            } else {
                System.out.println("LIST 失败：路径不存在或不是目录：" + normalizedPath);
            }
        } catch (Exception e) {
            System.out.println("LIST 异常，但仍返回空列表。路径：" + normalizedPath + "，原因：" + e.getMessage());
        }

        json.append("]}");

        int bytes = json.toString().getBytes(StandardCharsets.UTF_8).length;
        System.out.println("LIST JSON 大小：" + bytes + " bytes");

        return json.toString();
    }

    private static String listDrives() {
        StringBuilder json = new StringBuilder();
        json.append("{\"drives\":[");

        boolean first = true;
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            String home = System.getProperty("user.home");
            String[] macRoots = {
                    home,
                    home + "/Desktop",
                    home + "/Documents",
                    home + "/Downloads",
                    "/Users",
                    "/Volumes",
                    "/"
            };

            for (String rootPath : macRoots) {
                File root = new File(rootPath);
                if (!root.exists()) {
                    continue;
                }
                if (!first)
                    json.append(",");
                first = false;
                String path = root.getAbsolutePath().replace("\\", "/");
                json.append("\"").append(escapeJson(path)).append("\"");
            }
        } else {
            File[] roots = File.listRoots();
            for (File root : roots) {
                if (!first)
                    json.append(",");
                first = false;
                String path = root.getAbsolutePath().replace("\\", "/");
                json.append("\"").append(escapeJson(path)).append("\"");
            }
        }

        json.append("]}");
        return json.toString();
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
