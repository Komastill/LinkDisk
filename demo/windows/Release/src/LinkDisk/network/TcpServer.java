package LinkDisk.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
                long fileSize
        );

        void onFileReceiveProgress(
                String clientIp,
                String fileName,
                String savePath,
                long fileSize,
                long receivedBytes,
                int progress
        );

        void onFileReceived(
                String clientIp,
                String fileName,
                String savePath,
                long fileSize
        );
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

                        String clientIp =
                                socket.getInetAddress().getHostAddress();

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

            DataInputStream in =
                    new DataInputStream(socket.getInputStream());

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            String command = in.readUTF();

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
                        formatFileSize(fileSize)
                );

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
                            fileSize
                    );
                }

                FileOutputStream fileOut =
                        new FileOutputStream(tempFile);

                byte[] buffer = new byte[8192];

                long received = 0;

                int len;

                int lastProgress = -1;

                while (received < fileSize) {

                    len = in.read(
                            buffer,
                            0,
                            (int) Math.min(buffer.length, fileSize - received)
                    );

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
                                    progress
                            );
                        }
                    }
                }

                fileOut.close();

                if (received == fileSize) {

                    Files.move(
                            tempFile.toPath(),
                            saveFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    System.out.println(safeRelativePath + " 接收完成");
                    System.out.println("实际保存路径：" + saveFile.getAbsolutePath());
                    System.out.println("实际接收大小：" + formatFileSize(received));

                    if (receiveCallback != null) {
                        receiveCallback.onFileReceived(
                                clientIp,
                                safeRelativePath,
                                saveFile.getAbsolutePath(),
                                received
                        );
                    }

                } else {

                    if (tempFile.exists()) {
                        tempFile.delete();
                    }

                    System.out.println(
                            safeRelativePath +
                            " 接收失败：文件未完整接收，已删除临时文件"
                    );
                }
            }

            System.out.println("设备 " + clientIp + " 所有文件接收完成\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
