package LinkDisk.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

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

    public interface CancelChecker {
        boolean isCancelled(int fileIndex, String relativePath);
    }

    public static ConnectResult connectDevice(String ip) {
        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, TCP_PORT), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            out.writeUTF("AUTH");
            out.flush();
            String result = in.readUTF();
            if (!"OK".equals(result)) {
                return new ConnectResult(false, null, null, "对方拒绝连接");
            }
            String deviceName = in.readUTF();
            String platform = in.readUTF();
            return new ConnectResult(true, deviceName, platform, "连接成功");
        } catch (java.net.SocketTimeoutException e) {
            return new ConnectResult(false, null, null, "连接超时：目标设备无响应");
        } catch (java.net.ConnectException e) {
            return new ConnectResult(false, null, null, "连接失败：目标设备未启动 LinkDisk 或端口未开放");
        } catch (Exception e) {
            e.printStackTrace();
            return new ConnectResult(false, null, null, "连接失败：" + e.getMessage());
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            closeQuietly(socket);
        }
    }

    public static SendResult sendFiles(TransferFileItem[] items, String ip, ProgressListener listener) {
        return sendFiles(items, ip, listener, null);
    }

    public static SendResult sendFiles(TransferFileItem[] items, String ip, ProgressListener listener,
            CancelChecker cancelChecker) {
        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, TCP_PORT), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            out.writeUTF("FILE");
            out.flush();
            String result = in.readUTF();
            if (!"OK".equals(result)) {
                return new SendResult(false, "对方拒绝连接，文件未发送");
            }
            out.writeInt(items.length);
            long totalBytes = 0;
            for (TransferFileItem item : items)
                totalBytes += item.getSize();
            long sentBytes = 0;
            byte[] buffer = new byte[8192];
            for (int i = 0; i < items.length; i++) {
                TransferFileItem item = items[i];
                File file = item.getSourceFile();
                String relativePath = item.getRelativePath();
                if (cancelChecker != null && cancelChecker.isCancelled(i, relativePath)) {
                    sentBytes += item.getSize();
                    int totalProgress = (totalBytes == 0) ? 100 : (int) ((sentBytes * 100) / totalBytes);
                    listener.onTotalProgress(totalProgress);
                    continue;
                }
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
                        int totalProgress = (totalBytes == 0) ? 100 : (int) ((sentBytes * 100) / totalBytes);
                        int fileProgress = (item.getSize() == 0) ? 100 : (int) ((fileSentBytes * 100) / item.getSize());
                        listener.onTotalProgress(totalProgress);
                        listener.onFileProgress(i, relativePath, fileProgress);
                    }
                } finally {
                    closeQuietly(fileIn);
                }
                listener.onFileComplete(i, relativePath);
            }
            out.flush();
            return new SendResult(true, "文件发送完成");
        } catch (java.net.SocketTimeoutException e) {
            return new SendResult(false, "发送失败：连接超时或对方无响应");
        } catch (java.net.ConnectException e) {
            return new SendResult(false, "发送失败：目标设备未启动 LinkDisk 或端口未开放");
        } catch (Exception e) {
            e.printStackTrace();
            return new SendResult(false, "发送失败：" + e.getMessage());
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            closeQuietly(socket);
        }
    }

    public static String getRemoteDrives(String ip) throws Exception {
        return sendCommand(ip, "DRIVES");
    }

    public static String listRemoteFiles(String ip, String path) throws Exception {
        return sendCommand(ip, "LIST", path);
    }

    public static String sendCommand(String ip, String command, String... params) throws Exception {
        Socket socket = null;
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, TCP_PORT), CONNECT_TIMEOUT);
            socket.setSoTimeout(READ_TIMEOUT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            out.writeUTF(command);
            out.flush();
            if ("DOWNLOAD".equals(command)) {
                for (String param : params)
                    out.writeUTF(param);
                out.flush();
                String result = in.readUTF();
                if ("OK".equals(result)) {
                    long fileSize = in.readLong();
                    File tempFile = File.createTempFile("remote_download_", ".tmp");
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    byte[] buffer = new byte[8192];
                    long remaining = fileSize;
                    while (remaining > 0) {
                        int len = in.read(buffer, 0, (int) Math.min(remaining, buffer.length));
                        if (len == -1)
                            break;
                        fos.write(buffer, 0, len);
                        remaining -= len;
                    }
                    fos.close();
                    return "OK:" + tempFile.getAbsolutePath();
                } else {
                    return "FAILED";
                }
            }
            // 普通命令：先读一次对方返回的 OK/FAILED，再发送参数，最后读最终结果
            String result = in.readUTF();
            if (!"OK".equals(result)) {
                throw new Exception("对方拒绝请求：" + result);
            }
            for (String param : params)
                out.writeUTF(param);
            out.flush();
            String response = in.readUTF();
            return response;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            closeQuietly(socket);
        }
    }

    public static void requestFileSend(String targetIp, String requesterIp, String filePath) throws Exception {
        String result = sendCommand(targetIp, "REQUEST_SEND", requesterIp, filePath);
        if (!"SENT".equals(result)) {
            throw new Exception("远程发送请求失败：" + result);
        }
    }

    private static String formatFileSize(long size) {
        double value = size;
        if (size < 1000)
            return size + " B";
        value /= 1000;
        if (value < 1000)
            return String.format(Locale.US, "%.2f KB", value);
        value /= 1000;
        if (value < 1000)
            return String.format(Locale.US, "%.2f MB", value);
        value /= 1000;
        return String.format(Locale.US, "%.2f GB", value);
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        try {
            if (closeable != null)
                closeable.close();
        } catch (Exception e) {
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
        }
    }
}