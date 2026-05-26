package LinkDisk.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import LinkDisk.model.TransferFileItem;

public class TcpClient {

    private static final int TCP_PORT = 6000;

    private static final int CONNECT_TIMEOUT = 5000;

    private static final int READ_TIMEOUT = 15000;

    public static class ConnectResult {
        public boolean success;
        public String deviceName;
        public String platform;
        public String message;

        public ConnectResult(boolean success, String deviceName, String platform, String message) {
            this.success = success;
            this.deviceName = deviceName;
            this.platform = platform;
            this.message = message;
        }
    }

    public static class SendResult {
        public boolean success;
        public String message;

        public SendResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static ConnectResult connectDevice(String ip) {

        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            socket = new Socket();

            socket.connect(
                    new InetSocketAddress(ip, TCP_PORT),
                    CONNECT_TIMEOUT
            );

            socket.setSoTimeout(READ_TIMEOUT);

            out = new DataOutputStream(socket.getOutputStream());

            in = new DataInputStream(socket.getInputStream());

            out.writeUTF("AUTH");

            out.flush();

            String result = in.readUTF();

            if (!"OK".equals(result)) {
                return new ConnectResult(
                        false,
                        null,
                        null,
                        "对方拒绝连接"
                );
            }

            String deviceName = in.readUTF();

            String platform = in.readUTF();

            return new ConnectResult(
                    true,
                    deviceName,
                    platform,
                    "连接成功"
            );

        } catch (java.net.SocketTimeoutException e) {

            return new ConnectResult(
                    false,
                    null,
                    null,
                    "连接超时：目标设备无响应"
            );

        } catch (java.net.ConnectException e) {

            return new ConnectResult(
                    false,
                    null,
                    null,
                    "连接失败：目标设备未启动 LinkDisk 或端口未开放"
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new ConnectResult(
                    false,
                    null,
                    null,
                    "连接失败：" + e.getMessage()
            );

        } finally {

            closeQuietly(in);

            closeQuietly(out);

            closeQuietly(socket);
        }
    }

    public static SendResult sendFiles(
            TransferFileItem[] items,
            String ip,
            ProgressListener listener
    ) {

        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;

        try {
            socket = new Socket();

            socket.connect(
                    new InetSocketAddress(ip, TCP_PORT),
                    CONNECT_TIMEOUT
            );

            socket.setSoTimeout(READ_TIMEOUT);

            out = new DataOutputStream(socket.getOutputStream());

            in = new DataInputStream(socket.getInputStream());

            out.writeUTF("FILE");

            out.flush();

            String result = in.readUTF();

            if (!"OK".equals(result)) {
                return new SendResult(
                        false,
                        "对方拒绝连接，文件未发送"
                );
            }

            out.writeInt(items.length);

            long totalBytes = 0;

            for (TransferFileItem item : items) {
                totalBytes += item.getSize();
            }

            long sentBytes = 0;

            byte[] buffer = new byte[8192];

            for (int i = 0; i < items.length; i++) {

                TransferFileItem item = items[i];

                File file = item.getSourceFile();

                String relativePath = item.getRelativePath();

                System.out.println("准备发送文件：" + file.getAbsolutePath());

                System.out.println("发送相对路径：" + relativePath);

                System.out.println("发送文件大小：" + formatFileSize(item.getSize()));

                listener.onFileStart(i, relativePath);

                out.writeUTF(relativePath);

                out.writeLong(item.getSize());

                FileInputStream fileIn = null;

                try {
                    fileIn = new FileInputStream(file);

                    int len;

                    long fileSentBytes = 0;

                    while ((len = fileIn.read(buffer)) != -1) {

                        out.write(buffer, 0, len);

                        sentBytes += len;

                        fileSentBytes += len;

                        int totalProgress;

                        if (totalBytes == 0) {
                            totalProgress = 100;
                        } else {
                            totalProgress = (int) ((sentBytes * 100) / totalBytes);
                        }

                        int fileProgress;

                        if (item.getSize() == 0) {
                            fileProgress = 100;
                        } else {
                            fileProgress = (int) ((fileSentBytes * 100) / item.getSize());
                        }

                        listener.onTotalProgress(totalProgress);

                        listener.onFileProgress(i, relativePath, fileProgress);
                    }

                } finally {
                    closeQuietly(fileIn);
                }

                listener.onFileComplete(i, relativePath);
            }

            out.flush();

            System.out.println("全部文件发送完成");

            return new SendResult(
                    true,
                    "文件发送完成"
            );

        } catch (java.net.SocketTimeoutException e) {

            return new SendResult(
                    false,
                    "发送失败：连接超时或对方无响应"
            );

        } catch (java.net.ConnectException e) {

            return new SendResult(
                    false,
                    "发送失败：目标设备未启动 LinkDisk 或端口未开放"
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new SendResult(
                    false,
                    "发送失败：" + e.getMessage()
            );

        } finally {

            closeQuietly(in);

            closeQuietly(out);

            closeQuietly(socket);
        }
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

    private static void closeQuietly(java.io.Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // 忽略关闭异常
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            // 忽略关闭异常
        }
    }
}
